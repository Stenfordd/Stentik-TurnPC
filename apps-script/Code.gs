/**
 * Stentik server panel - API (Google Apps Script)
 *
 * Nothing to configure here: the PC agent pairs itself on its first start and sets the panel
 * password. Deploy -> Web app, Execute as "Me", Who has access "Anyone".
 * To pair a different PC: Project Settings -> Script properties -> delete AGENT_KEY.
 *
 * Access requests: the CtC launcher has a "Request access" button. David gets an e-mail with Approve / Deny
 * links (and sees the requests in the web panel); an approved launcher then works without the password.
 * After updating this file: Deploy -> Manage deployments -> edit -> New version (and allow sending e-mail).
 *
 * Wake-on-LAN: Apps Script can't send the UDP magic packet itself, so an approved launcher asks for "wake" and sends
 * the packet from the friend's own PC to the router (port forward UDP 9 -> the PC). The router address and the PC's
 * MAC are not in this public repo: set WOL_HOST and WOL_MAC in Project Settings -> Script properties
 * (or fill WOL_DEFAULT in the copy you paste into the editor).
 */

var CACHE = CacheService.getScriptCache();
var PROPS = PropertiesService.getScriptProperties();
var ONLINE_SECONDS = 35;
var PENDING_SECONDS = 600;
var LOG_SIZE = 15;
var WAKE_SECONDS = 240;   // "waking up" shown this long after a wake
var WOL_DEFAULT = { host: '', mac: '', port: 9 };

function doGet(e) {
  var p = (e && e.parameter) || {};
  if (p.a && p.t) return decideLink_(p);
  return json_({ ok: true, name: 'Stentik server panel API', paired: !!PROPS.getProperty('AGENT_KEY') });
}

function doPost(e) {
  var req;
  try {
    req = JSON.parse(e.postData.contents);
  } catch (err) {
    return json_({ ok: false, error: 'bad request' });
  }
  var lock = LockService.getScriptLock();
  lock.waitLock(8000);
  try {
    switch (req.action) {
      case 'status': return json_(status_());
      case 'login': return json_(login_(req));
      case 'command': return json_(command_(req));
      case 'agent': return json_(agent_(req));
      case 'register': return json_(register_(req));
      case 'console': return json_(console_(req));
      case 'requestAccess': return json_(requestAccess_(req));
      case 'accessStatus': return json_(accessStatus_(req));
      case 'access': return json_(access_(req));
      case 'wake': return json_(wake_(req));
    }
    return json_({ ok: false, error: 'unknown action' });
  } finally {
    lock.releaseLock();
  }
}

// ---------- web page ----------

function status_() {
  var st = getJson_('status') || {};
  var seen = Number(CACHE.get('lastSeen') || 0);
  var online = seen > 0 && (Date.now() - seen) / 1000 < ONLINE_SECONDS;
  return {
    ok: true,
    paired: !!PROPS.getProperty('AGENT_KEY'),
    pc: online ? 'online' : 'offline',
    lastSeen: seen || null,
    servers: (st.servers || []).map(function (s) {
      return {
        id: s.id, name: s.name,
        state: online ? (s.state || 'stopped') : 'unknown',
        players: online ? (s.players || []) : [],
        max: s.max || 0,
        detail: online ? (s.detail || '') : ''
      };
    }),
    pending: getJson_('pending'),
    waking: !online && !!CACHE.get('wakeAt'),
    afterWake: !online ? (getJson_('afterWake') || null) : null,
    canWake: !!wolConfig_(),
    log: getJson_('log') || []
  };
}

// ---------- Wake-on-LAN ----------

function wolConfig_() {
  var host = PROPS.getProperty('WOL_HOST') || WOL_DEFAULT.host;
  var mac = PROPS.getProperty('WOL_MAC') || WOL_DEFAULT.mac;
  if (!host || !mac) return null;
  return { host: host, mac: mac, port: Number(PROPS.getProperty('WOL_PORT') || WOL_DEFAULT.port || 9) };
}

