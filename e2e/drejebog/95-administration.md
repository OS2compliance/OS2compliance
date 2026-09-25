# Administration
Kode: ADM

### ADM-01 — Opret og brug et tag
- **Krav:** KRAV-ADM-1
- **Forudsætning:** Logget ind som `e2e-admin`, og AKT-02 er gennemført
- **Start:** `/admin/tags`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/admin/tags` | Listen over tags vises |
| 2 | Opret tagget `E2E Tag {KØRSEL}` | Tagget fremgår af listen |
| 3 | Sæt tagget på `E2E Aktiv {KØRSEL}` og gem | Tagget vises på aktivet |

- **Skærmbillede:** trin 2, trin 3
- **Efterlader:** tagget `E2E Tag {KØRSEL}`

### ADM-02 — Udvid en valgliste
- **Krav:** KRAV-ADM-2
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/admin/choicelists`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/admin/choicelists` | Listen over valglister vises |
| 2 | Åbn en valgliste og tilføj værdien `E2E Valg {KØRSEL}` | Værdien fremgår af valglisten |
| 3 | Åbn den formular hvor valglisten bruges | Den nye værdi kan vælges |

- **Skærmbillede:** trin 2, trin 3

### ADM-03 — Auditloggen viser ændringerne
- **Krav:** KRAV-ADM-3
- **Forudsætning:** Modulerne før dette er gennemført
- **Start:** `/admin/auditlog`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/admin/auditlog` | Loggen vises med de seneste hændelser |
| 2 | Find kørslens egne ændringer | `E2E Administrator` står som bruger på de ændringer kørslen har lavet |

- **Skærmbillede:** trin 1, trin 2

### ADM-04 — Organisationens indstillinger
- **Krav:** KRAV-ADM-2
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/dashboard`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn indstillingerne fra menuen — ikke ved at skrive `/settings/form` i adressefeltet | Indstillingerne vises |
| 2 | Ret en indstilling og gem | Ændringen slår igennem i brugerfladen |

- **Bemærk:** Siden læser hvor man kom fra. Åbnes `/settings/form` direkte uden referer, fejler den — notér det som fund, hvis det stadig sker
- **Skærmbillede:** trin 1, trin 2
