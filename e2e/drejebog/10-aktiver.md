# Aktiver
Kode: AKT

### AKT-01 — Aktivlisten åbner
- **Krav:** KRAV-AKT-3
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/assets`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/assets` | Listen vises med kolonnerne Navn, Type, Status og Handlinger og knapperne Gem som Excel, Tilføj aktiv og Ny opgave |
| 2 | Notér antallet af rækker | Antallet kan aflæses; demodata giver fire aktiver |

- **Skærmbillede:** trin 1

### AKT-02 — Opret aktiv
- **Krav:** KRAV-AKT-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/assets`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Tilføj aktiv" | Formularen "Nyt aktiv" vises med Navn, Leverandør, Type og Systemejere |
| 2 | Udfyld navn `E2E Aktiv {KØRSEL}`, vælg en leverandør, typen It-system og `E2E Administrator` som systemejer | Felterne accepterer værdierne uden valideringsfejl |
| 3 | Gem | Aktivet gemmes, og brugeren lander på aktivets side med det indtastede navn i overskriften |
| 4 | Åbn `/assets` igen | Det nye aktiv fremgår af listen |

- **Bemærk:** Alle fire felter er påkrævede — gemmes der uden, vises "Name is required", "Supplier is required" og "User is required"
- **Skærmbillede:** trin 2, trin 3, trin 4
- **Efterlader:** aktivet `E2E Aktiv {KØRSEL}`, som senere tilfælde bruger

### AKT-03 — Ret aktivets stamdata
- **Krav:** KRAV-AKT-2
- **Forudsætning:** AKT-02 er gennemført
- **Start:** aktivet `E2E Aktiv {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn redigering af aktivets stamdata | Formularen er forudfyldt med de gemte værdier |
| 2 | Ret beskrivelsen og gem | Siden genindlæses med den nye tekst |
| 3 | Genindlæs siden med F5 | Den nye tekst står stadig — ændringen er gemt i basen, ikke kun i visningen |

- **Skærmbillede:** trin 2, trin 3

### AKT-04 — Filtrering i aktivlisten
- **Krav:** KRAV-AKT-3
- **Forudsætning:** AKT-02 er gennemført
- **Start:** `/assets`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Skriv `E2E Aktiv` i kolonnefilteret Navn | Kun matchende rækker vises, og kørslens aktiv er iblandt dem |
| 2 | Filtrér på en tekst der ikke findes, fx `zzzfindesikke` | Listen viser "Ingen data fundet", ikke en fejl |
| 3 | Ryd filterfeltet | Den fulde liste vises igen |

- **Skærmbillede:** trin 1, trin 2

### AKT-05 — Relatér aktivet til et andet element
- **Krav:** KRAV-AKT-4
- **Forudsætning:** AKT-02 er gennemført
- **Start:** aktivet `E2E Aktiv {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Find afsnittet med relaterede elementer på aktivets side | Afsnittet vises med en knap til at tilføje en relation |
| 2 | Tilføj et andet eksisterende aktiv som relation | Relationen fremgår af aktivets liste over relaterede elementer |
| 3 | Åbn det relaterede aktiv | Kørslens aktiv fremgår af dets liste — relationen går begge veje |

- **Skærmbillede:** trin 2, trin 3

### AKT-06 — Leverandørtilsyn på aktivet
- **Krav:** KRAV-AKT-5
- **Forudsætning:** AKT-02 er gennemført
- **Start:** aktivet `E2E Aktiv {KØRSEL}`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn tilsynsdelen på aktivet | Afsnittet vises med mulighed for at oprette et tilsyn |
| 2 | Opret et tilsyn med dato, ansvarlig og konklusion, og gem | Tilsynet gemmes og fremgår af aktivets tilsynsliste |
| 3 | Genindlæs siden | Tilsynet står stadig med den valgte konklusion |

- **Bemærk:** Et tilsyn lægger en opgave i opgavecentret. Den optræder senere i OPG-01's liste uden at være en fejl
- **Skærmbillede:** trin 2, trin 3
