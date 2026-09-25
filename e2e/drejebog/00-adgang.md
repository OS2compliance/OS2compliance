# Adgang og navigation
Kode: ADG

### ADG-01 — Login og landing på dashboardet
- **Krav:** KRAV-ADG-1
- **Forudsætning:** Ikke logget ind
- **Start:** `/e2e/login`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Åbn `/e2e/login` | Harniskets loginside vises med felter til brugernavn og kodeord |
| 2 | Indtast `e2e-admin` / `E2E-test1234` og log ind | Brugeren sendes til `/dashboard`, og overskriften er "Hej E2E Administrator" |
| 3 | Se rollelinjen under overskriften | Der står Administrator, Superbruger, Bruger — rollerne fra databasen er foldet ud |

- **Bemærk:** `/e2e/login` er e2e-harniskets eget login. Produktet logger ind med SAML, som ikke kan rejses lokalt; rollerne foldes ud af den samme kode som ved SAML-login
- **Skærmbillede:** trin 1, trin 2

### ADG-02 — Beskyttet side kræver login
- **Krav:** KRAV-ADG-2
- **Forudsætning:** Ikke logget ind — log ud først, ADG-01 efterlader en session
- **Start:** `/assets`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Log ud, og åbn så `/assets` direkte | Brugeren sendes til login, ikke til aktivlisten og ikke til en fejlside |
| 2 | Åbn `/admin/auditlog` direkte | Brugeren sendes til login |

- **Skærmbillede:** trin 1

### ADG-03 — Log ud afslutter sessionen
- **Krav:** KRAV-ADG-3
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/dashboard`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik "Log ud" nederst i sidemenuen | Brugeren forlader dashboardet og har ikke længere en session |
| 2 | Naviger tilbage i browseren | Der vises ikke beskyttet indhold; brugeren er stadig logget ud |

- **Skærmbillede:** trin 1

### ADG-04 — Almindelig bruger holdes ude af administration
- **Krav:** KRAV-ADG-4
- **Forudsætning:** Logget ind som `e2e-bruger`
- **Start:** `/dashboard`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Se hovedmenuen | Administration vises ikke som menupunkt |
| 2 | Åbn `/admin/auditlog` direkte | Adgang nægtes med 403 — auditloggen vises ikke |
| 3 | Åbn `/settings/form` direkte | Adgang nægtes med 403 |

- **Bemærk:** `e2e-bruger` har kun adgangsrollen. Dashboardet og aktivlisten skal stadig kunne åbnes
- **Skærmbillede:** trin 1, trin 2

### ADG-05 — Alle menupunkter loader
- **Krav:** KRAV-NAV-1
- **Forudsætning:** Logget ind som `e2e-admin`
- **Start:** `/dashboard`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Klik hvert punkt i sidemenuen ét ad gangen. Fire af punkterne folder ud i stedet for at navigere — åbn også hver underside under dem | Hver side loader med indhold — ingen fejlside, ingen tom side, ingen fejl i browserkonsollen |
| 2 | Notér for hvert punkt hvilken URL det førte til | Alle URL'er svarer med indhold |

- **Bemærk:** Sidemenuen har elleve punkter, hvoraf Aktiver, Risikostyring, Hændelser og Administration kun folder ud. I alt cirka 25 sider
- **Skærmbillede:** ét pr. side der faktisk navigeres til — ikke af udfolderne

### ADG-06 — Dashboardet viser brugerens eget indhold
- **Krav:** KRAV-NAV-2
- **Forudsætning:** Logget ind som `e2e-admin`, og kørslens opgave, aktiv, fortegnelse og dokument er oprettet
- **Bemærk:** Fanerne viser kun det brugeren selv er ansvarlig for. Køres tilfældet før de øvrige moduler, er alle fire tomme uden at der er noget galt — tag det til sidst
- **Start:** `/dashboard`

| # | Trin | Forventet |
|---|------|-----------|
| 1 | Se dashboardet | De fire faner Mine Opgaver, Mine Aktiver, Mine Fortegnelser og Mine Dokumenter vises |
| 2 | Åbn hver fane | Hver tabel viser kørslens egne rækker — ingen fejl, ingen uendelig indlæsning |

- **Skærmbillede:** trin 1, trin 2
