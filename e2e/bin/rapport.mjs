#!/usr/bin/env node
// Bygger en selvstændig HTML-rapport ud fra en kørsels resultat.json.
// Skærmbilleder indlejres som base64, så filen kan sendes videre uden mappen omkring sig.
//
//   node e2e/bin/rapport.mjs <kørsels-id> [--ingen-indlejring]

import { readFileSync, writeFileSync, existsSync, statSync, copyFileSync, readdirSync } from 'node:fs';
import { join, dirname, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const rod = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const kørselId = process.argv[2];
const indlejr = !process.argv.includes('--ingen-indlejring');
if (!kørselId) { console.error('Brug: node e2e/bin/rapport.mjs <kørsels-id>'); process.exit(1); }

const kørselDir = join(rod, 'e2e', 'runs', kørselId);
const resultatSti = join(kørselDir, 'resultat.json');
if (!existsSync(resultatSti)) {
  console.error(`Kørslen ${kørselId} findes ikke (ledte efter ${resultatSti}).`);
  const runsDir = join(rod, 'e2e', 'runs');
  if (existsSync(runsDir)) console.error(`Kendte kørsler: ${readdirSync(runsDir).join(', ') || '(ingen)'}`);
  process.exit(1);
}
const data = JSON.parse(readFileSync(resultatSti, 'utf8'));

const STATUS = {
  bestaaet:      { tekst: 'Bestået',      klasse: 'ok' },
  fejlet:        { tekst: 'Fejlet',       klasse: 'fejl' },
  blokeret:      { tekst: 'Blokeret',     klasse: 'blok' },
  sprunget_over: { tekst: 'Sprunget over', klasse: 'spring' },
  ikke_koert:    { tekst: 'Ikke kørt',    klasse: 'ukendt' },
};
const ALVOR = {
  kritisk: { tekst: 'Kritisk', vægt: 0 },
  hoej:    { tekst: 'Høj',     vægt: 1 },
  middel:  { tekst: 'Middel',  vægt: 2 },
  lav:     { tekst: 'Lav',     vægt: 3 },
};

const esc = (s) => String(s ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
// Drejebogen bruger `backticks` til URL'er og feltnavne — vis dem som kode.
const md = (s) => esc(s).replace(/`([^`]+)`/g, '<code>$1</code>');

const MIME = { '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp' };
const manglendeSkud = new Set();
let indlejretBytes = 0;
// Samme skærmbillede nævnes ofte på både et trin og et fund; det indlejres kun én gang.
const skudBank = new Map();

function billede(navn) {
  const sti = join(kørselDir, 'skud', navn);
  if (!existsSync(sti)) { manglendeSkud.add(navn); return null; }
  if (!skudBank.has(navn) && indlejr) {
    indlejretBytes += statSync(sti).size;
    const mime = MIME[extname(navn).toLowerCase()] || 'image/png';
    skudBank.set(navn, `data:${mime};base64,${readFileSync(sti).toString('base64')}`);
  }
  return navn;
}

function skudGalleri(navne, tekst) {
  const billeder = (navne || []).map((n) => ({ navn: n, kilde: billede(n) })).filter((b) => b.kilde);
  if (!billeder.length) return '';
  return `<div class="skud">${billeder.map((b) => `
    <figure><img ${indlejr ? `data-skud="${esc(b.navn)}"` : `src="skud/${esc(encodeURIComponent(b.navn))}"`} alt="${esc(tekst || b.navn)}" loading="lazy">
    <figcaption>${esc(b.navn)}</figcaption></figure>`).join('')}</div>`;
}

// --- optælling ------------------------------------------------------------
const alleTilfælde = data.moduler.flatMap((m) => m.tilfaelde.map((t) => ({ ...t, modul: m.navn })));
const tæl = (liste) => liste.reduce((a, t) => { a[t.status] = (a[t.status] || 0) + 1; return a; }, {});
const total = tæl(alleTilfælde);
const kørt = alleTilfælde.filter((t) => t.status !== 'ikke_koert').length;
const bestået = total.bestaaet || 0;

const alleFund = data.moduler.flatMap((m) => m.tilfaelde.flatMap((t) =>
  (t.fund || []).map((f) => ({ ...f, tilfaelde: t.id, titel: t.titel, modul: m.navn }))))
  .sort((a, b) => (ALVOR[a.alvor]?.vægt ?? 9) - (ALVOR[b.alvor]?.vægt ?? 9));

// Krav → status. Et krav er kun dækket hvis alle dets tilfælde er bestået.
const kravKort = new Map();
for (const t of alleTilfælde) {
  for (const k of t.krav || []) {
    if (!kravKort.has(k)) kravKort.set(k, []);
    kravKort.get(k).push(t);
  }
}
const kravRækker = [...kravKort.entries()].sort().map(([krav, tilfælde]) => {
  const status = tilfælde.some((t) => t.status === 'fejlet') ? 'fejlet'
    : tilfælde.some((t) => t.status === 'blokeret') ? 'blokeret'
    : tilfælde.every((t) => t.status === 'bestaaet') ? 'bestaaet'
    : tilfælde.some((t) => t.status === 'bestaaet') ? 'delvist' : 'ikke_koert';
  return { krav, status, tilfælde };
});
const kravOk = kravRækker.filter((k) => k.status === 'bestaaet').length;

const dato = (iso) => iso ? new Date(iso).toLocaleString('da-DK', { dateStyle: 'long', timeStyle: 'short' }) : '—';

// --- html -----------------------------------------------------------------
const chip = (status) => {
  const s = STATUS[status] || { tekst: status, klasse: 'ukendt' };
  return `<span class="chip ${s.klasse}">${esc(s.tekst)}</span>`;
};

const fundHtml = alleFund.length ? alleFund.map((f, i) => `
  <article class="fund ${esc(f.alvor)}">
    <header>
      <span class="alvor ${esc(f.alvor)}">${esc(ALVOR[f.alvor]?.tekst || f.alvor)}</span>
      <h3>${md(f.resume)}</h3>
      <span class="ref">${esc(f.tilfaelde)}${f.trin ? ` · trin ${esc(f.trin)}` : ''} · ${esc(f.modul)}</span>
    </header>
    ${f.detalje ? `<p>${md(f.detalje)}</p>` : ''}
    ${skudGalleri(f.skud, f.resume)}
  </article>`).join('') : '<p class="tom">Ingen fund registreret i denne kørsel.</p>';

const modulHtml = data.moduler.map((m) => {
  const mTotal = tæl(m.tilfaelde);
  const harFejl = (mTotal.fejlet || 0) + (mTotal.blokeret || 0) > 0;
  return `
  <section class="modul">
    <h2>${esc(m.navn)} <span class="kode">${esc(m.kode)}</span></h2>
    <p class="modulopsummering">
      ${m.tilfaelde.length} tilfælde ·
      ${mTotal.bestaaet || 0} bestået · ${mTotal.fejlet || 0} fejlet ·
      ${mTotal.blokeret || 0} blokeret · ${mTotal.sprunget_over || 0} sprunget over ·
      ${mTotal.ikke_koert || 0} ikke kørt
    </p>
    ${m.tilfaelde.map((t) => `
      <details class="tilfaelde ${STATUS[t.status]?.klasse}" ${harFejl && (t.status === 'fejlet' || t.status === 'blokeret') ? 'open' : ''}>
        <summary>
          ${chip(t.status)}
          <span class="tid">${esc(t.id)}</span>
          <span class="titel">${esc(t.titel)}</span>
          <span class="krav">${t.krav.map(esc).join(', ')}</span>
        </summary>
        <div class="krop">
          ${t.forudsaetning ? `<p class="meta"><strong>Forudsætning:</strong> ${md(t.forudsaetning)}</p>` : ''}
          ${t.start ? `<p class="meta"><strong>Start:</strong> ${md(t.start)}</p>` : ''}
          ${t.bemaerk ? `<p class="bemaerk">${md(t.bemaerk)}</p>` : ''}
          <table class="trin">
            <thead><tr><th>#</th><th>Trin</th><th>Forventet</th><th>Observeret</th><th>Status</th></tr></thead>
            <tbody>
              ${t.trin.map((s) => `
                <tr class="${STATUS[s.status]?.klasse}">
                  <td class="nr">${s.nr}</td>
                  <td>${md(s.trin)}</td>
                  <td>${md(s.forventet)}</td>
                  <td>${s.observeret ? md(s.observeret) : '<span class="tom">—</span>'}</td>
                  <td>${chip(s.status)}</td>
                </tr>
                ${(s.skud || []).length ? `<tr class="skudraekke"><td></td><td colspan="4">${skudGalleri(s.skud, `${t.id} trin ${s.nr}`)}</td></tr>` : ''}
              `).join('')}
            </tbody>
          </table>
        </div>
      </details>`).join('')}
  </section>`;
}).join('');

const html = `<!doctype html>
<html lang="da">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>E2E-rapport ${esc(data.koersel.id)}</title>
<style>
  :root {
    --bg: #fbfbfa; --kort: #fff; --tekst: #1c1b19; --dæmpet: #6b6a67; --kant: #e3e1dd;
    --ok: #1f7a4d; --ok-bg: #e6f4ec; --fejl: #b3261e; --fejl-bg: #fdeceb;
    --blok: #8a5a00; --blok-bg: #fdf3e0; --spring: #5a5a5a; --spring-bg: #efefee;
    --ukendt: #6b6a67; --ukendt-bg: #f2f1ef; --accent: #2d5bd7;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --bg: #16150f; --kort: #1f1e19; --tekst: #f0eee9; --dæmpet: #a6a49e; --kant: #35332c;
      --ok: #6fcf97; --ok-bg: #16301f; --fejl: #ff8a80; --fejl-bg: #3a1a18;
      --blok: #f0c060; --blok-bg: #35290f; --spring: #a6a49e; --spring-bg: #26251f;
      --ukendt: #a6a49e; --ukendt-bg: #26251f; --accent: #8fb0ff;
    }
  }
  * { box-sizing: border-box; }
  body { margin: 0; padding: 0 1.5rem 5rem; background: var(--bg); color: var(--tekst);
         font: 15px/1.6 -apple-system, "Segoe UI", Roboto, "Helvetica Neue", sans-serif; }
  .side { max-width: 1180px; margin: 0 auto; }
  header.top { padding: 2.5rem 0 1.5rem; border-bottom: 2px solid var(--kant); }
  header.top h1 { margin: 0 0 .35rem; font-size: 1.9rem; letter-spacing: -.02em; }
  .stamdata { display: flex; flex-wrap: wrap; gap: .35rem 1.5rem; color: var(--dæmpet); font-size: .875rem; }
  .stamdata b { color: var(--tekst); font-weight: 600; }

  .noegletal { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 1rem; margin: 1.75rem 0; }
  .tal { background: var(--kort); border: 1px solid var(--kant); border-radius: 10px; padding: 1rem 1.1rem; }
  .tal .v { font-size: 2rem; font-weight: 650; line-height: 1.1; letter-spacing: -.02em; }
  .tal .e { color: var(--dæmpet); font-size: .8rem; text-transform: uppercase; letter-spacing: .06em; }
  .tal.ok .v { color: var(--ok); } .tal.fejl .v { color: var(--fejl); } .tal.blok .v { color: var(--blok); }

  .bjaelke { display: flex; height: 10px; border-radius: 999px; overflow: hidden; background: var(--ukendt-bg); margin: 0 0 2rem; }
  .bjaelke i { display: block; }
  .bjaelke .b-ok { background: var(--ok); } .bjaelke .b-fejl { background: var(--fejl); }
  .bjaelke .b-blok { background: var(--blok); } .bjaelke .b-spring { background: var(--spring); }

  h2 { font-size: 1.25rem; margin: 2.5rem 0 .75rem; letter-spacing: -.01em; }
  h2 .kode { color: var(--dæmpet); font-weight: 400; font-size: .8rem; border: 1px solid var(--kant);
             border-radius: 5px; padding: .1rem .4rem; vertical-align: middle; margin-left: .4rem; }

  .chip { display: inline-block; font-size: .72rem; font-weight: 600; padding: .12rem .5rem; border-radius: 999px;
          white-space: nowrap; letter-spacing: .02em; }
  .chip.ok { color: var(--ok); background: var(--ok-bg); }
  .chip.fejl { color: var(--fejl); background: var(--fejl-bg); }
  .chip.blok { color: var(--blok); background: var(--blok-bg); }
  .chip.spring { color: var(--spring); background: var(--spring-bg); }
  .chip.ukendt { color: var(--ukendt); background: var(--ukendt-bg); }

  .fund { background: var(--kort); border: 1px solid var(--kant); border-left: 4px solid var(--dæmpet);
          border-radius: 8px; padding: 1rem 1.15rem; margin-bottom: .85rem; }
  .fund.kritisk, .fund.hoej { border-left-color: var(--fejl); }
  .fund.middel { border-left-color: var(--blok); }
  .fund header { display: flex; align-items: baseline; gap: .6rem; flex-wrap: wrap; }
  .fund h3 { margin: 0; font-size: 1rem; flex: 1 1 auto; }
  .fund .ref { color: var(--dæmpet); font-size: .8rem; font-variant-numeric: tabular-nums; }
  .fund p { margin: .6rem 0 0; color: var(--dæmpet); }
  .alvor { font-size: .7rem; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; }
  .alvor.kritisk, .alvor.hoej { color: var(--fejl); }
  .alvor.middel { color: var(--blok); } .alvor.lav { color: var(--dæmpet); }

  table { width: 100%; border-collapse: collapse; font-size: .875rem; }
  th { text-align: left; font-size: .72rem; text-transform: uppercase; letter-spacing: .06em;
       color: var(--dæmpet); font-weight: 600; padding: .4rem .6rem; border-bottom: 1px solid var(--kant); }
  td { padding: .5rem .6rem; border-bottom: 1px solid var(--kant); vertical-align: top; }
  td.nr { color: var(--dæmpet); font-variant-numeric: tabular-nums; width: 2rem; }
  tr.fejl td { background: var(--fejl-bg); }
  .skudraekke td { border-bottom: 1px solid var(--kant); }
  .krav-tabel { background: var(--kort); border: 1px solid var(--kant); border-radius: 8px; overflow: hidden; }
  .krav-tabel td, .krav-tabel th { padding: .45rem .8rem; }

  details.tilfaelde { background: var(--kort); border: 1px solid var(--kant); border-radius: 8px;
                      margin-bottom: .5rem; overflow: hidden; }
  details.tilfaelde.fejl { border-color: var(--fejl); }
  summary { cursor: pointer; padding: .6rem .9rem; display: flex; align-items: center; gap: .7rem; flex-wrap: wrap; }
  summary::-webkit-details-marker { display: none; }
  summary .tid { font-variant-numeric: tabular-nums; font-weight: 650; font-size: .85rem; }
  summary .titel { flex: 1 1 auto; }
  summary .krav { color: var(--dæmpet); font-size: .78rem; }
  .krop { padding: .3rem .9rem 1rem; border-top: 1px solid var(--kant); }
  .meta { margin: .5rem 0; font-size: .85rem; color: var(--dæmpet); }
  .bemaerk { margin: .5rem 0; padding: .5rem .7rem; background: var(--blok-bg); color: var(--blok);
             border-radius: 6px; font-size: .85rem; }
  .modulopsummering { color: var(--dæmpet); font-size: .82rem; margin: 0 0 .8rem; }

  code { font: 500 .85em/1 ui-monospace, "SF Mono", Menlo, monospace; background: var(--ukendt-bg);
         padding: .1rem .3rem; border-radius: 4px; }
  .tom { color: var(--dæmpet); font-style: italic; }

  .skud { display: flex; flex-wrap: wrap; gap: .7rem; margin: .7rem 0 .3rem; }
  .skud figure { margin: 0; width: 260px; }
  .skud img { width: 100%; border: 1px solid var(--kant); border-radius: 6px; cursor: zoom-in; display: block; background: var(--kort); }
  .skud figcaption { font-size: .7rem; color: var(--dæmpet); margin-top: .25rem; word-break: break-all; }
  dialog { border: none; background: transparent; max-width: 96vw; max-height: 96vh; padding: 0; }
  dialog::backdrop { background: rgba(0,0,0,.85); }
  dialog img { max-width: 96vw; max-height: 96vh; border-radius: 6px; cursor: zoom-out; }
  footer { margin-top: 3rem; padding-top: 1rem; border-top: 1px solid var(--kant); color: var(--dæmpet); font-size: .8rem; }
  @media print { .skud figure { width: 200px; } }
