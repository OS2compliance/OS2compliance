# E2E-drejebog

Manuel gennemgang af løsningen i en rigtig browser, kørt ved hvert release.
Samme drejebog kan køres af et menneske og af Claude. Resultatet er en
HTML-rapport med skærmbilleder og fund.

```
e2e/
  krav.md         Kravene løsningen efterprøves mod
  drejebog/       Testtilfældene, ét modul pr. fil
  seed/           Faste e2e-brugere
  testfiler/      Filer der uploades undervejs
  bin/            Miljø, resultatskema og rapportgenerator
  claude-kommando.md  Indholdet af /e2e-kommandoen
  runs/           Kørsler med resultat.json, skærmbilleder og rapport (ikke i git)
```

## Rejs miljøet

```bash
e2e/bin/stop-miljoe.sh             # kører der allerede noget på 8444, så stop det først
e2e/bin/start-miljoe.sh            # genbruger databasen hvis den kører
e2e/bin/start-miljoe.sh --nulstil  # helt frisk base — brug denne før en releasekørsel
```

Miljøet er isoleret fra udvikling: egen MariaDB på port **3307** i databasen
`os2compliance_e2e`, appen på **http://localhost:8444** uden TLS.
Udviklingsdatabasen på 3306 røres aldrig.

Databasen kører `mariadb:10.6.14` uden ekstra kollationsflag, præcis som
`docker-compose.yml`. Sættes serverens kollation til `utf8mb4_danish_ci`,
vælter Flyway i `V1_124`: tabeller uden egen `collate` arver serverens, og
migreringen sammenligner en af dem med en `CAST`.

Kør browseren i **2560x1440**. Løsningen er tegnet til brede skærme, og flere
lister kræver pladsen. En kørsel i et smalt vindue giver fund der ikke er fejl.

| Bruger | Kodeord | Roller |
|--------|---------|--------|
| `e2e-admin` | `E2E-test1234` | administrator, superbruger, adgang |
| `e2e-super` | `E2E-test1234` | superbruger, adgang |
| `e2e-bruger` | `E2E-test1234` | adgang |

Stop igen med `e2e/bin/stop-miljoe.sh` (`--slet-db` fjerner også databasen).

### Login uden IdP

OS2compliance logger ind med SAML, og en IdP kan ikke rejses lokalt. Derfor
findes `LocalLoginController` i `src/test/java/dk/digitalidentity/e2e/`: den
tager imod brugernavn og kodeord på `/e2e/login` og bygger den samme session
som et SAML-login ville — rollerne fra databasen sendes gennem
`RolePostProcessor`, så rettighederne foldes ud af produktets egen kode.

Controlleren ligger under `src/test` og kræver profilen `locallogin`. Den kan
ikke havne i en release; `start-miljoe.sh` lægger `target/test-classes` på
klassestien og åbner `/e2e/**` i SAML-modulets liste over usikrede sider.

Produktets eget "Log ud" i sidemenuen virker som normalt. Efter log ud lander
man på `/`, som under `locallogin` rendrer en gammel loginformular fra
`index.html`. Den virker ikke og er ikke et fund — i drift er `samllogin`
aktiv, og blokken vises ikke.

Basen er ikke tom efter `--nulstil`. Udviklingstilstanden lægger selv demodata
ind: fire aktiver, tre leverandører, cirka halvtreds behandlingsaktiviteter fra
KL's katalog, fire standarder, tre opgaver, to dokumenter og en håndfuld
afdelinger. `seed/` indeholder kun brugerne. Flere tilfælde bygger på de
demodata, så "listen vises" betyder ikke en tom liste.

Der følger **ingen** trusselskataloger med. `RIS-01` opretter derfor sit eget,
og det skal køres før `RIS-02`.

Filupload går til S3. Uden legitimationsoplysninger fejler dokumentmodulet, så
`DOK-02` kan kun køres hvis miljøet har adgang til en S3-bøtte — enten den
rigtige eller en lokal MinIO.

## Kør drejebogen

**1. Opret kørslen.** Skemaet dannes ud fra drejebogen, så det altid matcher:

```bash
node e2e/bin/ny-koersel.mjs                 # id = dato og klokkeslæt
node e2e/bin/ny-koersel.mjs release-2026-09 # eller et navn du selv vælger
```

Det lægger `e2e/runs/<id>/resultat.json` med alle tilfælde sat til `ikke_koert`.

