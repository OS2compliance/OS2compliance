# Risikovurdering
Kode: RIS

### RIS-01 — Opret trusselskatalog
- **Krav:** KRAV-RIS-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/catalogs`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/catalogs` | Listen vises. Den er tom i en frisk base — demodata indeholder ingen trusselskataloger |
| 2 | Klik "Ny trusselskatalog", udfyld navn `E2E Katalog {KØRSEL}`, og gem | Kataloget gemmes og fremgår af listen som synligt |
| 3 | Åbn kataloget og tilføj to trusler med kategori og beskrivelse | Begge trusler fremgår af katalogets trusselsliste |

- **Bemærk:** Uden trusler i kataloget kan RIS-02 ikke gennemføres — en risikovurdering uden trusler har intet at besvare
- **Skærmbillede:** trin 2, trin 3
- **Efterlader:** trusselskataloget `E2E Katalog {KØRSEL}` med to trusler

### RIS-02 — Opret risikovurdering
- **Krav:** KRAV-RIS-2
- **Forudsætning:** RIS-01 og AKT-02 er gennemført
- **Start:** `/risks`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/risks` og klik "Ny risikovurdering" | Oprettelsesformularen vises |
| 2 | Udfyld titel `E2E Risiko {KØRSEL}`, vælg typen Aktiv og `E2E Aktiv {KØRSEL}` som element, sæt en ejer | Felterne accepterer værdierne |
| 3 | Vælg trusselskataloget `E2E Katalog {KØRSEL}` og gem | Risikovurderingen gemmes, og brugeren lander på dens side med katalogets trusler i tabellen |
| 4 | Åbn `/risks` igen | Risikovurderingen fremgår af listen med typen Aktiv |

- **Skærmbillede:** trin 3, trin 4
- **Efterlader:** risikovurderingen `E2E Risiko {KØRSEL}`

### RIS-03 — Besvar trusler
- **Krav:** KRAV-RIS-3
- **Forudsætning:** RIS-02 er gennemført
- **Start:** risikovurderingen `E2E Risiko {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Sæt sandsynlighed og konsekvens på den første trussel | Risikotallet beregnes og farven skifter i takt med valgene |
| 2 | Sæt sandsynlighed og konsekvens på den anden trussel, og gem | Begge svar står i tabellen efter gem |
| 3 | Genindlæs siden | Svarene og de beregnede risikotal står stadig |
| 4 | Åbn risikobilledet på vurderingen | Begge trusler er placeret efter sandsynlighed og konsekvens |

- **Skærmbillede:** trin 2, trin 4

### RIS-04 — Kopiér risikovurdering
- **Krav:** KRAV-RIS-4
- **Forudsætning:** RIS-03 er gennemført
- **Start:** `/risks`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Vælg kopiér på `E2E Risiko {KØRSEL}` | En dialog beder om titel på kopien |
| 2 | Giv kopien titlen `E2E Risiko kopi {KØRSEL}` og bekræft | Kopien oprettes og åbnes |
| 3 | Se kopiens trusler | De samme to trusler med de samme svar som originalen |
| 4 | Åbn originalen igen | Originalen er uændret — kopien har ikke flyttet noget |

- **Skærmbillede:** trin 3, trin 4

### RIS-05 — Sæt revisionsinterval
- **Krav:** KRAV-RIS-5
- **Forudsætning:** RIS-03 er gennemført
- **Start:** risikovurderingen `E2E Risiko {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Sæt revisions interval", vælg en revideringsdato i kalenderen og en frekvens, og gem | Dialogen accepterer begge felter |
| 2 | Genindlæs siden og åbn dialogen igen | Dato og frekvens står stadig |

- **Bemærk:** Datofeltet skal udfyldes ved at vælge i kalenderen. Skrives datoen direkte i feltet, gemmes den ikke
- **Skærmbillede:** trin 1, trin 2
