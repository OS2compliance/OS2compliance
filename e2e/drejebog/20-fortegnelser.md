# Fortegnelser
Kode: FOR

### FOR-01 — Fortegnelsen åbner
- **Krav:** KRAV-FOR-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/registers`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/registers` | Listen vises med kolonnerne Titel, Risiko vurdering, Status, Aktiver og Handlinger |
| 2 | Notér antallet af rækker | Demodata giver omkring halvtreds behandlingsaktiviteter fra KL's katalog |

- **Skærmbillede:** trin 1

### FOR-02 — Opret behandlingsaktivitet
- **Krav:** KRAV-FOR-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/registers`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Ny behandlingsaktivitet" | Oprettelsesformularen vises |
| 2 | Udfyld titel `E2E Fortegnelse {KØRSEL}` og en ansvarlig, og gem | Behandlingsaktiviteten gemmes, og brugeren lander på dens side |
| 3 | Åbn `/registers` igen og filtrér på `E2E Fortegnelse` | Den nye behandlingsaktivitet fremgår af listen |

- **Skærmbillede:** trin 2, trin 3
- **Efterlader:** behandlingsaktiviteten `E2E Fortegnelse {KØRSEL}`

### FOR-03 — Formål og behandlingsgrundlag
- **Krav:** KRAV-FOR-2
- **Forudsætning:** FOR-02 er gennemført
- **Start:** behandlingsaktiviteten `E2E Fortegnelse {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Udfyld formål og gem | Teksten står på siden efter gem |
| 2 | Udfyld behandlingsgrundlag og de registreredes kategorier, og gem | Valgene står på siden efter gem |
| 3 | Genindlæs siden | Alt det indtastede står stadig |

- **Skærmbillede:** trin 2, trin 3

### FOR-04 — Knyt behandlingsaktiviteten til et aktiv
- **Krav:** KRAV-FOR-3
- **Forudsætning:** FOR-02 og AKT-02 er gennemført
- **Start:** behandlingsaktiviteten `E2E Fortegnelse {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Tilføj `E2E Aktiv {KØRSEL}` som tilknyttet aktiv | Aktivet fremgår af behandlingsaktivitetens liste |
| 2 | Åbn `/registers` | Kolonnen Aktiver tæller nu 1 for kørslens behandlingsaktivitet |
| 3 | Åbn aktivet | Behandlingsaktiviteten fremgår af aktivets relationer |

- **Skærmbillede:** trin 2, trin 3
