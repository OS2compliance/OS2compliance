# Hændelser
Kode: HAE

### HAE-01 — Registrér hændelse
- **Krav:** KRAV-HAE-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/incidents/logs`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/incidents/logs` | Hændelsesloggen vises med datofilter og knapperne Ny hændelse, Print rapport og Hent excel fil |
| 2 | Klik "Ny hændelse" | Formularen vises med titel og hændelsesspørgsmålene, og knapperne Annuller, Gem som kladde og Gem |
| 3 | Udfyld titel `E2E Hændelse {KØRSEL}`, besvar spørgsmålene og gem | Hændelsen gemmes |
| 4 | Åbn `/incidents/logs` | Hændelsen fremgår af loggen og kan åbnes igen med de indtastede svar |

- **Skærmbillede:** trin 2, trin 3, trin 4
- **Efterlader:** hændelsen `E2E Hændelse {KØRSEL}`

### HAE-02 — Gentagen indsendelse giver ikke en kopi
- **Krav:** KRAV-HAE-2
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/incidents/logForm`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Udfyld en hændelse med titlen `E2E Dobbelt {KØRSEL}` og gem | Hændelsen gemmes |
| 2 | Gå tilbage i browseren til formularen og gem igen uden at ændre noget | Der oprettes ikke en ny hændelse — indsendelsen rammer den samme hændelse |
| 3 | Åbn `/incidents/logs` og filtrér på `E2E Dobbelt` | Der står præcis én række |

- **Bemærk:** Dobbeltindsendelse har tidligere lagt en kopi i loggen. Tilfældet findes for at fange det igen
- **Skærmbillede:** trin 3

### HAE-03 — Filtrering og excel-udtræk
- **Krav:** KRAV-HAE-3
- **Forudsætning:** HAE-01 er gennemført
- **Start:** `/incidents/logs`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Sæt datofilteret Oprettelsesdato til i dag og søg | Kørslens hændelser vises |
| 2 | Sæt Fra-datoen til en dato efter i dag og søg | Listen er tom og viser en pæn tom-tilstand, ikke en fejl |
| 3 | Ryd filtrene og klik "Hent excel fil" | Der hentes en excel-fil uden fejl, og den kan åbnes |

- **Skærmbillede:** trin 1, trin 2
