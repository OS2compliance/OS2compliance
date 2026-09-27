# Dokumenter
Kode: DOK

### DOK-01 — Opret dokument med link
- **Krav:** KRAV-DOK-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/documents`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/documents` | Listen vises med kolonnerne Titel, Status og Handlinger; demodata giver to dokumenter |
| 2 | Klik "Nyt dokument", udfyld titel `E2E Dokument {KØRSEL}`, en ansvarlig og et link, og gem | Dokumentet gemmes |
| 3 | Åbn `/documents` igen | Dokumentet fremgår af listen og kan åbnes |

- **Skærmbillede:** trin 2, trin 3
- **Efterlader:** dokumentet `E2E Dokument {KØRSEL}`

### DOK-02 — Opret dokument med fil
- **Krav:** KRAV-DOK-2
- **Forudsætning:** Miljøet har adgang til en S3-bøtte
- **Start:** `/documents`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Nyt dokument", udfyld titel `E2E Fil {KØRSEL}` og vedhæft `e2e/testfiler/e2e-dokument.txt` | Filen accepteres |
| 2 | Gem | Dokumentet gemmes og fremgår af listen |
| 3 | Hent filen igen fra dokumentet | Filen hentes med det samme indhold som den uploadede |

- **Bemærk:** Filer lægges i S3. Uden legitimationsoplysninger fejler trin 2, og tilfældet sættes til `blokeret` — ikke `fejlet`
- **Skærmbillede:** trin 2, trin 3