</style>
</head>
<body>
<div class="side">

<header class="top">
  <h1>E2E-testrapport</h1>
  <div class="stamdata">
    <span>Kørsel <b>${esc(data.koersel.id)}</b></span>
    <span>Branch <b>${esc(data.koersel.branch)}</b> @ <b>${esc(data.koersel.commit)}</b></span>
    <span>Udført af <b>${esc(data.koersel.udfoert_af)}</b></span>
    <span>Startet <b>${dato(data.koersel.startet)}</b></span>
    <span>Afsluttet <b>${dato(data.koersel.afsluttet)}</b></span>
    <span>Miljø <b>${esc(data.koersel.url)}</b></span>
  </div>
</header>

<div class="noegletal">
  <div class="tal"><div class="v">${alleTilfælde.length}</div><div class="e">Testtilfælde</div></div>
  <div class="tal ok"><div class="v">${bestået}</div><div class="e">Bestået</div></div>
  <div class="tal fejl"><div class="v">${total.fejlet || 0}</div><div class="e">Fejlet</div></div>
  <div class="tal blok"><div class="v">${total.blokeret || 0}</div><div class="e">Blokeret</div></div>
  ${total.sprunget_over ? `<div class="tal"><div class="v">${total.sprunget_over}</div><div class="e">Sprunget over</div></div>` : ''}
  <div class="tal"><div class="v">${total.ikke_koert || 0}</div><div class="e">Ikke kørt</div></div>
  <div class="tal"><div class="v">${kravOk}/${kravRækker.length}</div><div class="e">Krav dækket</div></div>
