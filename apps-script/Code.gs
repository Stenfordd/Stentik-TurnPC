/**
 * Stentik server panel - API (Google Apps Script)
 *
 * Nothing to configure here: the PC agent pairs itself on its first start and sets the panel
 * password. Deploy -> Web app, Execute as "Me", Who has access "Anyone".
 * To pair a different PC: Project Settings -> Script properties -> delete AGENT_KEY.
 */

var CACHE = CacheService.getScriptCache();
var PROPS = PropertiesService.getScriptProperties();
var ONLINE_SECONDS = 35;
var PENDING_SECONDS = 600;
var LOG_SIZE = 15;

function doGet() {
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
    log: getJson_('log') || []
  };
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

function login_(req) {
  var err = checkPassword_(req.password);
  return err ? { ok: false, error: err } : { ok: true };
}

function command_(req) {
  var err = checkPassword_(req.password);
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
  var err = checkPassword_(req.password);
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