// Approved launcher (or the panel password): returns where to send the magic packet. With then:'start' the
// server is started as soon as the PC's agent checks in after booting.
function wake_(req) {
  var err = auth_(req);
  if (err) return { ok: false, error: err };
  var cfg = wolConfig_();
  if (!cfg) return { ok: false, error: 'Wake-on-LAN is not set up in the panel yet.' };
  var st = status_();
  if (st.pc === 'online') return { ok: true, online: true };
  var by = cleanName_(req.by);
  if (req.then === 'start') {
    putJson_('afterWake', { server: String(req.server || ''), by: by, at: Date.now() }, 900);
  }
  if (!CACHE.get('wakeAt')) addLog_(by + ': powered on the PC' + (req.then === 'start' ? ' + server' : ''));
  CACHE.put('wakeAt', String(Date.now()), WAKE_SECONDS);
  return { ok: true, host: cfg.host, port: cfg.port, mac: cfg.mac };
}

// The first check-in after a wake: queue the server start that was asked for.
function afterWake_(st) {
  var w = getJson_('afterWake');
  if (!w || getJson_('pending')) return;
  var list = st.servers || [], srv = null;
  list.forEach(function (s) { if (s.id === w.server) srv = s; });
  if (!srv) list.forEach(function (s) { if (!srv && /cogs|ctc|cosmos/i.test(s.id + ' ' + s.name)) srv = s; });
  if (!srv) srv = list[0];
  CACHE.remove('afterWake');
  if (!srv) return;
  if (srv.state === 'running' || srv.state === 'starting') return;
  var p = { id: Utilities.getUuid().slice(0, 8), cmd: 'start', server: srv.id, name: srv.name, by: w.by, at: Date.now() };
  putJson_('pending', p, PENDING_SECONDS);
  addLog_(w.by + ': start ' + srv.name + ' (after power on)');
}

function checkPassword_(pw) {
  var fails = Number(CACHE.get('fails') || 0);
  if (fails >= 10) return 'Too many wrong passwords, try again in 10 minutes.';
  var real = PROPS.getProperty('PANEL_PASSWORD');
  if (!real) return 'The PC agent is not set up yet.';
  if (String(pw || '') !== real) {
    CACHE.put('fails', String(fails + 1), 600);
    return 'Wrong password.';
  }
  return '';
}

// A launcher that was approved sends its device token instead of the password.
function auth_(req) {
  if (req.token) {
    var a = getAccess_().approved[String(req.token)];
    if (a) { req.by = a.name; return ''; }
    return 'Access not approved (anymore). Request it again.';
  }
  return checkPassword_(req.password);
}

function login_(req) {
  var err = checkPassword_(req.password);
  return err ? { ok: false, error: err } : { ok: true };
}

function command_(req) {
  var err = auth_(req);
  if (err) return { ok: false, error: err };
  var cmd = String(req.cmd || '');
  if (['start', 'stop', 'restart', 'shutdown'].indexOf(cmd) < 0) return { ok: false, error: 'unknown command' };
  var st = status_();
  if (st.pc !== 'online') return { ok: false, error: 'The PC is offline right now.' };
  var by0 = String(req.by || 'someone').replace(/[^\w .\-]/g, '').slice(0, 24) || 'someone';
  if (cmd === 'shutdown') {
    var sp = { id: Utilities.getUuid().slice(0, 8), cmd: 'shutdown', server: '', name: 'PC', by: by0, at: Date.now() };
    putJson_('pending', sp, PENDING_SECONDS);
    addLog_(by0 + ': shut down the PC');
    return { ok: true, pending: sp };
  }
  var srv = null;
  st.servers.forEach(function (s) { if (s.id === req.server) srv = s; });
  if (!srv) return { ok: false, error: 'unknown server' };
  var by = String(req.by || 'someone').replace(/[^\w .\-]/g, '').slice(0, 24) || 'someone';
  var p = { id: Utilities.getUuid().slice(0, 8), cmd: cmd, server: srv.id, name: srv.name, by: by, at: Date.now() };
  putJson_('pending', p, PENDING_SECONDS);
  addLog_(by + ': ' + cmd + ' ' + srv.name);
  return { ok: true, pending: p };
}