</div>

<div class="bjaelke" title="${bestået} bestået af ${alleTilfælde.length}">
  ${[['b-ok', bestået], ['b-fejl', total.fejlet || 0], ['b-blok', total.blokeret || 0], ['b-spring', total.sprunget_over || 0]]
    .filter(([, n]) => n > 0)
    .map(([k, n]) => `<i class="${k}" style="width:${(n / alleTilfælde.length * 100).toFixed(2)}%"></i>`).join('')}
</div>

${data.koersel.noter ? `<p class="bemaerk">${md(data.koersel.noter)}</p>` : ''}

<h2>Fund${alleFund.length ? ` <span class="kode">${alleFund.length}</span>` : ''}</h2>
${fundHtml}

<h2>Kravdækning</h2>
<table class="krav-tabel">
  <thead><tr><th>Krav</th><th>Status</th><th>Efterprøvet af</th></tr></thead>
  <tbody>
    ${kravRækker.map((k) => `<tr>
      <td><b>${esc(k.krav)}</b></td>
      <td>${k.status === 'delvist' ? '<span class="chip blok">Delvist</span>' : chip(k.status)}</td>
      <td>${k.tilfælde.map((t) => esc(t.id)).join(', ')}</td>
    </tr>`).join('')}
  </tbody>
</table>

