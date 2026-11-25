package main

// Local control for the PC owner: a small HTTP API on 127.0.0.1 used by the tray icon (tray.go).
// Everything that touches the servers still runs in the agent loop (local requests are queued),
// handlers only read a snapshot.

import (
	"encoding/json"
	"log"
	"net"
	"net/http"
	"sync"
	"time"
)

const localAddr = "127.0.0.1:47811"

type localItem struct {
	cmd  *command // start | stop | restart
	srv  string
	line string // console line
}

type srvSnap struct {
	ID      string   `json:"id"`
	Name    string   `json:"name"`
	Dir     string   `json:"dir"`
	State   string   `json:"state"`
	Players []string `json:"players"`
	Max     int      `json:"max"`
	By      string   `json:"by"`
	Detail  string   `json:"detail"`
	Lines   []string `json:"lines,omitempty"`
}

var (
	localq    = make(chan localItem, 16)
	snapMu    sync.Mutex
	snap      []srvSnap
	trayWatch string // server id the tray console shows
	trayAt    time.Time
)

// takeSnapshot runs in the agent loop.
func takeSnapshot() {
	snapMu.Lock()
	w := trayWatch
	if time.Since(trayAt) > 6*time.Second {
		w = ""
	}
	snapMu.Unlock()
	out := []srvSnap{}
	for _, s := range servers {
		x := srvSnap{ID: s.ID, Name: s.Name, Dir: s.Dir, State: s.state, Players: s.players, Max: s.max, By: s.startedBy, Detail: s.detail}
		if x.Players == nil {
			x.Players = []string{}
		}
		if s.ID == w {
			s.con.poll(s.Dir)
			x.Lines = append([]string(nil), s.con.lines...)
		}
		out = append(out, x)
	}
	snapMu.Lock()
	snap = out
	snapMu.Unlock()
}

// wait sleeps up to d, but handles requests from the tray right away.
func wait(d time.Duration) {
	t := time.NewTimer(d)
	defer t.Stop()
	for {
		select {
		case it := <-localq:
			handleLocal(it)
			refreshAll()
			takeSnapshot()
			report()
		case <-t.C:
			return
		}
	}
}

func handleLocal(it localItem) {
	if it.cmd != nil {
		run(it.cmd)
		return
	}
	for _, s := range servers {
		if s.ID == it.srv {
			s.con.poll(s.Dir)
			consoleCmd(s, it.line, "PC")
		}
	}
}

func startLocal() {
	ln, err := net.Listen("tcp", localAddr)
	if err != nil {
		log.Printf("local api: %v", err)
		return
	}
	mux := http.NewServeMux()
	writeJSON := func(w http.ResponseWriter, v any) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(v)
	}
	guard := func(h http.HandlerFunc) http.HandlerFunc {
		// only the tray: right host and a custom header (a web page cannot send one without a CORS preflight)
		return func(w http.ResponseWriter, r *http.Request) {
			if r.Host != localAddr || r.Header.Get("X-Stentik") != "1" {
				http.Error(w, "forbidden", 403)
				return
			}
			h(w, r)
		}
	}
	mux.HandleFunc("/status", guard(func(w http.ResponseWriter, r *http.Request) {
		if id := r.URL.Query().Get("watch"); id != "" {
			snapMu.Lock()
			trayWatch, trayAt = id, time.Now()
			snapMu.Unlock()
		}
		snapMu.Lock()
		s := snap
		snapMu.Unlock()
		writeJSON(w, map[string]any{"servers": s})
	}))
	mux.HandleFunc("/do", guard(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != "POST" {
			http.Error(w, "POST only", 405)
			return
		}
		var in struct{ Server, Cmd, Line string }
		json.NewDecoder(r.Body).Decode(&in)
		switch {
		case in.Line != "":
			localq <- localItem{srv: in.Server, line: in.Line}
		case in.Cmd == "start" || in.Cmd == "stop" || in.Cmd == "restart":
			localq <- localItem{cmd: &command{Cmd: in.Cmd, Server: in.Server, By: "PC"}}
		default:
			http.Error(w, "bad request", 400)
			return
		}
		writeJSON(w, map[string]any{"ok": true})
	}))
	srv := &http.Server{Handler: mux, ReadTimeout: 10 * time.Second}
	go srv.Serve(ln)
	log.Printf("local api on %s", localAddr)
}
