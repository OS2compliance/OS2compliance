# Change Log in OS2 projects/products

All notable changes to the project MUST be documented in this file.

The format can be based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to adher to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Fixed
- Links felter alle steder i løsningen er lavet så de kan være 4000 tegn lange.
- Dokumenter: Maks længde på beskrivelses felt ændret fra 255 til ~65000 tegn.

### Changed

- Dokumenter: hvis et dokument slettes, slettes tilhørende kontroller også 
- Dashboard: Søgbar tag kolonne tilføjet 
- Leverandør: Tilsyn aktiv kolonne tilføjet
- Opgavecenter: Status og resultat er blevet slået sammen (>1 md til deadline grå / 30-0 dage til deadline gul / overskredet rød + resultatet)
- Opgavecenter: Mulighed for at kopiere en opgave
- Risikovurdering: Ny brugerflade, nemmere at betjene
- Risikovurdering: Overskrift følger med ned når man scroller
- Risikovurdering: Kategorier kan folde ud/ind
- Risikovurdering: Fagområde forudfyldes med den ansvarliges afdeling

### Added

- Opgavecenter: Mulighed for at udskrive kontrol historik
- Opgavecenter: Mulighed for at angive dato interval under historik
- Risikovurdering: Kopier funktion til at kopiere en trusselsvurdering
- Risikovurdering: Mulighed for at sætte "til stede på møde" (vises også i rapport)
- Risikovurdering: Mulighed for at redigere, titel, ansvarlig, fagområde og tilstede på møde
- Risikovurdering: Mulighed for at oprette opgaver der relaterer sig til en bestemt trussel
- Risikovurdering: Mulighed for at sende risikovurderings rapport til ansvarlig for et aktive/behandlingsaktivitet
- Risikovurdering: Mulighed for at sætte et revisions interval der vil danne en tilhørende opgave

- Trusselskataloger: Nyt modul for administratorer, giver mulighed for at administrere trusselskataloger 
- Trusselskataloger: Opret, rediger, og slet trusselskataloger (Sletning kun hvis trusselskataloget ikke er i brug)
- Trusselskataloger: Mulighed for at sætte trusselskataloger som skjulte
