# Konsekvensanalyse
Kode: DPI

### DPI-01 — Opret konsekvensanalyse
- **Krav:** KRAV-DPI-1
- **Forudsætning:** AKT-02 er gennemført
- **Start:** `/dpia`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/dpia` | Listen Konsekvensanalyser vises med knapperne Tilknyt Ekstern DPIA og Ny konsekvensanalyse |
| 2 | Klik "Ny konsekvensanalyse", vælg `E2E Aktiv {KØRSEL}` og giv den titlen `E2E DPIA {KØRSEL}` | Konsekvensanalysen oprettes og åbnes |
| 3 | Åbn `/dpia` igen | Konsekvensanalysen fremgår af listen |

- **Skærmbillede:** trin 2, trin 3
- **Efterlader:** konsekvensanalysen `E2E DPIA {KØRSEL}`

### DPI-02 — Besvar screeningen
- **Krav:** KRAV-DPI-2
- **Forudsætning:** DPI-01 er gennemført
- **Start:** konsekvensanalysen `E2E DPIA {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn screeningen | Screeningens spørgsmål vises |
| 2 | Besvar samtlige spørgsmål og gem | Svarene står efter gem, og screeningens konklusion beregnes |
| 3 | Åbn `/dpia` | Kolonnen Screening viser den farve konklusionen gav |

- **Skærmbillede:** trin 2, trin 3