**2. Gennemgå tilfældene i rækkefølge.** Modulerne er nummereret, og senere
tilfælde bygger på data fra tidligere — `AKT-03` forudsætter at `AKT-02` har
oprettet aktivet. Kører du kun et udsnit, så tag hele moduler.

Hvor drejebogen skriver `{KØRSEL}`, indsættes kørslens id, så to kørsler ikke
kolliderer i den samme base.

**3. Vent på tabellerne.** Listerne hentes asynkront efter sidens indlæsning. Et
skærmbillede taget lige efter navigation viser en tom tabel, og det ligner et
fund. Vent på at rækkerne står der før du vurderer trinnet.

**4. Kig i serverloggen ved et fejlet trin.** `e2e/runs/app.log` er ofte eneste
sted fejlen kan ses — brugerfladen viser i flere tilfælde enten ingenting eller
bare "Der opstod en teknisk fejl". Stakspor og endpoint hører med i `detalje`.

**5. Notér undervejs.** For hvert trin udfyldes `observeret` og `status` i
`resultat.json`. Skærmbilleder lægges i `e2e/runs/<id>/skud/` og nævnes ved
filnavn i trinnets `skud`-liste.

Gem skærmbilleder som `.jpg`. De indlejres i rapporten, og en fuld kørsel i PNG
giver en fil på 40+ MB. Brug Playwright-værktøjet — det skriver filen til disk,
og det er filen rapporten indlejrer.

Status på trin og tilfælde: `bestaaet`, `fejlet`, `blokeret`,
`sprunget_over`, `ikke_koert`.

Et fund hører på testtilfældet:

```json
"fund": [{
  "alvor": "hoej",
  "resume": "Kopiknappen i opgavelisten gør ingenting",
  "detalje": "Klik på kopiér giver ingen dialog og ingen netværkskald. Konsollen viser en TypeError.",
  "trin": 1,
  "skud": ["opg-06-1-kopiknap.jpg"]
}]
```

`alvor` er `kritisk`, `hoej`, `middel` eller `lav`. Skriv `resume` som det
observerede symptom — ikke som en formodning om årsagen.

**6. Dan rapporten.**

```bash
node e2e/bin/rapport.mjs <kørsels-id>
```

Rapporten lander i kørselsmappen og som `e2e-rapport.html` i projektets rod.
Skærmbillederne indlejres, så filen kan sendes videre alene.

## Når Claude kører den

Bed om det i almindelig tekst, eller med `/e2e`. Kommandoen ligger i
`e2e/claude-kommando.md`, fordi `.claude/` ikke er i git — link den ind første
gang: `ln -s ../../e2e/claude-kommando.md .claude/commands/e2e.md`.

Claude gør så følgende:

1. Rejser miljøet med `--nulstil` og opretter kørslen.
2. Åbner browseren mod `http://localhost:8444/e2e/login` og gennemgår
   tilfældene i nummerorden — ét trin ad gangen, med et skærmbillede hvor
   drejebogen beder om det, og altid ved et fejlet trin.
3. Skriver `observeret` som det der faktisk skete, også når det svarer til det
   forventede. Et trin er kun `bestaaet` hvis det forventede rent faktisk blev
   set — ikke hvis siden bare loadede.
4. Tjekker browserkonsollen for fejl på hver side og noterer dem som fund, også
   når trinnet i øvrigt lykkedes.
5. Fortsætter efter et fejlet trin hvis resten af tilfældet giver mening, og
   sætter ellers de efterfølgende tilfælde til `blokeret` med en begrundelse.
6. Danner rapporten og fortæller hvad der fejlede.

Claude retter ikke fejl undervejs. Fundene skal stå i rapporten, så de kan
prioriteres samlet.

## Vedligeholdelse

Drejebogen er kilden — resultatskemaet dannes altid ud fra den. Ændrer en
skærm sig, rettes trinnet i `drejebog/`, ikke i en kørsel.

Nye krav føjes til `krav.md` og får mindst ét testtilfælde. Hver browserfejl
fundet i produktion bør blive til et testtilfælde her, så den ikke kan komme
tilbage ubemærket.

Trinnene er skrevet på hensigtsniveau — "opret en opgave med titel, ansvarlig
og frist" — ikke som CSS-selektorer. Det er med vilje: et menneske skal kunne
følge dem, og de skal overleve, at en knap flytter sig.
