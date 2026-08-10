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

- **Titlen er nøglen.** `RegisterImporter` slår op på `name`, så en ændret titel
  opretter en *ny* fortegnelse ved siden af den gamle i stedet for at opdatere.
  Rapporten lister titler der kun findes i pakkerne - dem skal der omdøbes i et
  `seedVxx`-trin, som `DataBootstrap.seedV28` gjorde det.
- **Eksisterende kunder får ikke `addRegistersV0` igen.** De skal have et nyt
  `seedVxx`-trin, der kalder `importRegister` (opretter de nye), og
  `updateRegisterGdprChoices` + `enrichWithKLE` (opdaterer de eksisterende).
  Bemærk at det overskriver eventuelle rettelser kunden selv har lavet.
- **KLE-koder appen ikke kender bliver droppet lydløst.** Enricheren lister dem;
  hvis de er gyldige i nyeste KLE, skal `data/kle-emneplan.xml` opdateres først.
- **Konsekvensvurderingen (kolonne F-I) importeres ikke.** Feltet findes på entiteten
  (`ConsequenceAssessment`), men hverken `RegisterDTO` eller `RegisterImporter` rører
  det, så det kræver kode. Arket har desuden kun ét tal uden perspektiv, hvor appen har
  fortrolighed/integritet/tilgængelighed × den registrerede/organisationen/samfundet.
