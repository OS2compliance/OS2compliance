# Opgavecenter
Kode: OPG

### OPG-01 — Opgavelisten åbner
- **Krav:** KRAV-OPG-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/tasks`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/tasks` | Listen vises med fanerne Liste og Årshjul og kolonnerne Opgavenavn, Opgave type, Ansvarlig, Startdato, Slutdato, Resultat og Status |
| 2 | Notér antallet af rækker | Demodata giver tre opgaver; tilsyn oprettet i AKT-06 kan ligge iblandt dem |

- **Skærmbillede:** trin 1

### OPG-02 — Opret engangsopgave
- **Krav:** KRAV-OPG-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/tasks`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Ny opgave" | Oprettelsesformularen vises |
| 2 | Udfyld navn `E2E Opgave {KØRSEL}`, `E2E Administrator` som ansvarlig, en frist og gentagelsen Ingen | Felterne accepterer værdierne |
| 3 | Gem | Opgaven gemmes og fremgår af listen med status Ikke udført |

- **Skærmbillede:** trin 2, trin 3
- **Efterlader:** opgaven `E2E Opgave {KØRSEL}`

### OPG-03 — Opret gentagen opgave
- **Krav:** KRAV-OPG-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/tasks`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Opret opgaven `E2E Gentagen {KØRSEL}` med `E2E Administrator` som ansvarlig, en frist og en gentagelse (fx årligt) | Opgaven gemmes |
| 2 | Åbn opgaven | Gentagelsen fremgår af opgavens stamdata |

- **Skærmbillede:** trin 2
- **Efterlader:** opgaven `E2E Gentagen {KØRSEL}`

### OPG-04 — Udfør opgaverne
- **Krav:** KRAV-OPG-2
- **Forudsætning:** OPG-02 og OPG-03 er gennemført
- **Start:** `/tasks`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Udfør `E2E Opgave {KØRSEL}` med en bemærkning | Opgaven lukkes og står ikke længere som åben på listen |
| 2 | Udfør `E2E Gentagen {KØRSEL}` med en bemærkning | Opgaven bliver stående på listen med en ny frist beregnet ud fra gentagelsen |
| 3 | Genindlæs listen | Begge opgaver står som i trin 1 og 2 |

- **Skærmbillede:** trin 1, trin 2

### OPG-05 — Opgavens historik
- **Krav:** KRAV-OPG-4
- **Forudsætning:** OPG-04 er gennemført
- **Start:** opgaven `E2E Gentagen {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn opgavens historik | Udførelsen fra OPG-04 fremgår med dato og bemærkning |

- **Skærmbillede:** trin 1

### OPG-06 — Kopiér opgave
- **Krav:** KRAV-OPG-3
- **Forudsætning:** OPG-03 er gennemført
- **Start:** `/tasks`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Vælg kopiér på `E2E Gentagen {KØRSEL}` | Kopien åbnes til redigering med stamdata fra originalen |
| 2 | Giv kopien navnet `E2E Opgave kopi {KØRSEL}` og gem | Kopien fremgår af listen ved siden af originalen |
| 3 | Åbn originalen | Originalen er uændret |

- **Skærmbillede:** trin 2, trin 3
