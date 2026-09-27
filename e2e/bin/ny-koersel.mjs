#!/usr/bin/env node
// Læser drejebogen og bygger et tomt resultatskema for en ny kørsel.
// Drejebogen er eneste kilde til hvilke tilfælde der findes — skemaet kan ikke drive fra den.
import { readFileSync, readdirSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { execSync } from 'node:child_process';
import { join, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const rod = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const drejebogDir = join(rod, 'e2e', 'drejebog');

const NØGLER = {
  'krav': 'krav', 'forudsætning': 'forudsaetning', 'start': 'start',
  'skærmbillede': 'skaermbillede', 'bemærk': 'bemaerk', 'efterlader': 'efterlader',
};

function parsModul(tekst, filnavn) {
  const linjer = tekst.split('\n');
  const navn = (linjer.find(l => l.startsWith('# ')) || '# ?').slice(2).trim();
  const kode = (linjer.find(l => l.startsWith('Kode:')) || 'Kode: ?').slice(5).trim();
  const tilfaelde = [];
  let nu = null;

  for (const linje of linjer) {
    const overskrift = linje.match(/^###\s+([A-ZÆØÅ0-9]+-\d+)\s+[—–-]\s+(.+)$/);
    if (!overskrift && linje.startsWith('### ')) {
      throw new Error(`${filnavn}: overskriften kan ikke læses som et testtilfælde: ${linje.trim()}\n` +
        `  forventet format: ### KODE-01 — Titel (bindestreg, en-dash eller em-dash)`);
    }
    if (overskrift) {
      nu = { id: overskrift[1], titel: overskrift[2].trim(), krav: [], forudsaetning: '', start: '',
             bemaerk: '', efterlader: '', skaermbillede: '', status: 'ikke_koert', trin: [], fund: [] };
      tilfaelde.push(nu);
      continue;
    }
    if (!nu) continue;

    const meta = linje.match(/^-\s+\*\*(.+?):\*\*\s*(.*)$/);
    if (meta) {
      const nøgle = NØGLER[meta[1].trim().toLowerCase()];
      if (nøgle === 'krav') nu.krav = meta[2].split(',').map(s => s.trim()).filter(Boolean);
      else if (nøgle) nu[nøgle] = meta[2].trim();
      continue;
    }

    const række = linje.match(/^\|\s*(\d+)\s*\|(.+?)\|(.+?)\|?\s*$/);
    if (række) {
      nu.trin.push({
        nr: Number(række[1]),
        trin: række[2].trim(),
        forventet: række[3].trim(),
        observeret: '',
        status: 'ikke_koert',
        skud: [],
      });
    }
  }
  if (!tilfaelde.length) console.warn(`  advarsel: ingen testtilfælde fundet i ${filnavn}`);
  if (kode === '?') console.warn(`  advarsel: ${filnavn} mangler en "Kode:"-linje — modulet kan ikke vælges enkeltvis`);
  for (const t of tilfaelde) {
    if (!t.id.startsWith(kode + '-')) console.warn(`  advarsel: ${filnavn}: ${t.id} matcher ikke modulkoden ${kode}`);
    if (!t.trin.length) console.warn(`  advarsel: ${filnavn}: ${t.id} har ingen trin`);
    if (!t.krav.length) console.warn(`  advarsel: ${filnavn}: ${t.id} har ingen krav`);
  }
  return { navn, kode, fil: filnavn, tilfaelde };
}

const moduler = readdirSync(drejebogDir).filter(f => f.endsWith('.md')).sort()
  .map(f => parsModul(readFileSync(join(drejebogDir, f), 'utf8'), f));

const stamp = new Date().toISOString().slice(0, 16).replace('T', '-').replace(':', '');
const kørselId = process.argv[2] || stamp;
const kørselDir = join(rod, 'e2e', 'runs', kørselId);
if (!/^[A-Za-z0-9._-]+$/.test(kørselId) || kørselId.startsWith('.')) {
  console.error(`Ugyldigt kørsels-id "${kørselId}" — brug bogstaver, tal, punktum, bindestreg og understreg.`);
  process.exit(1);
}
if (existsSync(join(kørselDir, 'resultat.json')) && !process.argv.includes('--overskriv')) {
  console.error(`Kørslen ${kørselId} findes allerede med et udfyldt resultat.\n` +
    `Vælg et andet id, eller gentag med --overskriv hvis den skal kasseres.`);
  process.exit(1);
}
mkdirSync(join(kørselDir, 'skud'), { recursive: true });

const git = (cmd) => { try { return execSync(cmd, { cwd: rod }).toString().trim(); } catch { return '?'; } };

const resultat = {
  koersel: {
    id: kørselId,
    startet: new Date().toISOString(),
    afsluttet: null,
    branch: git('git rev-parse --abbrev-ref HEAD'),
    commit: git('git rev-parse --short HEAD'),
    url: process.env.E2E_URL || 'http://localhost:8444',
    udfoert_af: process.env.E2E_UDFOERT_AF || 'Claude',
    noter: '',
  },
  moduler,
};

writeFileSync(join(kørselDir, 'resultat.json'), JSON.stringify(resultat, null, 2));
const antal = moduler.reduce((n, m) => n + m.tilfaelde.length, 0);
const trin = moduler.reduce((n, m) => n + m.tilfaelde.reduce((k, t) => k + t.trin.length, 0), 0);
console.log(`Kørsel ${kørselId}: ${moduler.length} moduler, ${antal} testtilfælde, ${trin} trin`);
console.log(join(kørselDir, 'resultat.json'));
