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

### HAE-02 — Den samme formular kan kun oprette én hændelse
- **Krav:** KRAV-HAE-2
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/incidents/logs`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Udfyld en hændelse med titlen `E2E Dobbelt {KØRSEL}`, og klik Gem to gange hurtigt efter hinanden | Knapperne slås fra ved første klik, så det andet klik ikke sender noget |
| 2 | Gentag den samme indsendelse uden at hente formularen på ny — kopiér POST'en til `/incidents/log` fra netværksfanen og send den igen | Svaret peger på den hændelse den første indsendelse oprettede; der oprettes ingen ny |
| 3 | Åbn `/incidents/logs` og filtrér på `E2E Dobbelt` | Der står præcis én række |

- **Bemærk:** Formularen bærer en engangstoken, der ligger unikt på hændelsen, så det er databasen der afgør kapløbet mellem to samtidige indsendelser. Hentes formularen på ny — fx ved at åbne `/incidents/logForm` direkte og gå tilbage — får den en ny token, og en indsendelse derfra bliver med vilje en ny hændelse. I brugerfladen kommer man kun tilbage til en tom formular
- **Skærmbillede:** trin 1, trin 3

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
