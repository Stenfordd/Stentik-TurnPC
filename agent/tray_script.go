package main

// trayPS is the tray icon (PowerShell + Windows Forms, no extra install). It talks to the agent's local API.
// Icon colour: grey = no server running, orange = starting/stopping, green = running.
// Right-click: per server Open console / Start / Stop / Restart / Open folder; double-click opens the console.
const trayPS = `
$ErrorActionPreference = 'SilentlyContinue'
$mtx = New-Object System.Threading.Mutex($false, 'Local\StentikServerTray')
if (-not $mtx.WaitOne(0)) { exit }
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()
$global:api = 'http://127.0.0.1:47811'
$global:hdr = @{ 'X-Stentik' = '1' }

function global:Get-Status([string]$watch) {
  $u = "$global:api/status"; if ($watch) { $u += "?watch=" + [uri]::EscapeDataString($watch) }
  try { return Invoke-RestMethod -Uri $u -Headers $global:hdr -TimeoutSec 3 } catch { return $null }
}
function global:Send-Do($body) {
  try { Invoke-RestMethod -Uri "$global:api/do" -Method Post -Headers $global:hdr -ContentType 'application/json' -Body ($body | ConvertTo-Json) -TimeoutSec 5 | Out-Null } catch {}
}
function global:New-Dot([System.Drawing.Color]$c) {
  $b = New-Object System.Drawing.Bitmap 16, 16
  $g = [System.Drawing.Graphics]::FromImage($b)
  $g.SmoothingMode = 'AntiAlias'
  $g.FillRectangle((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(60, 60, 64))), 1, 3, 14, 11)
  $g.FillEllipse((New-Object System.Drawing.SolidBrush $c), 4, 5, 8, 8)
  $g.DrawRectangle((New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(20, 20, 20))), 1, 3, 13, 10)
  $g.Dispose()
  return [System.Drawing.Icon]::FromHandle($b.GetHicon())
}
$global:icons = @{
  stopped  = New-Dot ([System.Drawing.Color]::FromArgb(140, 140, 140))
  starting = New-Dot ([System.Drawing.Color]::FromArgb(240, 160, 40))
  running  = New-Dot ([System.Drawing.Color]::FromArgb(70, 200, 90))
}

$global:ni = New-Object System.Windows.Forms.NotifyIcon
$global:ni.Icon = $global:icons.stopped
$global:ni.Text = 'Minecraft servers'
$global:ni.Visible = $true
$global:menu = New-Object System.Windows.Forms.ContextMenuStrip
$global:ni.ContextMenuStrip = $global:menu
$global:last = $null
$global:prev = @{}
$global:forms = @{}

function global:Open-Console($s) {
  if ($global:forms.ContainsKey($s.id) -and -not $global:forms[$s.id].IsDisposed) { $global:forms[$s.id].Activate(); return }
  $f = New-Object System.Windows.Forms.Form
  $f.Text = "Console - " + $s.name
  $f.Size = New-Object System.Drawing.Size(900, 560)
  $f.StartPosition = 'CenterScreen'
  $f.Icon = $global:icons.running
  $tb = New-Object System.Windows.Forms.TextBox
  $tb.Multiline = $true; $tb.ReadOnly = $true; $tb.ScrollBars = 'Vertical'; $tb.WordWrap = $true
  $tb.Font = New-Object System.Drawing.Font('Consolas', 9)
  $tb.BackColor = [System.Drawing.Color]::FromArgb(24, 24, 28); $tb.ForeColor = [System.Drawing.Color]::FromArgb(220, 220, 220)
  $tb.Dock = 'Fill'
  $bottom = New-Object System.Windows.Forms.Panel; $bottom.Dock = 'Bottom'; $bottom.Height = 34
  $in = New-Object System.Windows.Forms.TextBox; $in.Font = New-Object System.Drawing.Font('Consolas', 10)
  $in.SetBounds(6, 6, 640, 24); $in.Anchor = 'Left,Right,Top'
  $send = New-Object System.Windows.Forms.Button; $send.Text = 'Send'; $send.SetBounds(652, 4, 70, 26); $send.Anchor = 'Right,Top'
  $stop = New-Object System.Windows.Forms.Button; $stop.Text = 'Stop server'; $stop.SetBounds(728, 4, 150, 26); $stop.Anchor = 'Right,Top'
  $bottom.Controls.AddRange(@($in, $send, $stop))
  $f.Controls.Add($tb); $f.Controls.Add($bottom)
  $id = $s.id
  $doSend = { if ($in.Text.Trim()) { Send-Do @{ server = $id; line = $in.Text }; $in.Text = '' } }.GetNewClosure()
  $send.Add_Click($doSend)
  $in.Add_KeyDown({ param($o, $e) if ($e.KeyCode -eq 'Enter') { $e.SuppressKeyPress = $true; & $doSend } }.GetNewClosure())
  $stop.Add_Click({ if ([System.Windows.Forms.MessageBox]::Show('Stop the server? Players get a 10 second warning.', 'Stop server', 'YesNo', 'Question') -eq 'Yes') { Send-Do @{ server = $id; cmd = 'stop' } } }.GetNewClosure())
  $t = New-Object System.Windows.Forms.Timer; $t.Interval = 1500
  $seen = @{ t = '' }
  $t.Add_Tick({
    $st = Get-Status $id
    if (-not $st) { return }
    $me = $st.servers | Where-Object { $_.id -eq $id }
    if (-not $me) { return }
    $f.Text = $me.name + " - " + $me.state + $(if ($me.state -eq 'running') { ", " + @($me.players).Count + " players" } else { '' })
    $txt = ($me.lines -join "` + "`r`n" + `")
    if ($txt -ne $seen.t) {
      $seen.t = $txt
      $tb.Text = $txt; $tb.SelectionStart = $tb.TextLength; $tb.ScrollToCaret()
    }
  }.GetNewClosure())
  $f.Add_FormClosed({ $t.Stop() }.GetNewClosure())
  $t.Start()
  $global:forms[$id] = $f
  $f.Show(); $f.Activate(); $in.Focus()
}

function global:Build-Menu {
  $global:menu.Items.Clear()
  $st = $global:last
  if (-not $st -or -not $st.servers) {
    $na = $global:menu.Items.Add('Server agent not reachable'); $na.Enabled = $false
  } else {
    foreach ($s in $st.servers) {
      $sv = $s
      $head = $global:menu.Items.Add($s.name + '  -  ' + $s.state + $(if ($s.state -eq 'running') { ' (' + @($s.players).Count + '/' + $s.max + ')' } else { '' }))
      $head.Font = New-Object System.Drawing.Font($head.Font, [System.Drawing.FontStyle]::Bold)
      $live = $s.state -eq 'running' -or $s.state -eq 'starting'
      $i = $global:menu.Items.Add('    Open console'); $i.Add_Click({ Open-Console $sv }.GetNewClosure())
      if ($live) {
        $i = $global:menu.Items.Add('    Restart'); $i.Add_Click({ Send-Do @{ server = $sv.id; cmd = 'restart' } }.GetNewClosure())
        $i = $global:menu.Items.Add('    Stop'); $i.Add_Click({ Send-Do @{ server = $sv.id; cmd = 'stop' } }.GetNewClosure())
      } else {
        $i = $global:menu.Items.Add('    Start'); $i.Add_Click({ Send-Do @{ server = $sv.id; cmd = 'start' } }.GetNewClosure())
      }
      $i = $global:menu.Items.Add('    Open folder'); $i.Add_Click({ Start-Process explorer.exe $sv.dir }.GetNewClosure())
      $global:menu.Items.Add('-') | Out-Null
    }
  }
  $i = $global:menu.Items.Add('Web panel'); $i.Add_Click({ Start-Process 'https://stenfordd.github.io/Stentik-TurnPC/' })
  $i = $global:menu.Items.Add('Hide this icon'); $i.Add_Click({ $global:ni.Visible = $false; [System.Windows.Forms.Application]::Exit() })
}
$global:menu.Add_Opening({ Build-Menu })
Build-Menu

$global:ni.Add_DoubleClick({
  $st = $global:last
  if ($st -and $st.servers) {
    $s = $st.servers | Where-Object { $_.state -eq 'running' -or $_.state -eq 'starting' } | Select-Object -First 1
    if (-not $s) { $s = $st.servers | Sort-Object { [int64]$_.logAge } | Select-Object -First 1 }
    Open-Console $s
  }
})

$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 3000
$timer.Add_Tick({
  $st = Get-Status ''
  $global:last = $st
  if (-not $st) { $global:ni.Icon = $global:icons.stopped; $global:ni.Text = 'Server agent not running'; return }
  $run = @($st.servers | Where-Object { $_.state -eq 'running' })
  $busy = @($st.servers | Where-Object { $_.state -eq 'starting' -or $_.state -eq 'stopping' })
  if ($busy.Count) { $global:ni.Icon = $global:icons.starting } elseif ($run.Count) { $global:ni.Icon = $global:icons.running } else { $global:ni.Icon = $global:icons.stopped }
  $tip = if ($run.Count) { ($run | ForEach-Object { $_.name + ': ' + @($_.players).Count + ' online' }) -join "` + "`n" + `" } elseif ($busy.Count) { $busy[0].name + ': ' + $busy[0].state } else { 'No server running' }
  if ($tip.Length -gt 63) { $tip = $tip.Substring(0, 63) }
  $global:ni.Text = $tip
  foreach ($s in $st.servers) {
    $old = $global:prev[$s.id]
    if ($old -and $old -ne $s.state) {
      $who = if ($s.by -and $s.by -ne 'PC') { ' by ' + $s.by } else { '' }
      if ($s.state -eq 'starting') { $global:ni.ShowBalloonTip(5000, $s.name, 'Server is starting' + $who + '. Double-click the icon for the console.', 'Info') }
      elseif ($s.state -eq 'running') { $global:ni.ShowBalloonTip(4000, $s.name, 'Server is up.', 'Info') }
      elseif ($s.state -eq 'stopped') { $global:ni.ShowBalloonTip(4000, $s.name, 'Server stopped.', 'Info') }
    }
    $global:prev[$s.id] = $s.state
  }
})
$timer.Start()
[System.Windows.Forms.Application]::Run()
$global:ni.Visible = $false
`
