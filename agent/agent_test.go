package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func mk(t *testing.T, dir string, files ...string) {
	os.MkdirAll(dir, 0o755)
	for _, f := range files {
		os.WriteFile(filepath.Join(dir, f), []byte("server-port=25565\n"), 0o644)
	}
}

func TestScanAndRcon(t *testing.T) {
	home := t.TempDir()
	t.Setenv("USERPROFILE", home)
	mk(t, filepath.Join(home, "CogsToCosmos-Server"), "server.properties", "start-server.bat")
	mk(t, filepath.Join(home, "Desktop", "ATM10 Server"), "server.properties", "startserver.bat")
	mk(t, filepath.Join(home, "AppData", "Roaming", "PrismLauncher", "instances", "x"), "server.properties")
	found := scanServers()
	if len(found) != 2 {
		t.Fatalf("found %d: %+v", len(found), found)
	}
	if found[0].Name != "ATM10 Server" || found[1].Name != "CogsToCosmos Server" {
		t.Errorf("names: %+v", found)
	}
	d := found[1].Dir
	if err := ensureRcon(d); err != nil {
		t.Fatal(err)
	}
	gp, rp, pw := readProps(d)
	if gp != "25565" || rp != "25575" || len(pw) != 24 {
		t.Errorf("props %s %s %s", gp, rp, pw)
	}
}

func TestSwitchQueuesStart(t *testing.T) {
	a := &srv{serverCfg: serverCfg{ID: "a", Name: "A"}, state: "running", gamePort: "25565"}
	b := &srv{serverCfg: serverCfg{ID: "b", Name: "B"}, state: "stopped", gamePort: "25565"}
	servers = []*srv{a, b}
	run(&command{ID: "1", Cmd: "start", Server: "b", By: "T"})
	if b.wantStart != "T" {
		t.Errorf("start of B should wait for A to stop")
	}
	if !strings.Contains(a.detail, "") {
		t.Fail()
	}
}

func TestRegister(t *testing.T) {
	var got map[string]any
	srvr := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		json.NewDecoder(r.Body).Decode(&got)
		w.Write([]byte(`{"ok":true}`))
	}))
	defer srvr.Close()
	cfg = config{API: srvr.URL, Key: "k"}
	if err := register("heslo"); err != nil || got["action"] != "register" || got["password"] != "heslo" {
		t.Fatalf("%v %v", err, got)
	}
}

func TestTail(t *testing.T) {
	d := t.TempDir()
	os.MkdirAll(filepath.Join(d, "logs"), 0o755)
	p := filepath.Join(d, "logs", "latest.log")
	os.WriteFile(p, []byte("a\r\nb\nhalf"), 0o644)
	var tl tail
	tl.poll(d)
	if strings.Join(tl.lines, "|") != "a|b" {
		t.Fatalf("got %v", tl.lines)
	}
	f, _ := os.OpenFile(p, os.O_APPEND|os.O_WRONLY, 0)
	f.WriteString(" line\nc\n")
	f.Close()
	tl.poll(d)
	if strings.Join(tl.lines, "|") != "a|b|half line|c" {
		t.Fatalf("got %v", tl.lines)
	}
	os.WriteFile(p, []byte("new\n"), 0o644) // restart: log truncated
	tl.poll(d)
	if strings.Join(tl.lines, "|") != "new" {
		t.Fatalf("after rotate %v", tl.lines)
	}
}
