# Leverandører
Kode: LEV

### LEV-01 — Leverandørlisten åbner
- **Krav:** KRAV-LEV-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/suppliers`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/suppliers` | Listen vises med kolonnerne Navn, Opdateret, Status og Handlinger; demodata giver tre leverandører |

- **Skærmbillede:** trin 1

### LEV-02 — Opret leverandør
- **Krav:** KRAV-LEV-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/suppliers`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Ny leverandør" | Oprettelsesformularen vises |
| 2 | Udfyld navn `E2E Leverandør {KØRSEL}` og et cvr-nummer, og gem | Leverandøren gemmes, og brugeren lander på dens side |
| 3 | Åbn `/suppliers` igen | Leverandøren fremgår af listen |

- **Skærmbillede:** trin 2, trin 3
- **Efterlader:** leverandøren `E2E Leverandør {KØRSEL}`

### LEV-03 — Ret leverandørens stamdata
- **Krav:** KRAV-LEV-2
- **Forudsætning:** LEV-02 er gennemført
- **Start:** leverandøren `E2E Leverandør {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Ret kontaktoplysningerne og gem | De nye værdier står på siden efter gem |
| 2 | Genindlæs siden | De nye værdier står stadig |
| 3 | Åbn `/suppliers` | Kolonnen Opdateret viser dagens dato for leverandøren |

- **Skærmbillede:** trin 1, trin 3
