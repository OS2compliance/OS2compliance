# Change Log in OS2compliance

All notable changes to the project MUST be documented in this file.

The format can be based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to adher to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added
- (Løsningsbeskrivelse 21.04.2025) KLE-integration
- (Løsningsbeksrivelse 16.04.2025) Udvidelse af OS2kitos integration
- (https://os2web.atlassian.net/browse/COMPLY-65) Standarder: Ændring af nuværende forside
- (https://os2web.atlassian.net/browse/COMPLY-124) (Rapporter) Samlet rapport til systemejer vedr. alle systemer
- (https://os2web.atlassian.net/browse/COMPLY-320) (Risikostyring) OS2Compliance skal understøtte NIS2 risikovurderinger
- (https://os2web.atlassian.net/browse/COMPLY-283) (Risikostyring/Den enkelte risikovurdering) Fritekstfelt
- (https://os2web.atlassian.net/browse/COMPLY-182) (Risikostyring/Opsætning) Mulighed for at ændre konsekvenstyper
- (https://os2web.atlassian.net/browse/COMPLY-282) (Risikostyring/Forside) Risikovurderingens tilknytning til systemer/behandlingsaktiviteter

### Fixed
- (Ad-hoc) Riskovurdering: når man fjerner indhold forsvinder scoren ikke
- (Ad-hoc) Tekstfelt i konsekvensanalyserne (DPIA)
- (Ad-hoc) Konsekvensvurdering kolonne bredder
- (Ad-hoc) Bug i "Tilknyt ekstern risikovurdering" Aktiv-fane
- (Ad-hoc) Bug - Navigation forsvinder - implementeret burger menu
- (Ad-hoc) Søgning og sortering efter navn i konsekvensanalyser virkede ikke

## [2.2.1]
### Fixed
- (Ad-hoc) Der var en fejl på konsekvensvurderingsoverblikssiden, der fjorde at den ikke kunne vises korrekt, det er retttet.

## [2.2.0]

### Added
- (Ad-hoc) Mulighed for at vælge flere E-mail påmindelser
- (Ad-hoc) De enkelte kontroller medtages i årshjulet
- (Ad-hoc) Nyt kategoriseringsfelt på aktiver
- (Ad-hoc) Tilføj mulighed for at trække excel rapport over hændelser
- (Ad-hoc) Kolonne søgning tilføjet
- (Ad-hoc) Ny administrativ side, hvor det er muligt at tilføje aktive system-typer.
- (Syddjurs/Norddjurs) Ændringer til konsekvensvurderingsrapporten - Der tilføjes en liste af risikovurdering i rapporten
- (Syddjurs/Norddjurs) Mulighed for indsættelse af billeder - Det skal være muligt at indsætte billeder i besvarelserne i konsekvensanalysen.
- (Syddjurs/Norddjurs) Tilføjelser til risikovurderingsrapporten
- (Syddjurs/Norddjurs) Konsekvensanalysen: Ændring i linkede felter
- (Syddjurs/Norddjurs) Tilføjelse af eksterne risikovurdering
- (Syddjurs/Norddjurs) Ændring af eksisterende DPIA
- (Syddjurs/Norddjurs) Tilretning af aktiver -> DPIA-fanen
- (Syddjurs/Norddjurs) Ny konsekvensanalyse detalje side
- (Syddjurs/Norddjurs) Foranstaltninger genvej flyttes
- (Syddjurs/Norddjurs) Konsekvensanalyse genvej flyttes
- (Syddjurs/Norddjurs) Nyt konsekvensanalyser modul
- (Syddjurs/Norddjurs) Ændringer til konsekvensvurderingsrapporten

### Fixed
- (Ad-hoc) Årshjulet tog alle opgaver i stedet for kun dem der var markeret til at blive inkluderet.
- (Ad-hoc) Rediger trusselskatalog knappen virkede ikke
- (Ad-hoc) Fjernelse af revisionsdato fra et dokument gav fejl
- (Ad-hoc) Ikke muligt at slette risikovurderinger med " i navnet
- (Ad-hoc) Ikke muligt at oprette tilsyn, hvis ansvarlig ikke er sat
- (https://github.com/OS2compliance/OS2compliance/issues/191) Overfør inaktive rettigheder virkede ikke 
- (https://github.com/OS2compliance/OS2compliance/issues/193) Hændelser - Manglende visning af svar ved Valgliste eller Valgliste (flere svar) (Syddjurs)
- (https://github.com/OS2compliance/OS2compliance/issues/195) Sortering på Ubehandlet tilsyn virker ikke
- (https://github.com/OS2compliance/OS2compliance/issues/196) DBS aktive vises flere gange
- (https://github.com/OS2compliance/OS2compliance/issues/197) Bruger kan ikke lave tilknytninger

## [2.1.0] - 2024-11-22
### Added
- (https://os2web.atlassian.net/browse/COMPLY-172) (Generelt) Links skal være klikbare.
- (https://os2web.atlassian.net/browse/COMPLY-79)  (Aktiver/DPIA) Felt til DPO’s kommentarer.
- (https://os2web.atlassian.net/browse/COMPLY-184) (Generelt) Ny brugerrolle med adgang til dashboard og ansvarlige opgaver. Skal kunne løse egne opgaver.
- (Ad-hoc) Syddjurs: Flere tags kan udskrives på én gang.

## [2.0.0] - 2024-10-31

### Fixed
- (https://github.com/OS2compliance/OS2compliance/issues/187) Notifikations mail loop

### Added
- (https://os2web.atlassian.net/browse/COMPLY-68)  (Standarder) Når man klikker gem, så ryger man til toppen - det vil være skønt (og tidsbesparende) hvis man bliver udfor den man netop har redigeret
- (https://os2web.atlassian.net/browse/COMPLY-80)  Modul hvor brugerne kan registrere sikkerhedsbrud
- (https://os2web.atlassian.net/browse/COMPLY-122) Mulighed for at vælge hvilke kolonner man vil have vist på forsiderne
- (https://os2web.atlassian.net/browse/COMPLY-143) Integration med DBS
- (https://os2web.atlassian.net/browse/COMPLY-200) 7) Ny DPIA skabelon (Syddjurs)


## [1.2.0] - 2024-09-10

### Fixed
- (https://github.com/OS2compliance/OS2compliance/issues/126) Manglende validering af at et aktiv er valgt
- (https://github.com/OS2compliance/OS2compliance/issues/125) Status fjernet fra fravalgte sektioner i iso2700X
- (https://github.com/OS2compliance/OS2compliance/issues/97)  Sortering på kolonner med rød/gul/grøn
- (https://github.com/OS2compliance/OS2compliance/issues/135) Fix dead links from registers
- (https://github.com/OS2compliance/OS2compliance/issues/131) Fejl i samspillet mellem dokument og opgave ift. dato (Syddjurs)
- (https://github.com/OS2compliance/OS2compliance/issues/134) Intetsigende fejlside
- (https://github.com/OS2compliance/OS2compliance/issues/133) Eksterne links skal åbne på en ny fane
- (https://github.com/OS2compliance/OS2compliance/issues/141) Manglende visning af data
- (https://github.com/OS2compliance/OS2compliance/issues/145) Kopiering af kontrol fejler
- (https://github.com/OS2compliance/OS2compliance/issues/148) Sortering under mine opgaver
- (https://github.com/OS2compliance/OS2compliance/issues/156) Der modtages mail med deadline trods opgaven er løst
- (https://github.com/OS2compliance/OS2compliance/issues/150) Aktiver sorteres ikke alfabetisk (Herning)
- (https://github.com/OS2compliance/OS2compliance/issues/151) Hvis først man sætter en ansvarlig på en behandlingsaktivitet, så kan denne ikke slettes / sættes til blank igen
- (https://github.com/OS2compliance/OS2compliance/issues/153) Ikke muligt at flytte trussel i trusselskatalog
- (https://github.com/OS2compliance/OS2compliance/issues/167) Fejl i risikovurderingen (Jammerbugt)
- (https://github.com/OS2compliance/OS2compliance/issues/168) Tilknytning til slettet IT-system fjernes ikke automatisk på leverandøren når it-systemet slettes
- (https://github.com/OS2compliance/OS2compliance/issues/172) Kan ikke rette Navn på Leverandør
- (https://github.com/OS2compliance/OS2compliance/issues/173) Man kan ikke fjerne relationer på leverandør siden
- (https://github.com/OS2compliance/OS2compliance/issues/179) Tab af data når formål & lovhjemmel opdateres
- (https://github.com/OS2compliance/OS2compliance/issues/176) Søgning på navn fejler hvis ikke der søges på fulde navn
- (https://github.com/OS2compliance/OS2compliance/issues/178) Fejl rækkefølge i NSIS tag rapport
- (https://os2web.atlassian.net/browse/COMPLY-35) Fortegnelse/Formål & lovhjemmel: Artikel 9 er forkert
- (https://os2web.atlassian.net/browse/COMPLY-32) Fortegnelse/Formål & lovhjemmel: Fjern teksten "§6" og "§7"

### Added
- (https://github.com/OS2compliance/OS2compliance/issues/165) Oprettelse af en ny systemtype under aktiver der hedder ”Ydelse”
- (https://os2web.atlassian.net/browse/COMPLY-160) Understøttelse af fk-adgangsstyring
- (https://os2web.atlassian.net/browse/COMPLY-194) Fortegnelse: KL fortegnelsen skal opdateres med oprettelse af en ny behandlingsaktivitet
- (https://os2web.atlassian.net/browse/COMPLY-197) Katalog over foranstaltninger (Syddjurs)
- (https://os2web.atlassian.net/browse/COMPLY-167) Fortegnelse/Konsekvens- og risikovurdering: Risikovurderinger for de tilknyttede IT-systemer der er risikovurderet skal vises under Konsekvens- og Risikovurdering
- (https://os2web.atlassian.net/browse/COMPLY-181) Fortegnelse/Konsekvens- og risikovurdering: Ved valg af konsekvens mangler der tekst med skalabeskrivelse – lige nu står der bare 1, 2, 3 og 4
- (https://os2web.atlassian.net/browse/COMPLY-192) Fortegnelse/Konsekvens- og risikovurdering: Det skal ikke være Gns. vurdering som nu, men max. værdi for kolonnen som vises. Det skal også være max værdi der vises på fortegnelsessiden i kolonnen "konsekvensvurdering"
- (https://os2web.atlassian.net/browse/COMPLY-195) Overblik over hvornår opgaver er løst
- (https://os2web.atlassian.net/browse/COMPLY-196) Udvidelse af antal tegn i tekstfelt
- (https://os2web.atlassian.net/browse/COMPLY-198) Signering af risikovurdering (Syddjurs)
- (https://os2web.atlassian.net/browse/COMPLY-99)  Overblik over ansvarlige der er stoppet og mulighed for at massetildele ejerskab til en anden
- (https://os2web.atlassian.net/browse/COMPLY-130) Mulighed for tilretning af mailtekster
- (https://os2web.atlassian.net/browse/COMPLY-139) (Alle/forsider) Når man klikker tilbage, så skal man komme tilbage til den søgning og sortering man var ved


## [1.1.0] - 2024-03-15

### Fixed

- (Ad-hoc) Links felter alle steder i løsningen er lavet så de kan være 4000 tegn lange.
- (Ad-hoc) Dokumenter: Maks længde på beskrivelses felt ændret fra 255 til ~65000 tegn.
- (Ad-hoc) Aktiver: Sortering på ansvarlig, navn og leverandør fixed
- (https://github.com/OS2compliance/OS2compliance/issues/117) Send til "system ejer" skal være et ord (I risikovurderingen)
- (https://github.com/OS2compliance/OS2compliance/issues/111) Stavefejl i risikovurderingerne
- (https://github.com/OS2compliance/OS2compliance/issues/118) Mine opgaver: Viste ikke kontroller som tidligere var gennemført
- (https://github.com/OS2compliance/OS2compliance/issues/109) Udvidelse af beskrivelsesfeltet for en opgave
- (https://github.com/OS2compliance/OS2compliance/issues/116) Man kan ikke slette en ny trussel

### Changed

- (https://os2web.atlassian.net/browse/COMPLY-85)  Aktiver/Databehandling valgmulighederne ”ingen behandling af personoplysninger” eller "0" tilføjet
- (https://os2web.atlassian.net/browse/COMPLY-14)  Aktiver/Databehandling (gengivet under behandlingsaktivitet)) Der mangler en overskrift over typer af personoplysninger
- (https://os2web.atlassian.net/browse/COMPLY-23)  Behandlingsaktivitet/Fortegnelse Mulighed for at rette overskriften på en behandlingsaktivitet
- (https://os2web.atlassian.net/browse/COMPLY-30)  Dokumenter: hvis et dokument slettes, slettes tilhørende kontroller også
- (https://os2web.atlassian.net/browse/COMPLY-39)  Dokumenter: Når en kontol der vedrører et dokument gennemføres skal det slå igennem på dokumentet ift. hvornår det næste gang skal revideres
- (https://os2web.atlassian.net/browse/COMPLY-113) Dashboard: Søgbar tag kolonne tilføjet 
- (https://os2web.atlassian.net/browse/COMPLY-46)  Dashboard: De tre øverste kasser (brugeroplysninger samt kommende deadline for opgaver og kontroller) skal slettes
- (https://os2web.atlassian.net/browse/COMPLY-36)  Fortegnelse/Databehandling Mulighed for at redigere (slette og tilføje) i typer af personoplysninger
- (https://os2web.atlassian.net/browse/COMPLY-19)  Leverandør: Tilsyn aktiv kolonne tilføjet
- (https://os2web.atlassian.net/browse/COMPLY-40)  Opgavecenter: Status og resultat er blevet slået sammen (>1 md til deadline grå / 30-0 dage til deadline gul / overskredet rød + resultatet)
- (https://os2web.atlassian.net/browse/COMPLY-9)   Opgavecenter: Afdelingsfeltet skal udfyldes automatisk når ansvarlig angives
- (https://os2web.atlassian.net/browse/COMPLY-106) Opgavecenter: Opgaver skal kunne relatere sig til en leverandør
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Ny brugerflade, nemmere at betjene
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Overskrift følger med ned når man scroller
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Kategorier kan folde ud/ind
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Fagområde forudfyldes med den ansvarliges afdeling
- (https://os2web.atlassian.net/browse/COMPLY-37)  Aktiver: Alle ikke angivne felter sættes default tomme
- (https://os2web.atlassian.net/browse/COMPLY-37)  Fortegnelse: Alle ikke angivne felter sættes default tomme
- (https://os2web.atlassian.net/browse/COMPLY-31)  Fortegnelse: §6 litra f – marker med fed "f), gælder ikke for behandling, som offentlige myndigheder foretager som led i udførelsen af deres opgaver."
- (https://os2web.atlassian.net/browse/COMPLY-27)  Fortegnelse: Feltet til at tilføje relationer (aktiver) under behandlingsaktiviteter må gerne være bredere, så man kan se hele navnet
- (https://os2web.atlassian.net/browse/COMPLY-14)  Fortegnelse/Aktiver: Der mangler en overskrift over typer af personoplysninger
- (https://os2web.atlassian.net/browse/COMPLY-8)   Fortegnelse/Aktiver: Det er ændret så man kan sætte flere ansvarlige på, både afdelinger og personer
- (https://os2web.atlassian.net/browse/COMPLY-34)  Fortegnelse/Formål & lovhjemmel - Artikel 9 j) - slettet
- (https://os2web.atlassian.net/browse/COMPLY-17)  Standard/NSIS: Mulighed for at vælge status "ikke relevant"
- (https://os2web.atlassian.net/browse/COMPLY-16)  Understøttende standarder: Mulighed for at sortere i standarder, så man eksempelvis kun får vist dem i status "ikke klar"
 
### Added
- (https://os2web.atlassian.net/browse/COMPLY-42)  Aktiver: 3. landes overførsel kolonne tilføjet på oversigtssiden
- (https://os2web.atlassian.net/browse/COMPLY-25)  Aktiver: Tilføjet kolonne på oversigtssiden som viser antallet af understøttede behandlingsaktiviteter
- (https://os2web.atlassian.net/browse/COMPLY-105) Aktiver: Mulighed for at markere at et aktiv ikke skal risikovurderes
- (https://os2web.atlassian.net/browse/COMPLY-18)  Aktiver: Flueben på DPIA fanen, hvor der kan vælges "fravalgt" hvilket får farven i fanen til at forsvinde
- (https://os2web.atlassian.net/browse/COMPLY-26)  Behandlingsaktiviteter: kolonne med Ansvarlig forvaltning)
- (https://os2web.atlassian.net/browse/COMPLY-24)  Fortegnelse: kolonne på oversigtssiden der viser hvor mange IT-systemet der understøtter behandlingsaktiviteten
- (https://os2web.atlassian.net/browse/COMPLY-70)  Fortegnelse: Kolonne ved relaterede aktiver hvor det fremgår om aktivet er kritisk eller ej
- (https://os2web.atlassian.net/browse/COMPLY-8)   Opgavecenter: Mulighed for at udskrive kontrol historik
- (https://os2web.atlassian.net/browse/COMPLY-12)  Opgavecenter: Mulighed for at markere i en kontrol/opgave at den skal indgå i årshjul
- (https://os2web.atlassian.net/browse/COMPLY-6)   Opgavecenter: Mulighed for at angive dato interval under historik
- (https://os2web.atlassian.net/browse/COMPLY-2)   Opgavecenter: Mulighed for at kopiere en opgave
- (https://os2web.atlassian.net/browse/COMPLY-113) Opgavecenter: Søgbar tag kolonne tilføjet
- (https://os2web.atlassian.net/browse/COMPLY-109) Opgavecenter: Mulighed for at indsætte et link i en opgave
- (https://os2web.atlassian.net/browse/COMPLY-28)  Opgavecenter: Mulighed for at redigere titlen på en opgave
- (https://os2web.atlassian.net/browse/COMPLY-13)  Rapporter: Mulighed for at udskrive en årshjulsopgave i Excel
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Kopier funktion til at kopiere en trusselsvurdering
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Mulighed for at sætte "til stede på møde" (vises også i rapport)
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Mulighed for at redigere, titel, ansvarlig, fagområde og tilstede på møde
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Mulighed for at oprette opgaver der relaterer sig til en bestemt trussel
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Mulighed for at sende risikovurderings rapport til ansvarlig for et aktive/behandlingsaktivitet
- (https://os2web.atlassian.net/browse/COMPLY-88)  Risikovurdering: Mulighed for at sætte et revisions interval der vil danne en tilhørende opgave
- (https://os2web.atlassian.net/browse/COMPLY-90)  Risikovurdering: Hvis man vælger at koble med en konsekvensvurdering fra behandlingsaktiviteterne så skal der i bunden (under knyttede opgaver) listes de behandlingsaktiviteter der er knyttet til aktivet og deres vurderinger
- (https://os2web.atlassian.net/browse/COMPLY-111) Trusselskataloger: Nyt modul for administratorer, giver mulighed for at administrere trusselskataloger 
- (https://os2web.atlassian.net/browse/COMPLY-111) Trusselskataloger: Opret, rediger, og slet trusselskataloger (Sletning kun hvis trusselskataloget ikke er i brug)
- (https://os2web.atlassian.net/browse/COMPLY-111) Trusselskataloger: Mulighed for at sætte trusselskataloger som skjulte
