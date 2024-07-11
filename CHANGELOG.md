# Change Log in OS2compliance

All notable changes to the project MUST be documented in this file.

The format can be based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to adher to [Semantic Versioning](http://semver.org/).

## [Unreleased]

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
- (https://os2web.atlassian.net/browse/COMPLY-35) Fortegnelse/Formål & lovhjemmel: Artikel 9 er forkert
- (https://os2web.atlassian.net/browse/COMPLY-32) Fortegnelse/Formål & lovhjemmel: Fjern teksten "§6" og "§7"

### Added
- (https://os2web.atlassian.net/browse/COMPLY-160) Understøttelse af fk-adgangsstyring
- (https://os2web.atlassian.net/browse/COMPLY-194) Fortegnelse: KL fortegnelsen skal opdateres med oprettelse af en ny behandlingsaktivitet
- (https://os2web.atlassian.net/browse/COMPLY-197) Katalog over foranstaltninger (Syddjurs)
- (https://os2web.atlassian.net/browse/COMPLY-167) Fortegnelse/Konsekvens- og risikovurdering: Risikovurderinger for de tilknyttede IT-systemer der er risikovurderet skal vises under Konsekvens- og Risikovurdering
- (https://os2web.atlassian.net/browse/COMPLY-181) Fortegnelse/Konsekvens- og risikovurdering: Ved valg af konsekvens mangler der tekst med skalabeskrivelse – lige nu står der bare 1, 2, 3 og 4
- (https://os2web.atlassian.net/browse/COMPLY-192) Fortegnelse/Konsekvens- og risikovurdering: Det skal ikke være Gns. vurdering som nu, men max. værdi for kolonnen som vises. Det skal også være max værdi der vises på fortegnelsessiden i kolonnen "konsekvensvurdering"

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
