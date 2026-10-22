package main

import (
	"io"
	"os"
	"path/filepath"
	"strings"
)

const consoleLines = 120

// tail follows logs/latest.log of a server and keeps the last lines (works for servers started
// by the panel and by hand).
type tail struct {
	info  os.FileInfo
	path  string
	off   int64
	part  string
	lines []string
	drop  int // lines dropped from the front since the last scan
}

func (t *tail) poll(dir string) {
	p := filepath.Join(dir, "logs", "latest.log")
	if p != t.path {
		*t = tail{path: p}
	}
	f, err := os.Open(p)
	if err != nil {
		return
	}
	defer f.Close()
	st, err := f.Stat()
	if err != nil {
		return
	}
	if st.Size() < t.off || (t.info != nil && !os.SameFile(t.info, st)) { // new log after a restart
		t.off, t.part, t.lines, t.drop = 0, "", nil, 0
	}
	t.info = st
	if t.off == 0 && st.Size() > 64*1024 {
		t.off = st.Size() - 64*1024 // start near the end of a big log
	}
	f.Seek(t.off, io.SeekStart)
	b, _ := io.ReadAll(io.LimitReader(f, 512*1024))
	t.off += int64(len(b))
	chunk := t.part + strings.ReplaceAll(string(b), "\r\n", "\n")
	parts := strings.Split(chunk, "\n")
	t.part = parts[len(parts)-1]
	for _, l := range parts[:len(parts)-1] {
		t.add(l)
	}
}

func (t *tail) add(l string) {
	if len(l) > 400 {
		l = l[:400] + "…"
	}
	t.lines = append(t.lines, l)
	if len(t.lines) > consoleLines {
		t.drop += len(t.lines) - consoleLines
		t.lines = t.lines[len(t.lines)-consoleLines:]
	}
}

// consoleCmd runs a command typed on the web page.
func consoleCmd(s *srv, line, by string) {
	line = strings.TrimSpace(strings.TrimPrefix(strings.TrimSpace(line), "/"))
	if line == "" {
		return
	}
	s.con.add("[panel] " + by + ": /" + line)
	if s.state != "running" && s.state != "starting" {
		s.con.add("[panel] the server is not running")
		return
	}
	if s.alive() && s.stdin != nil {
		io.WriteString(s.stdin, line+"\n")
		return
	}
	if s.rconPass == "" {
		s.con.add("[panel] no console access: restart the server once from the panel")
		return
	}
	r, err := rconDial("127.0.0.1:"+s.rconPort, s.rconPass)
	if err != nil {
		s.con.add("[panel] console not reachable (RCON): " + err.Error())
		return
	}
	defer r.Close()
	out, err := r.cmd(line)
	if err != nil {
		s.con.add("[panel] " + err.Error())
		return
	}
	for _, l := range strings.Split(strings.TrimSpace(out), "\n") {
		if l != "" {
			s.con.add("[rcon] " + l)
		}
	}
}
