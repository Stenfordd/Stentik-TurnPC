// Stentik server agent: lets the web panel start and stop Minecraft servers on this PC.
//
// Run it once (double click): it finds the server folders, asks for a panel password, pairs with
// the Apps Script API, installs itself into %LOCALAPPDATA%\StentikAgent and starts with Windows.
// The background copy (--background) polls the API and controls the servers.
package main

import (
	"bytes"
	"crypto/rand"
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"time"
)

const (
	version    = "2.0"
	defaultAPI = "https://script.google.com/macros/s/AKfycbx4XO5fIneyf4_Zb8sal9cAHtoVXo8vMankC1wGpRP39US6x7d4t4j2fzIDwwsMKX-C/exec"
	panelURL   = "https://stenfordd.github.io/Stentik-TurnPC/"
)

type serverCfg struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	Dir  string `json:"dir"`
}

type config struct {
	API     string      `json:"api"`
	Key     string      `json:"key"`
	Servers []serverCfg `json:"servers"`
}

type srv struct {
	serverCfg
	state     string // stopped | starting | running | stopping
	stateAt   time.Time
	players   []string
	max       int
	detail    string
	gamePort  string
	rconPort  string
	rconPass  string
	proc      *exec.Cmd
	stdin     io.WriteCloser
	lastList  time.Time
	wantStart string // start once the port is free (switching servers / restart)
}

var (
	dataDir string
	cfg     config
	servers []*srv
	cfgTime time.Time
	ack     string
)

func main() {
	dataDir = filepath.Join(os.Getenv("LOCALAPPDATA"), "StentikAgent")
	os.MkdirAll(dataDir, 0o755)
	lf, _ := os.OpenFile(filepath.Join(dataDir, "agent.log"), os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0o644)
	if lf != nil {
		log.SetOutput(lf)
	}
	background := len(os.Args) > 1 && os.Args[1] == "--background"
	log.Printf("agent %s run (background=%v)", version, background)
	if background {
		runLoop()
		return
	}
	setup()
}

// ---------------- setup (double click) ----------------

func cfgPath() string { return filepath.Join(dataDir, "agent.json") }

func loadCfg() bool {
	b, err := os.ReadFile(cfgPath())
	if err != nil {
		return false
	}
	if json.Unmarshal(b, &cfg) != nil || cfg.Key == "" {
		return false
	}
	if cfg.API == "" {
		cfg.API = defaultAPI
	}
	if st, err := os.Stat(cfgPath()); err == nil {
		cfgTime = st.ModTime()
	}
	return true
}

func saveCfg() error {
	b, _ := json.MarshalIndent(cfg, "", "  ")
	return os.WriteFile(cfgPath(), b, 0o644)
}

func setup() {
	have := loadCfg()
	if have {
		names := []string{}
		for _, s := range cfg.Servers {
			names = append(names, "  • "+s.Name)
		}
		if !yesNo("The server agent is already set up for:\n\n" + strings.Join(names, "\n") +
			"\n\nSearch for servers again and set the panel password again?") {
			ensureRunning()
			openURL(panelURL)
			return
		}
	} else {
		cfg = config{API: defaultAPI, Key: randHex(24)}
	}

	found := scanServers()
	log.Printf("setup: found %d server folders", len(found))
	var chosen []serverCfg
	if len(found) > 0 {
		lines := []string{}
		for _, s := range found {
			lines = append(lines, "  • "+s.Name+"   ("+s.Dir+")")
		}
		if yesNo("Found these Minecraft servers:\n\n" + strings.Join(lines, "\n") + "\n\nUse all of them in the panel?\n(No = pick the folders yourself)") {
			chosen = found
		}
	}
	if chosen == nil {
		for {
			d := pickFolder("Pick a Minecraft SERVER folder (the one with server.properties)")
			if d == "" {
				break
			}
			if !isServerDir(d) {
				msgBox("That folder has no server.properties / start script. Pick the server folder itself.")
				continue
			}
			chosen = append(chosen, makeServer(d))
			if !yesNo("Added: " + filepath.Base(d) + "\n\nAdd another server?") {
				break
			}
		}
	}
	if len(chosen) == 0 {
		msgBox("No server selected, nothing was changed.")
		return
	}
	cfg.Servers = chosen

	pw := ""
	for {
		pw = strings.TrimSpace(inputBox("Choose the password for the web panel.\nGive it only to friends who may start and stop the servers.", "Server panel password", ""))
		if pw == "" {
			if have {
				break // keep the old password
			}
			log.Printf("setup: cancelled at password")
		msgBox("Setup cancelled.")
			return
		}
		if len(pw) >= 4 {
			break
		}
		msgBox("Use at least 4 characters.")
	}
	log.Printf("setup: %d servers chosen, password entered: %v", len(cfg.Servers), pw != "")
	if pw != "" {
		if err := register(pw); err != nil {
			log.Printf("setup: register failed: %v", err)
			msgBox("Could not connect the panel:\n\n" + err.Error() + "\n\nCheck the internet connection and that the Apps Script is deployed for \"Anyone\".")
			return
		}
	}
	for _, s := range cfg.Servers {
		if err := ensureRcon(s.Dir); err != nil {
			log.Printf("rcon %s: %v", s.Name, err)
		}
	}
	if err := saveCfg(); err != nil {
		msgBox("Could not save the settings: " + err.Error())
		return
	}
	log.Printf("setup: paired and saved")
	ensureRunning()
	msgBox("Done! The agent now runs in the background and starts with Windows.\n\nPanel: " + panelURL +
		"\n\nIf a server is running right now, restart it once so the panel can see its players.")
	openURL(panelURL)
}

