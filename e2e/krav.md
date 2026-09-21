# Krav dækket af drejebogen

Hvert krav er en påstand om hvad løsningen skal kunne. Testtilfældene i
`drejebog/` refererer til kravene, og rapporten viser hvilke krav der er
efterprøvet i den enkelte kørsel.

Kravene er bevidst formuleret på brugerniveau. Et krav er opfyldt når en bruger
kan gøre det beskrevne i en browser — ikke når en enhedstest er grøn.

| Krav | Beskrivelse | Modul |
|------|-------------|-------|
| KRAV-ADG-1 | En bruger med adgangsrolle kan logge ind og lander på dashboardet | Adgang |
| KRAV-ADG-2 | En ikke-logget-ind bruger får ikke adgang til en beskyttet side | Adgang |
| KRAV-ADG-3 | Log ud afslutter sessionen, og tilbage-knappen giver ikke adgang til beskyttet indhold | Adgang |
| KRAV-ADG-4 | En bruger uden superbruger- eller administratorrolle kan hverken se eller åbne administrationssiderne | Adgang |
| KRAV-NAV-1 | Alle menupunkter i hovednavigationen fører til en side der loader uden fejl | Adgang |
| KRAV-NAV-2 | Forsiden viser dashboardet med brugerens egne opgaver, aktiver, fortegnelser og dokumenter | Adgang |
| KRAV-AKT-1 | Et aktiv kan oprettes med navn, leverandør, type og systemejer og fremgår derefter af listen | Aktiver |
| KRAV-AKT-2 | Et aktivs stamdata kan rettes, og ændringen består efter genindlæsning | Aktiver |
| KRAV-AKT-3 | Aktivlisten kan filtreres, og filtret afspejles i resultatet | Aktiver |
| KRAV-AKT-4 | Et aktiv kan relateres til et andet element, og relationen vises begge veje | Aktiver |
| KRAV-AKT-5 | Et aktivs leverandørtilsyn kan registreres og gemmes | Aktiver |
| KRAV-FOR-1 | En behandlingsaktivitet kan oprettes og fremgår af fortegnelsen | Fortegnelser |
| KRAV-FOR-2 | Formål, behandlingsgrundlag og konsekvensvurdering kan udfyldes og gemmes | Fortegnelser |
| KRAV-FOR-3 | En behandlingsaktivitet kan knyttes til et aktiv | Fortegnelser |
| KRAV-RIS-1 | Et trusselskatalog kan oprettes med trusler og vælges i en risikovurdering | Risiko |
| KRAV-RIS-2 | En risikovurdering kan oprettes med titel, ejer og trusselskatalog | Risiko |
| KRAV-RIS-3 | Trusler kan besvares med sandsynlighed og konsekvens, og risikotallet beregnes | Risiko |
| KRAV-RIS-4 | En risikovurdering kan kopieres, og kopien indeholder de samme trusler og svar | Risiko |
| KRAV-RIS-5 | En risikovurdering kan revideres, og revisionen bevarer historikken | Risiko |
| KRAV-DPI-1 | En konsekvensanalyse kan oprettes på et aktiv | DPIA |
| KRAV-DPI-2 | Screeningen kan besvares, og resultatet slår igennem på oversigten | DPIA |
| KRAV-STD-1 | En standards afsnit vises i strukturen og kan åbnes | Standarder |
| KRAV-STD-2 | Et afsnits modenhed og status kan sættes og gemmes | Standarder |
| KRAV-LEV-1 | En leverandør kan oprettes og fremgår af listen | Leverandører |
| KRAV-LEV-2 | En leverandørs stamdata kan rettes og gemmes | Leverandører |
| KRAV-OPG-1 | En opgave kan oprettes med titel, ansvarlig, frist og gentagelse | Opgaver |
| KRAV-OPG-2 | En opgave kan udføres. En engangsopgave lukkes; en gentagen får ny frist | Opgaver |
| KRAV-OPG-3 | En opgave kan kopieres fra opgavelisten | Opgaver |
| KRAV-OPG-4 | En opgaves historik viser tidligere udførelser | Opgaver |
| KRAV-HAE-1 | En hændelse kan registreres og fremgår af hændelsesloggen | Hændelser |
| KRAV-HAE-2 | Gentagen indsendelse af den samme hændelse giver ikke en kopi | Hændelser |
| KRAV-HAE-3 | Hændelsesloggen kan filtreres på dato og hentes som excel | Hændelser |
| KRAV-DOK-1 | Et dokument kan oprettes med link og fremgår af listen | Dokumenter |
| KRAV-DOK-2 | Et dokument kan oprettes med fil og hentes igen | Dokumenter |
| KRAV-RAP-1 | Word-rapporterne kan dannes og hentes uden fejl | Rapporter |
| KRAV-RAP-2 | Excel-udtræk kan dannes og hentes uden fejl | Rapporter |
| KRAV-RAP-3 | Årshjulet viser årets opgaver | Rapporter |
| KRAV-ADM-1 | Tags kan oprettes og bruges | Administration |
| KRAV-ADM-2 | En valgliste kan udvides med en ny værdi, og værdien kan vælges | Administration |
| KRAV-ADM-3 | Auditloggen viser hvem der har ændret hvad | Administration |
