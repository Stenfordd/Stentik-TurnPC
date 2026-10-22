//go:build windows

package main

import (
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"syscall"
	"unsafe"
)

const createNoWindow = 0x08000000

func hide(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true, CreationFlags: createNoWindow}
}

func detach(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{HideWindow: true, CreationFlags: createNoWindow | 0x00000008} // DETACHED_PROCESS
}

var user32 = syscall.NewLazyDLL("user32.dll")

func box(msg string, flags uintptr) int {
	t, _ := syscall.UTF16PtrFromString(msg)
	c, _ := syscall.UTF16PtrFromString("Stentik server agent")
	r, _, _ := user32.NewProc("MessageBoxW").Call(0, uintptr(unsafe.Pointer(t)), uintptr(unsafe.Pointer(c)), flags|0x00010000) // MB_SETFOREGROUND
	return int(r)
}

func msgBox(msg string)     { box(msg, 0x40) }
func yesNo(msg string) bool { return box(msg, 0x04|0x20) == 6 }

func ps(script string) string {
	c := exec.Command("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command",
		"[Console]::OutputEncoding=[Text.Encoding]::UTF8; "+script)
	// no console window, but NOT HideWindow: that would also hide the dialog itself (SW_HIDE applies
	// to the first window the process shows)
	c.SysProcAttr = &syscall.SysProcAttr{CreationFlags: createNoWindow}
	out, err := c.Output()
	if err != nil {
		log.Printf("powershell dialog: %v", err)
	}
	return strings.TrimSpace(string(out))
}

func q(s string) string { return "'" + strings.ReplaceAll(s, "'", "''") + "'" }

// inputBox shows a small always-on-top Windows Forms dialog (the VB InputBox could open behind other windows).
func inputBox(prompt, title, def string) string {
	return ps(`Add-Type -AssemblyName System.Windows.Forms; Add-Type -AssemblyName System.Drawing;
$f = New-Object System.Windows.Forms.Form; $f.Text = ` + q(title) + `; $f.TopMost = $true; $f.StartPosition = 'CenterScreen';
$f.FormBorderStyle = 'FixedDialog'; $f.MaximizeBox = $false; $f.MinimizeBox = $false; $f.ClientSize = New-Object System.Drawing.Size(420,150);
$l = New-Object System.Windows.Forms.Label; $l.Text = ` + q(prompt) + `; $l.SetBounds(14,12,392,48); $f.Controls.Add($l);
$t = New-Object System.Windows.Forms.TextBox; $t.Text = ` + q(def) + `; $t.SetBounds(14,66,392,24); $f.Controls.Add($t);
$ok = New-Object System.Windows.Forms.Button; $ok.Text = 'OK'; $ok.SetBounds(236,106,80,28); $ok.DialogResult = 'OK'; $f.Controls.Add($ok); $f.AcceptButton = $ok;
$c = New-Object System.Windows.Forms.Button; $c.Text = 'Cancel'; $c.SetBounds(326,106,80,28); $c.DialogResult = 'Cancel'; $f.Controls.Add($c); $f.CancelButton = $c;
$f.Add_Shown({ $f.Activate(); $t.Focus() });
if ($f.ShowDialog() -eq 'OK') { $t.Text }`)
}

func pickFolder(title string) string {
	return ps("Add-Type -AssemblyName System.Windows.Forms; $f = New-Object System.Windows.Forms.FolderBrowserDialog; $f.Description = " + q(title) +
		"; $f.ShowNewFolderButton = $false; $w = New-Object System.Windows.Forms.Form -Property @{TopMost=$true}; if ($f.ShowDialog($w) -eq 'OK') { $f.SelectedPath }")
}

func openURL(u string) { exec.Command("rundll32", "url.dll,FileProtocolHandler", u).Start() }

func installAutostart(exe string) error {
	startup := filepath.Join(os.Getenv("APPDATA"), "Microsoft", "Windows", "Start Menu", "Programs", "Startup")
	os.Remove(filepath.Join(startup, "CtC server agent.vbs"))
	vbs := "Set s = CreateObject(\"WScript.Shell\")\r\ns.Run \"\"\"" + exe + "\"\" --background\", 0, False\r\n"
	return os.WriteFile(filepath.Join(startup, "Stentik server agent.vbs"), []byte(vbs), 0o644)
}

var mutexName, _ = syscall.UTF16PtrFromString("Local\\StentikServerAgent")

func singleInstance() bool {
	h, _, err := syscall.NewLazyDLL("kernel32.dll").NewProc("CreateMutexW").Call(0, 0, uintptr(unsafe.Pointer(mutexName)))
	return h != 0 && err != syscall.Errno(183)
}

func agentRunning() bool {
	h, _, _ := syscall.NewLazyDLL("kernel32.dll").NewProc("OpenMutexW").Call(0x00100000, 0, uintptr(unsafe.Pointer(mutexName)))
	if h != 0 {
		syscall.CloseHandle(syscall.Handle(h))
		return true
	}
	return false
}

func stopOldAgent() {
	c := exec.Command("taskkill", "/F", "/IM", "stentik-agent.exe")
	hide(c)
	c.Run()
}