func register(pw string) error {
	var r struct {
		OK    bool   `json:"ok"`
		Error string `json:"error"`
	}
	if err := post(map[string]any{"action": "register", "key": cfg.Key, "password": pw}, &r); err != nil {
		return err
	}
	if !r.OK {
		return errors.New(r.Error)
	}
	return nil
}

// ensureRunning installs this exe into dataDir, registers autostart and starts the background copy.
func ensureRunning() {
	self, _ := os.Executable()
	target := filepath.Join(dataDir, "stentik-agent.exe")
	if !strings.EqualFold(filepath.Clean(self), filepath.Clean(target)) {
		if b, err := os.ReadFile(self); err == nil {
			stopOldAgent()
			os.WriteFile(target, b, 0o755)
		}
	}
	if err := installAutostart(target); err != nil {
		log.Printf("autostart: %v", err)
	}
	if !agentRunning() {
		c := exec.Command(target, "--background")
		c.Dir = dataDir
		detach(c)
		c.Start()
	}
}

var startNames = []string{"start-server.bat", "startserver.bat", "run.bat", "start.bat", "ServerStart.bat"}

func isServerDir(d string) bool {
	if _, err := os.Stat(filepath.Join(d, "server.properties")); err == nil {
		return true
	}
	if _, err := os.Stat(filepath.Join(d, "user_jvm_args.txt")); err == nil {
		for _, n := range startNames {
			if _, err := os.Stat(filepath.Join(d, n)); err == nil {
				return true
			}
		}
	}
	return false
}

func makeServer(d string) serverCfg {
	name := strings.NewReplacer("-", " ", "_", " ").Replace(filepath.Base(d))
	h := sha1.Sum([]byte(strings.ToLower(d)))
	id := strings.ToLower(regexp.MustCompile(`[^A-Za-z0-9]+`).ReplaceAllString(filepath.Base(d), "-"))
	if len(id) > 20 {
		id = id[:20]
	}
	return serverCfg{ID: id + "-" + hex.EncodeToString(h[:2]), Name: name, Dir: d}
}

