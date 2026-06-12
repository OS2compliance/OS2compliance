# Change Log in OS2compliance

All notable changes to the project MUST be documented in this file.

The format can be based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to adher to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- (https://os2web.atlassian.net/browse/COMPLY-41) (Aktiver/Generelt) Visning af resultatet for senest gennemførte kontroller, deadline og opgavetype
- (https://os2web.atlassian.net/browse/COMPLY-57) (Aktiver/generelt) Markering af integrationer mellem systemer
- (https://os2web.atlassian.net/browse/COMPLY-149) (Aktiver/TIA) Mulighed for at notere at der ikke skal udarbejdes en TIA
- (https://os2web.atlassian.net/browse/COMPLY-150) (Aktiver/TIA) Mulighed for at indsætte link til TIA
- (https://os2web.atlassian.net/browse/COMPLY-292) (Aktiver/DPIA) Ved oprettelse af en påmindelse om at genbesøge DPIA skal det være systemansvarlig i stedet for systemejer, der default står som ansvarlig på opgaven
- (https://os2web.atlassian.net/browse/COMPLY-324) (Opgavecenter/Ny fane) Dynamisk og brugerrettet årshjul
- (https://os2web.atlassian.net/browse/COMPLY-350) (Opgavecenter) Kolonne der viser tilknyttet risikovurdering
- (gitlab #24) DBS-integration v2: migrering til DBS' nye API — ny API-klient (dbs-platform-client), samlet synkronisering, cutover via kitos_uuid og backfill pr. kommune
- (https://os2web.atlassian.net/browse/COMPLY-344) (Opgaver) Arbejdsgang for registrering af tilsyn og udførelse af den tilhørende opgave
- (https://os2web.atlassian.net/browse/COMPLY-336) (DBS tilsyn) Mulighed for at definere hvem der er ansvarlig
- (https://os2web.atlassian.net/browse/COMPLY-338) (DBS tilsyn) Aktiver uden tilsyn markeres med "ingen dba"
- (https://os2web.atlassian.net/browse/COMPLY-392) (Aktiver/DBS tilsyn) Navneændring fra "DBS tilsyn" til "Tilsyn"
- (https://os2web.atlassian.net/browse/COMPLY-263) (Aktiver/Generelt) Mulighed for at vælge flere tilsynsmodeller
- (https://os2web.atlassian.net/browse/COMPLY-121) (Aktiver/DBS tilsyn) Ved oprettelse af tilsyn via leverandøren foreslås tilknyttede systemer

### Fixed
- (gitlab #1) Fejl i antal løsninger
- (gitlab #2) Forkert terminologi
- (gitlab #3) Manglende download af DPIA
- (gitlab #13) Kan ikke fjerne lovhenvisninger fra "Relevante lovhenvisninger" på Fortegnelse
- (gitlab #14) Dialog for oprettelse af ny opgave lukker og mister data ved klik udenfor
- (gitlab #15) Notifikation om kommende deadline sendes selvom kontrol er udført
- (gitlab #18) NullPointerException i task-grid når task har NULL task_type eller repetition
- (gitlab #19) Datofelt på databehandleraftale fejler ved manuel indtastning uden ledende nul
- (gitlab #20) Kopiering af risikovurdering medtager ikke tilknyttede foranstaltninger
- (gitlab #21) Systemroller (Driftsansvarlig/Systemansvarlig) vises forkert i risikorapport (PDF/DOCX/Excel)
- (gitlab #22) Standard-ID med skråstreger ødelagde URL og gjorde standarden utilgængelig (input-validering på ID ved oprettelse + robust håndtering af 404 på progress-endpoint)
- (gitlab #23) Man kan ikke redigere/slette eksisterende foranstaltninger
- (gitlab #35) StandardController eksponerede entitet i stedet for DTO mod frontend
- (gitlab !469) Owner-brugere kunne ikke administrere trusler (tilføj/ret/slet samt vælg trusselskataloger) på egne risikovurderinger — frontend var gated til *_all-roller, mens backend tillod *_owner
- (gitlab !473) Død Rediger-knap på aktivets Sikkerhedsfane ved tilpasset sikkerhedsskema (JS-fejl når et betinget opfølgningsspørgsmål var fjernet fra kommunens skema)
- (gitlab !464) Signeringslink forblev aktivt efter sletning af en risikovurdering — en slettet/tilbagetrukket vurdering (eller DPIA) kunne stadig signeres og PDF'en blev signeret og uploadet pga. manglende else-gren i SigningController; viser nu en "trukket tilbage"-side
- (Ad-hoc) Manglende redigeringsmulighed i standarder
- (Ad-hoc) Man kan oprette en konsekvensanalyse uden værdier
- (Ad-hoc) JS-fejl i fortegnelse view
- (Ad-hoc) Forskellig styling på card bodies på fortegnelser og aktiver
- (Ad-hoc) Duplikerede tags vist på aktiv-oversigt
- (Ad-hoc) Samlet rettelse af notifikationslogik (notify-fix)
- (Ad-hoc) Sanitering af filnavne på downloads (header-parsing)
- (Ad-hoc) Diverse bugfixes i trusselsvurderings-rapport
- (Ad-hoc) CI: anvend seneste version af review-workflow

## [2.6.0] - 2026-03-24
### Added
- (Ad-hoc) Ændringer til aktiv-fanen Foranstaltninger
- (Ad-hoc) Tilføj link til manual
- (Ad-hoc) Til drift og fejlsøgning: Task der sørger efter duplikedere id'er i relatebar tabeller
- (Gentofte 2.1) Udregnede felter på aktiver og behandlingsaktiver
- (Gentofte 2.2) Udvidelse af ”Gem som Excel”
- (Gentofte 2.3) Udvidelser til aktiver og behandlingsaktiviteternes listevisning
- (https://os2web.atlassian.net/browse/COMPLY-249) (Risikostyring/Forside) Administrator skal kunne ændre "udgangspunkt" for en risikovurdering eksempelvis fra scenarie til it-system
- (https://os2web.atlassian.net/browse/COMPLY-297) (Aktiver/DBS Tilsyn) Under opgaver skal skal også være link til systemforsiden
- (https://os2web.atlassian.net/browse/COMPLY-307) Flere ting står på engelsk og ikke dansk
- (https://os2web.atlassian.net/browse/COMPLY-294) (Aktiver/Generelt) På listen over hændelser skal dato for oprettelsen vises
- (https://os2web.atlassian.net/browse/COMPLY-340) (Trusselskatalog) mulighed for at skifte linje i beskrivelsesfeltet

### Fixed
- (Ad-hoc) Sorteringsfejl i standarder
- (Ad-hoc) Fejl i statistikvisningen for hændelser
- (Ad-hoc) Kan ikke vælge databehandleraftale dato
- (Ad-hoc) Behandlingsaktiviteter -> KLE valg, broken
- (Ad-hoc) Fixed print report error 500
- (Ad-hoc) Adgang til redigering af Risikovurdering - Man kan åbne en risikovurdering, selvom man ikke har lov til det.
- (Ad-hoc) Gør fortegnelses dropdown mere læsbar
- (Ad-hoc) <br> tags i risiko pdf
- (Ad-hoc) DPIA Pdf, rettet fejl, hvor tabellen løber over, når et link er for langt til at passe ind i cellen
- (Ad-hoc) Risikovurdering dropdowns virker ikke i firefox
- (Ad-hoc) Konsekvensanalyse, fejl i revisionsinterval (javascript)
- (Ad-hoc) Fejl i overfør ansvar
- (Ad-hoc) OS2kitos ikoner mangler
- (Ad-hoc) Dokumenter med uden tags skaber en fejl
- (Ad-hoc) Dokumenter, fejl i inkluder i årshjul
- (Ad-hoc) Statistik, fejl i farver
- (Ad-hoc) Truslerne mangler når man henter en Excel rapport for en risikovurdering
- (Ad-hoc) Lange titler på risikovurderinger ødelægger knapper
- (Ad-hoc) Signerings fejlbesked
- (Ad-hoc) Mærkelige kolonnestørrelser på risikovurderinger
- (Ad-hoc) Fejl i behandlingsaktivitet overskrift

## [2.5.0] - 2025-11-14
### Added
- (https://os2web.atlassian.net/browse/COMPLY-43) (Opgavecenter/Opgave og Kontrol) Ved ny opgave skal det være muligt at klikke Gem og gå til opgave eller gem
- (https://os2web.atlassian.net/browse/COMPLY-58) (Opgavecenter/Opgave og Kontrol) Oplysningerne omkring et tilsyn skal kunne udfyldes fra opgaven
- (https://os2web.atlassian.net/browse/COMPLY-84) (Rapporter) Mulighed for at udtrække liste over kontaktpersoner på leverandør
- (https://os2web.atlassian.net/browse/COMPLY-110) (Opgavecenter/Opgave og Kontrol) Mouse over ved oprettelse af opgave
- (https://os2web.atlassian.net/browse/COMPLY-141) (Aktiver og Leverandør) Slettes et aktiv der kun har én leverandør skal leverandøren automatisk slettes
- (https://os2web.atlassian.net/browse/COMPLY-146) (Aktiver/Tilsyn) Ændring af tekst
- (https://os2web.atlassian.net/browse/COMPLY-173) (Administrativt) Det skal være muligt at kunne justere alle lister selv
- (https://os2web.atlassian.net/browse/COMPLY-177) (Risikovurdering/Forside) Mulighed for at skjule risikovurderinger fra overblikket
- (https://os2web.atlassian.net/browse/COMPLY-178) (Risikostyring/Forside) Kolonne på forsiden der viser antal løste opgaver
- (https://os2web.atlassian.net/browse/COMPLY-187) (Aktiver) Man skal kunne fravælge/inaktivere muligheden for at oprette systemer i OS2compliance (ved synk fra KITOS)
- (https://os2web.atlassian.net/browse/COMPLY-189) (Dokumenter) Revidering og årshjul
- (https://os2web.atlassian.net/browse/COMPLY-203) (Opgavecenter/Opgave) Man skal kunne knytte flere ansvarlige til en opgave
- (https://os2web.atlassian.net/browse/COMPLY-216) (Leverandører) KITOS logo
- (https://os2web.atlassian.net/browse/COMPLY-218) (Risikostyring/Den enkelte risikovurdering) Udskriv i Excel
- (https://os2web.atlassian.net/browse/COMPLY-225) (Opgavecenter/Opgave og Kontrol) Mulighed for tjekliste
- (https://os2web.atlassian.net/browse/COMPLY-226) (Opgavecenter/Opgave og Kontrol) Tags skal være i forskellige farver
- (https://os2web.atlassian.net/browse/COMPLY-237) (Risikostyring/Den enkelte risikovurdering) Udvid/luk alle trusler
- (https://os2web.atlassian.net/browse/COMPLY-242) (Risikostyring/Risikovurderingsrapport) Tilføjelser til risikovurderingsrapporten
- (https://os2web.atlassian.net/browse/COMPLY-250) (Risikovurdering/Den enkelte risikovurdering) Mulighed for at rette i en tilføjet trussel
- (https://os2web.atlassian.net/browse/COMPLY-266) (Opgavecenter/Opgave) Ved Kontrol status ”Ingen fejl” kræves der alligevel en kommentar
- (https://os2web.atlassian.net/browse/COMPLY-267) (Aktiver og leverandør/Tilsyn) Mulighed for at slette en leverandør fra et aktiv
- (https://os2web.atlassian.net/browse/COMPLY-269) (Aktiver/Tilsyn og databehandling) Flyt felter om databehandleraftaler fra fanen Tilsyn
- (https://os2web.atlassian.net/browse/COMPLY-279) (Rapporter/Årshjul) Rapport over årshjul (i praksis var det bare status der manglede for rapporten)
- (https://os2web.atlassian.net/browse/COMPLY-281) (Opgavecenter/Forside) Ved "kopier opgave" skal der kunne vinges af i "Skal indgå i årshjul".
- (https://os2web.atlassian.net/browse/COMPLY-284) (Risikostyring/Den enkelte risikovurdering) CC felt ved afsendelse
- (https://os2web.atlassian.net/browse/COMPLY-285) (Risikostyring/Risikovurderinger) Risikokataloger skal stå i alfabetisk rækkefølge
- (https://os2web.atlassian.net/browse/COMPLY-286) (Risikostyring/Risikovurderinger) Signeringsknappen (både for risikovurdering og DPIA) skal være i bunden af dokumentet
- (https://os2web.atlassian.net/browse/COMPLY-288) (Risikostyring/Rapporten) Ikke relevante trusler med i rapporten
- (https://os2web.atlassian.net/browse/COMPLY-296) (Aktiver/DBS Tilsyn) Ændring af ordlyd
- (https://os2web.atlassian.net/browse/COMPLY-298) (Leverandører) 3. landes overblik
- (https://os2web.atlassian.net/browse/COMPLY-299) (Leverandører+aktiver/oversigt) Overblik og tilsyn
- (https://os2web.atlassian.net/browse/COMPLY-301) (Dashboard) Mulighed for at se brugerrolle
- (https://os2web.atlassian.net/browse/COMPLY-305) (Hændelser) Felt til klikbar links
- (https://os2web.atlassian.net/browse/COMPLY-308) (Hændelser/Hændelseslog) Mulighed for at kunne tagge til fx risikovurdering, konsekvensanalyser, behandlingsaktiviteter, dokumenter.
- (https://os2web.atlassian.net/browse/COMPLY-309) (Dokumenter/Nyt Dokument) Flere dokumenttyper
- (https://os2web.atlassian.net/browse/COMPLY-310) (Opgavecenter/Opgave og kontrol) Dato for udført kontrol
- (https://os2web.atlassian.net/browse/COMPLY-311) (Opgavecenter/Opgave og Kontrol) Beskrivelse af udførte kontroller
- (https://os2web.atlassian.net/browse/COMPLY-312) (Opgavecenter/Opgave og Kontrol) Individuel opsætning af påmindelse omkring opgaver
- (https://os2web.atlassian.net/browse/COMPLY-314) (Administrativt) Tags - Mulighed for at ændre/rette navn på tag efter oprettelse
- (https://os2web.atlassian.net/browse/COMPLY-315) (Administrativt) Ændring af modul-navn
- (https://os2web.atlassian.net/browse/COMPLY-329) (Aktiver/Databehandling) Nyt felt til logning
- (https://os2web.atlassian.net/browse/COMPLY-330) (Aktiver/Databehandling) Nyt felt til brugerstyring
- (https://os2web.atlassian.net/browse/COMPLY-341) Mulighed for massekontrol
- (https://os2web.atlassian.net/browse/COMPLY-242) (Risikostyring/Risikovurderingsrapport) Tilføjelser til risikovurderingsrapporten
- (https://os2web.atlassian.net/browse/COMPLY-351) (Risikostyring/Trusselsbillede) Navnet "Trusselsbillede" skal ændres til "Risikobillede"
- (https://os2web.atlassian.net/browse/COMPLY-352) (Standarder) Når man opretter et krav skal tallet i gruppenavnet være med ved ”tilhører til”
- (https://os2web.atlassian.net/browse/COMPLY-353) (Standarder) Når man opretter et krav står der ”tilhører til” det skal hedde ”tilhører” eller ”gruppe”
- (https://os2web.atlassian.net/browse/COMPLY-354) (Behandlingsaktiviteter/generelt) Statusfeltet skal man selv kunne definere under valglister
- (https://os2web.atlassian.net/browse/COMPLY-355) (Aktiver) Mulighed for selv at redigere valgliste
- (https://os2web.atlassian.net/browse/COMPLY-356) (Behandlingsaktivitet/Databehandling) Udfor notefeltet lige under Typer af personoplysninger skrives "Note"
- (https://os2web.atlassian.net/browse/COMPLY-357) (Standard) Udskriv SoA

### Changed
- (https://os2web.atlassian.net/browse/COMPLY-351) (Risikostyring/Trusselsbillede) Navnet "Trusselsbillede" skal ændres til "Risikobillede"
- (https://os2web.atlassian.net/browse/COMPLY-353) (Standarder) Når man opretter et krav står der ”tilhører til” det skal hedde ”tilhører” eller ”gruppe”

### Fixed
- (Ad-hoc) Fjern ikke OS2kitos oplysninger fra aktiv når der gemmes
- (Ad-hoc) Behandlingsansvarlige forsvandt når behandlingsrelation blev gemt, hvor man ikke selv stod som behandlingsansvarlig
- (Ad-hoc) Statistik overskredne opgaver virkede ikke
- (Ad-hoc) Tilføjet validering af trusselstype input felt, så der ikke sker server fejl, når man skriver for meget tekst 
- (Ad-hoc) Brug residual risiko i risk matrixen på dashboard
- (Ad-hoc) Fix risikovurderingsrapport kunne ikke dannes, hvis der var brug & tegn i kommentaren.

## [2.4.0] - 2025-09-10
### Added
- (https://os2web.atlassian.net/browse/COMPLY-253) (Risikostyring/Forside) Mulighed for at lave et udtræk af listen med alle risikovurderinger.
- (https://os2web.atlassian.net/browse/COMPLY-100) Global søgning
- (https://os2web.atlassian.net/browse/COMPLY-136) Tilføjelse af felter til ansvarlig forvaltning
- (https://os2web.atlassian.net/browse/COMPLY-224) Man skal kunne sætte flere links ind på en opgave
- (https://os2web.atlassian.net/browse/COMPLY-331) (Aktiver) Systemansvarlig som kolonne på listevisningen
- (https://os2web.atlassian.net/browse/COMPLY-277) Visningen i opgavehistorikken skal vende om, så den seneste kontrol står øverst
- (https://os2web.atlassian.net/browse/COMPLY-165) Opgavecenter - Mulighed for at tilføje forvaltning
- (https://os2web.atlassian.net/browse/COMPLY-69) Standarder - Oversigt over progression
- (https://os2web.atlassian.net/browse/COMPLY-252) (Administrativt) Mulighed for at konfigurere/indstille roller -> Denne er blevet til 2 nye roller begrænset og læse adgang. 
- (https://os2web.atlassian.net/browse/COMPLY-295) (COMPLY-295) Mulighed for at koble tilsyn på flere aktiver
- (https://os2web.atlassian.net/browse/COMPLY-323) Statistik modul, samling af forskellige ønsker, COMPLY-323 er den primære.
- (Ad-hoc) Mulighed for at udskrive standarder
- (Ad-hoc) Advarsel når man går væk fra en side man er ved at redigere

### Fixed
- (Adhoc) Tilføj dokument til aktiv virker ikke
- (Adhoc) Rettet OS2kitos integration, den fjernede ikke ansvarlige, den tilføjede kun
- (Adhoc) Print kontrol virkede ikke 

### Changed
- (Adhoc) Added more KLE details after test feedback 
- (Adhoc) Split excel export functionality into own endpoints

### Removed
- (Adhoc) Fjernet DPO for behandlingsaktiviteter, da der allerede var en global indstilling

## [2.3.0] - 2025-08-23

### Added
- (Løsningsbeskrivelse 15.04.2025) Fortegnelsesmodulet
- (Løsningsbeskrivelse 21.04.2025) KLE-integration
- (Løsningsbeksrivelse 16.04.2025) Udvidelse af OS2kitos integration
- (https://os2web.atlassian.net/browse/COMPLY-65) Standarder: Ændring af nuværende forside
- (https://os2web.atlassian.net/browse/COMPLY-124) (Rapporter) Samlet rapport til systemejer vedr. alle systemer
- (https://os2web.atlassian.net/browse/COMPLY-320) (Risikostyring) OS2Compliance skal understøtte NIS2 risikovurderinger
- (https://os2web.atlassian.net/browse/COMPLY-283) (Risikostyring/Den enkelte risikovurdering) Fritekstfelt
- (https://os2web.atlassian.net/browse/COMPLY-182) (Risikostyring/Opsætning) Mulighed for at ændre konsekvenstyper
- (https://os2web.atlassian.net/browse/COMPLY-282) (Risikostyring/Forside) Risikovurderingens tilknytning til systemer/behandlingsaktiviteter
- (https://os2web.atlassian.net/browse/COMPLY-260) (Aktiver/Generelt) Løsninger der imødekommer AI-forordningen
- (https://os2web.atlassian.net/browse/COMPLY-321) (Risikostyring) Risikostyring med visuel risikomatrix
- (https://os2web.atlassian.net/browse/COMPLY-101) (Administrativt) Overblik over hvilke mails systemet har sendt
- (https://os2web.atlassian.net/browse/COMPLY-66)  (Standarder/Understøttende standarder) Oprettelse af ny standard
- (https://os2web.atlassian.net/browse/COMPLY-262) (Aktiver/Generelt) Nyt felt på aktivets forside så der er 4 forskellige roller.
- (https://os2web.atlassian.net/browse/COMPLY-289) (Risikostyring/Risikovurderinger) Mulighed for at vælge flere trusselskataloger
- (https://os2web.atlassian.net/browse/COMPLY-255) (Rapporter/overblikssiderne) Mulighed for at eksportere alle sider med visninger til Excel
- (https://os2web.atlassian.net/browse/COMPLY-176) (Aktiver/oversigt) Markering på aktiver, der ikke længere er markeret som anvendt i KITOS
- (https://os2web.atlassian.net/browse/COMPLY-325) (Aktiver/Generelt) Visualisering af sammenhæng mellem systemer
- (https://os2web.atlassian.net/browse/COMPLY-188) (Opgavecenter/Opgave og Kontrol) Udvidelse af beskrivelsesfeltet i en opgave, så det automatisk passer til tekstens længde

### Fixed
- (Ad-hoc) Riskovurdering: når man fjerner indhold forsvinder scoren ikke
- (Ad-hoc) Tekstfelt i konsekvensanalyserne (DPIA)
- (Ad-hoc) Konsekvensvurdering kolonne bredder
- (Ad-hoc) Bug i "Tilknyt ekstern risikovurdering" Aktiv-fane
- (Ad-hoc) Bug - Navigation forsvinder - implementeret burger menu
- (Ad-hoc) Søgning og sortering efter navn i konsekvensanalyser virkede ikke
- (Ad-hoc) Sæt max længde på tilsyns konklusion op, og tilføje validering
- (Ad-hoc) Gør det muligt at slette dokumenter, som er anvendt som dokumentation for opgave udførsel

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
