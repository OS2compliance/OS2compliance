This is just assorted scripts for helping with data import.

## KL's behandlingsaktiviteter

KL opdaterer arket over mappede behandlingsaktiviteter hvert kvartal. Arket er kilden,
og pakkerne i `src/main/resources/data/registers/` genereres ud fra det - de følger med
releasen, så en ny tilslutning får den aktuelle version ved første opstart
(`DataBootstrap.addRegistersV0`).

Læg arket i projektets rod (`KL-mappede behandlingsaktiviteter ... .xlsx`) og kør:

```sh
ruby activity_importer.rb --rapport                     # hvad ændrer sig? intet skrives
ruby activity_importer.rb --ud ../src/main/resources/data/registers
ruby kle-activity-enricher.rb --tørkørsel               # hvilke KLE-ændringer?
ruby kle-activity-enricher.rb
```

`activity_importer.rb` laver titel, beskrivelse og GDPR-hjemmel;
`kle-activity-enricher.rb` lægger KLE oven på. Begge læser `.xlsx`-filen direkte via
`xlsx.rb` (stdlib + `unzip`, ingen gems) - der skal ikke længere eksporteres CSV i
hånden. `kle-mapping.csv` er den gamle, manuelt eksporterede indgang og bruges ikke af
scripterne mere.

Importeren genbruger filnavnet på en pakke med samme titel, så en opdatering giver en
læsbar diff i stedet for at renummerere alle filer. Nye aktiviteter får det næste
ledige nummer.

Husk ved en opdatering:

- **Rul ikke en opdatering ud på eksisterende kunder.** Pakkerne følger releasen og
  rammer nye tilslutninger via `addRegistersV0`; eksisterende installationer får dem
  ikke, og det er indtil videre med vilje. Et `seedVxx`-trin, der kalder
  `importRegister`, opretter dubletter - det skete med v1.8 (11-08-2026, 33 dubletter
  hos én kommune, genoprettet manuelt fra backup). To ting skal løses først:
  - **Titlen er ikke en stabil nøgle.** `findByNameAndDeletedFalse` matcher på `name`,
    men kundens titler er drevet fra pakkernes over flere kvartaler (`10a.`, dobbelte
    mellemrum, `\r\n` midt i titlen), og kunden kan selv rette dem i UI'et. Det er
    ikke nok at mappe forrige kvartals titler til dette - hver installation har sin
    egen årgang. Der mangler en identitet pr. aktivitet; `packageName` står på
    `kl_article30` for dem alle og duer ikke.
  - **Soft-deletede fortegnelser er usynlige for opslaget.** Har kunden slettet en
    KL-fortegnelse, ser `importRegister` den ikke og genopretter den.
- **`updateRegisterGdprChoices` + `enrichWithKLE` overskriver kundens eget arbejde.**
  De skriver KL's hjemmel og KLE oven i det der står, uanset om kunden har rettet i
  det. `updateRegisterGdprChoices` har i øvrigt ingen kaldere i dag.
- **KLE-koder appen ikke kender bliver droppet lydløst.** Enricheren lister dem;
  hvis de er gyldige i nyeste KLE, skal `data/kle-emneplan.xml` opdateres først.
- **Konsekvensvurderingen (kolonne F-I) importeres ikke.** Feltet findes på entiteten
  (`ConsequenceAssessment`), men hverken `RegisterDTO` eller `RegisterImporter` rører
  det, så det kræver kode. Arket har desuden kun ét tal uden perspektiv, hvor appen har
  fortrolighed/integritet/tilgængelighed × den registrerede/organisationen/samfundet.
