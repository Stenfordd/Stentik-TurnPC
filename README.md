# Stentik Servers – ovládací panel

Stránka: **https://stenfordd.github.io/Stentik-TurnPC/**

Kamarádi se tam přihlásí heslem a můžou spustit / restartovat / zastavit Minecraft servery
na Stentikově PC (CREATE: Cogs to Cosmos, ATM10, …). Běží vždy jen jeden server.

## Jak to funguje
`index.html` (GitHub Pages) ⇄ `apps-script/Code.gs` (Google Apps Script, drží heslo a příkazy)
⇄ `stentik-agent.exe` na PC (spouští servery, posílá stav a hráče).

## Nastavení (jednou)
1. **Apps Script:** obsah `apps-script/Code.gs` vlož do projektu na script.google.com →
   *Nasadit → Spravovat nasazení → ✏️ → Verze: Nová verze → Nasadit* (adresa zůstane stejná).
2. **PC:** stáhni `download/stentik-agent.exe` a spusť ho. Najde servery, zeptá se na heslo
   panelu a sám se spáruje. Pak běží na pozadí a startuje s Windows.

Přidat další server: spusť `stentik-agent.exe` znovu → „Ano“ → najde i nové složky.

## Soubory
- `index.html`, `config.js`, `icon.svg` – stránka
- `apps-script/Code.gs` – API
- `agent/` – zdrojový kód agenta (Go), `download/stentik-agent.exe` – hotový program
