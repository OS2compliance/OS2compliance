# Change Log in OS2 projects/products

All notable changes to the project MUST be documented in this file.

The format can be based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to adher to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Fixed
- (https://github.com/OS2compliance/OS2compliance/issues/126) Manglende validering af at et aktiv er valgt


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