func scanServers() []serverCfg {
	home := os.Getenv("USERPROFILE")
	type root struct {
		dir   string
		depth int
	}
	roots := []root{{home, 4}}
	for c := 'C'; c <= 'Z'; c++ {
		d := string(c) + `:\`
		if _, err := os.Stat(d); err == nil {
			roots = append(roots, root{d, 3})
		}
	}
	skip := regexp.MustCompile(`(?i)^(windows|program files.*|programdata|appdata|\$.*|system volume information|node_modules|\..*|steamapps|onedrive|recovery|perflogs|instances|mods|libraries|world|config|logs)$`)
	seen := map[string]bool{}
	var out []serverCfg
	var walk func(d string, depth int)
	walk = func(d string, depth int) {
		if seen[strings.ToLower(d)] {
			return
		}
		seen[strings.ToLower(d)] = true
		if isServerDir(d) {
			out = append(out, makeServer(d))
			return
		}
		if depth == 0 {
			return
		}
		ents, err := os.ReadDir(d)
		if err != nil {
			return
		}
		for _, e := range ents {
			if e.IsDir() && !skip.MatchString(e.Name()) {
				walk(filepath.Join(d, e.Name()), depth-1)
			}
		}
	}
	for _, r := range roots {
		walk(r.dir, r.depth)
	}
	sort.Slice(out, func(i, j int) bool { return out[i].Name < out[j].Name })
	return out
}

func randHex(n int) string {
	b := make([]byte, n)
	rand.Read(b)
	return hex.EncodeToString(b)
}

// ensureRcon turns RCON on so the agent can read the player list and stop servers it did not start.
func ensureRcon(dir string) error {
	p := filepath.Join(dir, "server.properties")
	b, err := os.ReadFile(p)
	if err != nil {
		return err
	}
	lines := strings.Split(strings.ReplaceAll(string(b), "\r\n", "\n"), "\n")
	get, set := propsFns(&lines)
	changed := false
	if get("enable-rcon") != "true" {
		set("enable-rcon", "true")
		changed = true
	}
	if get("rcon.password") == "" {
		set("rcon.password", randHex(12))
		changed = true
	}
	if get("rcon.port") == "" {
		set("rcon.port", "25575")
		changed = true
	}
	if changed {
		return os.WriteFile(p, []byte(strings.Join(lines, "\r\n")), 0o644)
	}
	return nil
}

func propsFns(lines *[]string) (func(string) string, func(string, string)) {
	get := func(k string) string {
		for _, l := range *lines {
			if strings.HasPrefix(l, k+"=") {
				return strings.TrimSpace(strings.TrimPrefix(l, k+"="))
			}
		}
		return ""
	}
	set := func(k, v string) {
		for i, l := range *lines {
			if strings.HasPrefix(l, k+"=") {
				(*lines)[i] = k + "=" + v
				return
			}
		}
		*lines = append(*lines, k+"="+v)
	}
	return get, set
}

func readProps(dir string) (gamePort, rconPort, rconPass string) {
	b, _ := os.ReadFile(filepath.Join(dir, "server.properties"))
	lines := strings.Split(strings.ReplaceAll(string(b), "\r\n", "\n"), "\n")
	get, _ := propsFns(&lines)
	gamePort, rconPort, rconPass = get("server-port"), get("rcon.port"), get("rcon.password")
	if gamePort == "" {
		gamePort = "25565"
	}
	if rconPort == "" {
		rconPort = "25575"
	}
	if get("enable-rcon") != "true" {
		rconPass = ""
	}
	return
}

// ---------------- background loop ----------------

func runLoop() {
	if !singleInstance() {
		return
	}
	log.Printf("agent %s started", version)
	for !loadCfg() {
		time.Sleep(30 * time.Second)
	}
	buildServers()
	fast := time.Time{}
	for {
		if st, err := os.Stat(cfgPath()); err == nil && st.ModTime().After(cfgTime) && loadCfg() {
			buildServers()
		}
		refreshAll()
		if c := report(); c != nil {
			fast = time.Now().Add(3 * time.Minute)
			run(c)
			refreshAll()
			report()
		}
		busy := false
		for _, s := range servers {
			if s.state == "starting" || s.state == "stopping" || s.wantStart != "" {
				busy = true
			}
		}
		if busy || time.Now().Before(fast) {
			time.Sleep(3 * time.Second)
		} else {
			time.Sleep(10 * time.Second)
		}
	}
}

func buildServers() {
	old := map[string]*srv{}
	for _, s := range servers {
		old[s.ID] = s
	}
	servers = nil
	for _, c := range cfg.Servers {
		s := old[c.ID]
		if s == nil {
			s = &srv{state: "stopped", stateAt: time.Now()}
		}
		s.serverCfg = c
		servers = append(servers, s)
	}
}

func portUp(p string) bool {
	c, err := net.DialTimeout("tcp", "127.0.0.1:"+p, time.Second)
	if err != nil {
		return false
	}
	c.Close()
	return true
}

func (s *srv) alive() bool { return s.proc != nil && s.proc.ProcessState == nil }

func (s *srv) set(st string) {
	if st != s.state {
		log.Printf("%s: %s -> %s", s.Name, s.state, st)
		s.state, s.stateAt = st, time.Now()
	}
	if st == "stopped" {
		s.players = nil
	}
}

var listRe = regexp.MustCompile(`There are (\d+) of a max of (\d+) players online:?\s*(.*)`)

// owner returns which configured server currently holds a game port.
func owner(port string) *srv {
	var cands []*srv
	for _, s := range servers {
		if s.gamePort == port {
			cands = append(cands, s)
		}
	}
	if len(cands) == 1 {
		return cands[0]
	}
	for _, s := range cands {
		if s.alive() {
			return s
		}
	}
	for _, s := range cands {
		if s.rconPass != "" {
			if r, err := rconDial("127.0.0.1:"+s.rconPort, s.rconPass); err == nil {
				r.Close()
				return s
			}
		}
	}
	for _, s := range cands {
		if s.state == "running" || s.state == "starting" || s.state == "stopping" {
			return s
		}
	}
	return nil
}

func refreshAll() {
	ports := map[string]bool{}
	for _, s := range servers {
		s.gamePort, s.rconPort, s.rconPass = readProps(s.Dir)
		ports[s.gamePort] = true
	}
	owners := map[*srv]bool{}
	for p := range ports {
		if portUp(p) {
			if o := owner(p); o != nil {
				owners[o] = true
			}
		}
	}
	for _, s := range servers {
		up := owners[s]
		switch {
		case up && s.state != "stopping":
			s.set("running")
		case up && s.state == "stopping" && time.Since(s.stateAt) > 3*time.Minute:
			s.set("running")
		case !up && s.state == "starting" && s.alive() && time.Since(s.stateAt) < 8*time.Minute:
			// still booting
		case !up && s.state == "starting":
			s.set("stopped")
			if s.detail == "" {
				s.detail = "did not start, see logs/latest.log in the server folder"
			}
		case !up && s.state != "stopped" && !s.alive():
			s.set("stopped")
		case !up && s.state == "running":
			s.set("stopped")
		}
		if s.state == "running" && s.rconPass != "" && time.Since(s.lastList) > 20*time.Second {
			s.lastList = time.Now()
			if r, err := rconDial("127.0.0.1:"+s.rconPort, s.rconPass); err == nil {
				out, err := r.cmd("list")
				r.Close()
				if m := listRe.FindStringSubmatch(out); err == nil && m != nil {
					fmt.Sscan(m[2], &s.max)
					s.players = nil
					for _, n := range strings.Split(m[3], ",") {
						if n = strings.TrimSpace(n); n != "" {
							s.players = append(s.players, n)
						}
					}
				}
			}
		}
	}
	// queued starts (switching servers / restart) once nothing else uses the port
	for _, s := range servers {
		if s.wantStart == "" || s.state != "stopped" {
			continue
		}
		free := true
		for _, o := range servers {
			if o != s && o.gamePort == s.gamePort && o.state != "stopped" {
				free = false
			}
		}
		if free && !portUp(s.gamePort) {
			by := s.wantStart
			s.wantStart = ""
			start(s, by)
		}
	}
}

type command struct {
	ID     string `json:"id"`
	Cmd    string `json:"cmd"`
	Server string `json:"server"`
	By     string `json:"by"`
}

func post(body any, out any) error {
	b, _ := json.Marshal(body)
	req, _ := http.NewRequest("POST", cfg.API, bytes.NewReader(b))
	req.Header.Set("Content-Type", "text/plain")
	resp, err := (&http.Client{Timeout: 25 * time.Second}).Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	rb, _ := io.ReadAll(resp.Body)
	if err := json.Unmarshal(rb, out); err != nil {
		return fmt.Errorf("unexpected answer from the API (%s)", resp.Status)
	}
	return nil
}

func report() *command {
	list := []map[string]any{}
	for _, s := range servers {
		list = append(list, map[string]any{"id": s.ID, "name": s.Name, "state": s.state, "players": s.players, "max": s.max, "detail": s.detail})
	}
	var r struct {
		OK    bool     `json:"ok"`
		Error string   `json:"error"`
		Cmd   *command `json:"cmd"`
	}
	err := post(map[string]any{"action": "agent", "key": cfg.Key, "ack": ack, "status": map[string]any{"servers": list, "agent": version}}, &r)
	if err != nil {
		log.Printf("api: %v", err)
		return nil
	}
	if !r.OK {
		log.Printf("api: %s", r.Error)
		return nil
	}
	ack = ""
	if r.Cmd != nil && r.Cmd.ID != "" {
		return r.Cmd
	}
	return nil
}

func run(c *command) {
	ack = c.ID
	var s *srv
	for _, x := range servers {
		if x.ID == c.Server {
			s = x
		}
	}
	if s == nil {
		return
	}
	log.Printf("command %s %s by %s", c.Cmd, s.Name, c.By)
	s.detail = ""
	switch c.Cmd {
	case "start":
		if s.state == "running" || s.state == "starting" {
			return
		}
		switched := false
		for _, o := range servers {
			if o != s && o.gamePort == s.gamePort && (o.state == "running" || o.state == "starting") {
				stop(o, "switching to "+s.Name)
				switched = true
			}
		}
		if switched {
			s.wantStart = c.By
		} else {
			start(s, c.By)
		}
	case "stop":
		stop(s, "")
	case "restart":
		stop(s, "restarting")
		s.wantStart = c.By
	}
}

func findJava(dir string) string {
	cands := []string{filepath.Join(dir, "runtime", "bin", "java.exe"), filepath.Join(dir, "jre", "bin", "java.exe")}
	if jh := os.Getenv("JAVA_HOME"); jh != "" {
		cands = append(cands, filepath.Join(jh, "bin", "java.exe"))
	}
	for _, pat := range []string{
		filepath.Join(os.Getenv("APPDATA"), "PrismLauncher", "java", "*21*", "bin", "java.exe"),
		filepath.Join(os.Getenv("ProgramFiles"), "Eclipse Adoptium", "jdk-21*", "bin", "java.exe"),
		filepath.Join(os.Getenv("ProgramFiles"), "Eclipse Adoptium", "jre-21*", "bin", "java.exe"),
		filepath.Join(os.Getenv("ProgramFiles"), "Java", "jdk-21*", "bin", "java.exe"),
	} {
		m, _ := filepath.Glob(pat)
		sort.Sort(sort.Reverse(sort.StringSlice(m)))
		cands = append(cands, m...)
	}
	for _, c := range cands {
		if _, err := os.Stat(c); err == nil {
			return c
		}
	}
	return "java"
}

func winArgs(dir string) string {
	for _, pat := range []string{"libraries/net/neoforged/neoforge/*/win_args.txt", "libraries/net/minecraftforge/forge/*/win_args.txt"} {
		m, _ := filepath.Glob(filepath.Join(dir, filepath.FromSlash(pat)))
		if len(m) > 0 {
			sort.Strings(m)
			rel, _ := filepath.Rel(dir, m[len(m)-1])
			return rel
		}
	}
	return ""
}

func start(s *srv, by string) {
	eula, _ := os.ReadFile(filepath.Join(s.Dir, "eula.txt"))
	if !strings.Contains(string(eula), "eula=true") {
		s.detail = "eula.txt is not accepted yet, start the server by hand once"
		return
	}
	lf, _ := os.OpenFile(filepath.Join(s.Dir, "panel-console.log"), os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0o644)
	var cmd *exec.Cmd
	if wa := winArgs(s.Dir); wa != "" {
		args := []string{}
		if _, err := os.Stat(filepath.Join(s.Dir, "user_jvm_args.txt")); err == nil {
			args = append(args, "@user_jvm_args.txt")
		}
		args = append(args, "@"+wa, "nogui")
		cmd = exec.Command(findJava(s.Dir), args...)
	} else {
		bat := ""
		for _, n := range startNames {
			if _, err := os.Stat(filepath.Join(s.Dir, n)); err == nil {
				bat = n
				break
			}
		}
		if bat == "" {
			s.detail = "no start script found"
			return
		}
		cmd = exec.Command("cmd.exe", "/c", bat)
	}
	cmd.Dir = s.Dir
	cmd.Stdout, cmd.Stderr = lf, lf
	in, _ := cmd.StdinPipe()
	hide(cmd)
	if err := cmd.Start(); err != nil {
		s.detail = "could not start: " + err.Error()
		log.Print(s.detail)
		return
	}
	s.proc, s.stdin = cmd, in
	go func() { cmd.Wait(); lf.Close() }()
	s.set("starting")
	log.Printf("%s start (by %s): %v", s.Name, by, cmd.Args)
}

func stop(s *srv, why string) {
	msg := "Server stops in 10 seconds (remote panel)"
	if why != "" {
		msg = "Server stops in 10 seconds: " + why
	}
	if s.alive() && s.stdin != nil {
		io.WriteString(s.stdin, "say "+msg+"\n")
		time.Sleep(10 * time.Second)
		io.WriteString(s.stdin, "stop\n")
		s.set("stopping")
		return
	}
	if s.rconPass == "" {
		s.detail = "this server was started by hand before the panel was set up; stop it by hand once"
		return
	}
	r, err := rconDial("127.0.0.1:"+s.rconPort, s.rconPass)
	if err != nil {
		s.detail = "cannot reach the server console; restart it by hand once"
		return
	}
	r.cmd("say " + msg)
	r.Close()
	time.Sleep(10 * time.Second)
	if r, err = rconDial("127.0.0.1:"+s.rconPort, s.rconPass); err == nil {
		r.cmd("stop")
		r.Close()
	}
	s.set("stopping")
}