<h2>Detaljer</h2>
${modulHtml}

<footer>
  Genereret ${dato(new Date().toISOString())} fra <code>e2e/runs/${esc(data.koersel.id)}/resultat.json</code>.
  Testtilfældene stammer fra <code>e2e/drejebog/</code>, kravene fra <code>e2e/krav.md</code>.
  ${manglendeSkud.size ? `<br><b>${manglendeSkud.size} skærmbillede(r) manglede:</b> ${[...manglendeSkud].map(esc).join(', ')}` : ''}
</footer>

</div>
<dialog id="lup"><img alt=""></dialog>
<script id="skudbank" type="application/json">__SKUDBANK__</script>
<script>
  const skud = JSON.parse(document.getElementById('skudbank').textContent);
  for (const img of document.querySelectorAll('img[data-skud]')) {
    const kilde = skud[img.dataset.skud];
    if (kilde) img.src = kilde;
  }
  const lup = document.getElementById('lup');
  document.addEventListener('click', (e) => {
    if (e.target.matches('.skud img')) { lup.querySelector('img').src = e.target.src; lup.showModal(); }
    else if (e.target.closest('dialog')) lup.close();
  });
</script>
</body>
</html>`;

const ud = join(kørselDir, 'rapport.html');
// Skærmbillederne indsættes til sidst, så hver fil kun optræder én gang i dokumentet.
// Funktionen som erstatning: et $ i et filnavn må ikke læses som en erstatningsreference.
const bank = JSON.stringify(Object.fromEntries(skudBank)).replace(/</g, '\\u003c');
writeFileSync(ud, html.replace('__SKUDBANK__', () => bank));
const rodKopi = join(rod, 'e2e-rapport.html');
copyFileSync(ud, rodKopi);

const mb = (n) => (n / 1024 / 1024).toFixed(1);
console.log(`Rapport: ${ud}`);
console.log(`Kopi:    ${rodKopi}`);
console.log(`${alleTilfælde.length} tilfælde (${bestået} bestået, ${total.fejlet || 0} fejlet, ${total.blokeret || 0} blokeret, ${total.ikke_koert || 0} ikke kørt), ${alleFund.length} fund, ${kørt} kørt`);
if (indlejr && indlejretBytes) {
  console.log(`Indlejrede skærmbilleder: ${mb(indlejretBytes)} MB`);
  if (indlejretBytes > 25 * 1024 * 1024) {
    console.warn('Rapporten er tung. Gem skærmbilleder som .jpg, eller dan den med --ingen-indlejring.');
  }
}
if (manglendeSkud.size) console.warn(`Manglende skærmbilleder: ${[...manglendeSkud].join(', ')}`);
