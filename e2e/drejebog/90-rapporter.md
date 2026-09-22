# Rapporter
Kode: RAP

### RAP-01 — Word-rapporterne dannes
- **Krav:** KRAV-RAP-1
- **Forudsætning:** STD-02 er gennemført, så en standard har indhold
- **Start:** `/reports`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/reports` | Siden viser Årshjul, ISO27001 Rapport, ISO270002 Rapport, Efterlevelse af NSIS, Artikel 30 fortegnelse, Tag-rapport, Risikobillede og Kontaktpersoner |
| 2 | Dan ISO27001-rapporten | Der hentes en Word-fil uden fejl, og den kan åbnes |
| 3 | Dan ISO27002-rapporten | Der hentes en Word-fil uden fejl |
| 4 | Dan Artikel 30-fortegnelsen | Der hentes en Word-fil uden fejl |

- **Bemærk:** Rapporterne dannes serverside. Fejler en af dem, står årsagen i `e2e/runs/app.log`, ikke i brugerfladen
- **Skærmbillede:** trin 1

### RAP-02 — Excel-udtræk
- **Krav:** KRAV-RAP-2
- **Forudsætning:** AKT-02 er gennemført
- **Start:** `/assets`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Gem som Excel" på aktivlisten | Der hentes en excel-fil, og kørslens aktiv står i den |
| 2 | Gør det samme på `/registers` og `/tasks` | Begge filer hentes uden fejl |

- **Skærmbillede:** trin 1

### RAP-03 — Årshjulet
- **Krav:** KRAV-RAP-3
- **Forudsætning:** OPG-02 er gennemført
- **Start:** `/reports`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn fanen Årshjul i Opgavecenter | Årets opgaver vises fordelt på måneder |
| 2 | Find en opgave der er markeret "Skal indgå i årshjul" | Opgaven står i måneden for dens frist. Opgaver uden markeringen vises ikke — feltet er fra som standard |

- **Skærmbillede:** trin 1, trin 2
