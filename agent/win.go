//go:build windows

package main

import (
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
	hide(c)
	out, _ := c.Output()
	return strings.TrimSpace(string(out))
}

func q(s string) string { return "'" + strings.ReplaceAll(s, "'", "''") + "'" }

func inputBox(prompt, title, def string) string {
	return ps("Add-Type -AssemblyName Microsoft.VisualBasic; [Microsoft.VisualBasic.Interaction]::InputBox(" + q(prompt) + "," + q(title) + "," + q(def) + ")")
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