// Live console: only for logged-in users (the log contains player IPs).
function console_(req) {
  var err = auth_(req);
  if (err) return { ok: false, error: err };
  var server = String(req.server || '');
  CACHE.put('watch', server, 30); // the agent sends the console while someone is watching
  var line = String(req.line || '').trim().replace(/^\//, '').slice(0, 300);
  if (line) {
    var by = String(req.by || 'someone').replace(/[^\w .\-]/g, '').slice(0, 24) || 'someone';
    var q = getJson_('conq') || [];
    q.push({ server: server, line: line, by: by });
    putJson_('conq', q.slice(-20), 300);
    addLog_(by + ': /' + line);
  }
  var c = getJson_('console') || {};
  return { ok: true, server: c.server || '', lines: c.server === server ? (c.lines || []) : [], at: c.at || 0 };
}

// ---------- access requests (CtC launcher) ----------

function getAccess_() {
  var v = PROPS.getProperty('ACCESS');
  var a = v ? JSON.parse(v) : {};
  a.pending = a.pending || {};
  a.approved = a.approved || {};
  return a;
}

function putAccess_(a) { PROPS.setProperty('ACCESS', JSON.stringify(a)); }

function cleanName_(n) { return String(n || 'someone').replace(/[^\w .\-]/g, '').slice(0, 24) || 'someone'; }

function secret_() {
  var s = PROPS.getProperty('ADMIN_SECRET');
  if (!s) { s = Utilities.getUuid() + Utilities.getUuid(); PROPS.setProperty('ADMIN_SECRET', s); }
  return s;
}

function sign_(action, token) {
  var b = Utilities.computeHmacSha256Signature(action + ':' + token, secret_());
  return Utilities.base64EncodeWebSafe(b).replace(/=+$/, '').slice(0, 32);
}

function requestAccess_(req) {
  var token = String(req.token || '');
  if (token.length < 24 || token.length > 80) return { ok: false, error: 'bad token' };
  var a = getAccess_();
  if (a.approved[token]) return { ok: true, state: 'approved' };
  var name = cleanName_(req.name);
  var prev = a.pending[token];
  if (Object.keys(a.pending).length >= 20 && !prev) return { ok: false, error: 'Too many open requests, ask David.' };
  a.pending[token] = { name: name, at: Date.now() };
  putAccess_(a);
  // one e-mail per launcher every 10 minutes
  if (!CACHE.get('mailed_' + token)) {
    CACHE.put('mailed_' + token, '1', 600);
    addLog_(name + ' requested access');
    try {
      var url = ScriptApp.getService().getUrl();
      var link = function (act) { return url + '?a=' + act + '&t=' + encodeURIComponent(token) + '&s=' + sign_(act, token); };
      MailApp.sendEmail({
        to: Session.getEffectiveUser().getEmail(),
        subject: 'CtC server: ' + name + ' wants access',
        htmlBody: '<p><b>' + name + '</b> pressed <i>Request access</i> in the Cogs to Cosmos launcher.</p>' +
          '<p>With access they can start, restart and stop the server and use the console.</p>' +
          '<p><a href="' + link('approve') + '" style="font-size:18px">Approve</a> &nbsp; · &nbsp; ' +
          '<a href="' + link('deny') + '" style="font-size:18px">Deny</a></p>' +
          '<p style="color:#888">You can also decide, or take access away later, in the web panel.</p>'
      });
    } catch (err) {
      addLog_('access e-mail failed: ' + err);
    }
  }
  return { ok: true, state: 'pending' };
}

function accessStatus_(req) {
  var token = String(req.token || '');
  var a = getAccess_();
  if (a.approved[token]) return { ok: true, state: 'approved', name: a.approved[token].name };
  if (a.pending[token]) return { ok: true, state: 'pending' };
  if (CACHE.get('denied_' + token)) return { ok: true, state: 'denied' };
  return { ok: true, state: 'none' };
}

function decide_(act, token) {
  var a = getAccess_();
  var r = a.pending[token] || a.approved[token];
  if (!r) return null;
  delete a.pending[token];
  if (act === 'approve') {
    a.approved[token] = { name: r.name, at: Date.now() };
    addLog_(r.name + ' got access');
  } else {
    delete a.approved[token];
    CACHE.put('denied_' + token, '1', 21600);
    addLog_(r.name + (act === 'revoke' ? ' lost access' : ' was denied'));
  }
  putAccess_(a);
  return r.name;
}

// the Approve / Deny links from the e-mail
function decideLink_(p) {
  var act = p.a === 'approve' ? 'approve' : 'deny';
  var ok = String(p.s || '') === sign_(act, String(p.t));
  var lock = LockService.getScriptLock();
  lock.waitLock(8000);
  var name;
  try { name = ok ? decide_(act, String(p.t)) : null; } finally { lock.releaseLock(); }
  var msg = !ok ? 'This link is not valid.'
    : !name ? 'This request was already handled.'
    : act === 'approve' ? name + ' now has access to the server. 👍' : name + ' was denied.';
  return HtmlService.createHtmlOutput('<div style="font:18px sans-serif;padding:40px;text-align:center">' + msg + '</div>')
    .setTitle('CtC server access');
}

// web panel (password): list, approve, deny, revoke
function access_(req) {
  var err = checkPassword_(req.password);
  if (err) return { ok: false, error: err };
  var t = String(req.target || '');
  if (req.op === 'approve' || req.op === 'deny' || req.op === 'revoke') {
    if (!decide_(req.op, t)) return { ok: false, error: 'unknown request' };
  }
  var a = getAccess_();
  var list = function (o, state) {
    return Object.keys(o).map(function (k) { return { id: k, name: o[k].name, at: o[k].at, state: state }; });
  };
  return { ok: true, pending: list(a.pending, 'pending'), approved: list(a.approved, 'approved') };
}

// ---------- PC agent ----------

function register_(req) {
  var key = String(req.key || '');
  var pw = String(req.password || '');
  if (key.length < 32 || pw.length < 3) return { ok: false, error: 'key or password too short' };
  var current = PROPS.getProperty('AGENT_KEY');
  if (current && current !== key) return { ok: false, error: 'already paired with another PC (delete AGENT_KEY in the script properties to re-pair)' };
  PROPS.setProperty('AGENT_KEY', key);
  PROPS.setProperty('PANEL_PASSWORD', pw);
  addLog_(current ? 'password changed' : 'PC paired');
  return { ok: true };
}

function agent_(req) {
  var key = PROPS.getProperty('AGENT_KEY');
  if (!key || req.key !== key) return { ok: false, error: 'not paired' };
  CACHE.put('lastSeen', String(Date.now()), 21600);
  var prev = getJson_('status') || {};
  var st = req.status || {};
  putJson_('status', st, 21600);
  var before = {};
  (prev.servers || []).forEach(function (s) { before[s.id] = s.state; });
  (st.servers || []).forEach(function (s) {
    if (before[s.id] && before[s.id] !== s.state && (s.state === 'running' || s.state === 'stopped')) addLog_(s.name + ' ' + s.state);
  });
  CACHE.remove('wakeAt');
  afterWake_(st);
  if (req.console && req.console.server) {
    putJson_('console', { server: req.console.server, lines: req.console.lines || [], at: Date.now() }, 120);
  }
  var p = getJson_('pending');
  if (p && req.ack && req.ack === p.id) {
    CACHE.remove('pending');
    p = null;
  }
  var q = getJson_('conq') || [];
  if (q.length) CACHE.remove('conq');
  return { ok: true, cmd: p, watch: CACHE.get('watch') || '', conq: q };
}

// ---------- helpers ----------

function addLog_(text) {
  var log = getJson_('log') || [];
  log.unshift({ t: Date.now(), text: text });
  putJson_('log', log.slice(0, LOG_SIZE), 21600);
}

function getJson_(k) {
  var v = CACHE.get(k);
  return v ? JSON.parse(v) : null;
}

function putJson_(k, v, sec) {
  CACHE.put(k, JSON.stringify(v), sec);
}

function json_(o) {
  return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);
}
