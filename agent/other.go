//go:build !windows

package main

import (
	"fmt"
	"os/exec"
)

func hide(cmd *exec.Cmd)                        {}
func detach(cmd *exec.Cmd)                      {}
func msgBox(msg string)                         { fmt.Println(msg) }
func yesNo(msg string) bool                     { fmt.Println(msg); return true }
func inputBox(prompt, title, def string) string { return def }
func pickFolder(title string) string            { return "" }
func openURL(u string)                          {}
func installAutostart(exe string) error         { return nil }
func singleInstance() bool                      { return true }
func agentRunning() bool                        { return false }
func stopOldAgent()                             {}
func powerOff()                                 {}
