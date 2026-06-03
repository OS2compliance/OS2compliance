# DBS Platform v2 — Driftsguide

Denne guide beskriver udrulning af den nye DBS-integration (v2) pr. kommune.

## Konfigurationsoversigt

| Property | Påkrævet | Beskrivelse |
|---|---|---|
| `dbs.platform.client.base-path` | Ja | Base URL for DBS Platform API |
| `dbs.platform.client.api-key` | Ja | API-nøgle modtaget fra kommunen |
| `os2compliance.integrations.dbs.backfillFrom` | Ja (første gang) | Dato hvorfra historiske tilsyn hentes (YYYY-MM-DD) |
| `os2compliance.integrations.dbs.enabled` | Nej | Skal være `true` (default). Styrer om DBS-sync kører |
| `os2compliance.integrations.dbs.platform.cron` | Nej | Cron-udtryk for sync-skema. Default: `0 0 3 * * *` (kl. 03:00) |

## Udrulningsprocedure pr. kommune

### 1. Modtag API-nøgle

Kommunen opretter en API-nøgle i DBS-platformen og sender den sikkert via `kundenet.digital-identity.dk`. Kommunen oplyser desuden hvilken dato den gamle integration blev lukket ned — dette bruges som backfill-startdato.

### 2. Konfigurér miljøvariabler

Tilføj til kommunens konfiguration:

```properties
dbs.platform.client.base-path=https://api.dbstilsyn.dk
dbs.platform.client.api-key=<nøgle fra kommunen>
os2compliance.integrations.dbs.backfillFrom=<dato for nedlukning af gammel integration>
```

Når `base-path` er sat, aktiveres den nye integration automatisk ved næste deploy. Den gamle integration deaktiveres automatisk (via `@ConditionalOnMissingBean`).

### 3. Deploy og verificér backfill

Deploy applikationen. Ved næste cron-kørsel (default kl. 03:00) sker følgende automatisk:

1. Sync-tasken opdager at der ikke findes et tidligere sync-timestamp
2. `backfillFrom`-datoen bruges som `publishedAfter`-filter mod API'et
3. Historiske tilsyn hentes, og suppliers, systemer og oversights synkroniseres
4. Systemer kobles automatisk til eksisterende aktiver via `kitos_uuid`
5. Ved succes gemmes et timestamp — efterfølgende kørsler henter kun nyere tilsyn

### 4. Verificér i loggen

Søg efter følgende log-linjer for at bekræfte:

INFO  - Started: DBS Platform Sync

INFO  - Backfill run with publishedAfter=<dato>

INFO  - Fetched <antal> audits from DBS Platform API

INFO  - DBS Platform sync result: <N> new suppliers, <N> new systems, <N> new oversights, <N> updated oversights

INFO  - Kitos cutover: <N> matched via kitos_uuid, <N> without match

INFO  - Finished: DBS Platform Sync in <N> ms

Ting at tjekke:

- Antal hentede audits ser fornuftigt ud for kommunen
- "matched via kitos_uuid" viser at eksisterende aktiv-relationer er bevaret
- Ingen ERROR-linjer

### 5. Fjern backfillFrom (valgfrit)

Når backfill er kørt succesfuldt, kan `backfillFrom` fjernes fra konfigurationen. Den bruges kun når der ikke er et eksisterende sync-timestamp, så den har ingen effekt efter første kørsel.

## Håndtering af fejlscenarier

### Ugyldig eller manglende API-nøgle

Loggen viser:
ERROR - DBS Platform API error: HTTP 401 - Unauthorized

Løsning: Verificér at `dbs.platform.client.api-key` er korrekt konfigureret og at nøglen er aktiv i DBS-platformen.

### API rate limiting

Loggen viser:
ERROR - DBS Platform API error: HTTP 429 - Too Many Requests

Løsning: Sync forsøges automatisk igen ved næste cron-kørsel. Hvis problemet fortsætter, kontakt DBS.

### Delvis backfill (sync afbrudt undervejs)

Hvis sync fejler halvvejs (fx netværksfejl), gemmes intet timestamp. Ved næste kørsel prøves hele backfill-perioden igen. Allerede oprettede suppliers, systemer og oversights opdateres blot — synkroniseringen er idempotent.

### Ingen audits returneret

Loggen viser:
INFO  - Fetched 0 audits from DBS Platform API
INFO  - No audits to synchronize

Mulige årsager:

- `backfillFrom` er sat til en dato efter alle tilsyn
- API-nøglen er gyldig men tilknyttet en kommune uden tilsyn i perioden
- Kommunen har ikke publicerede tilsyn i DBS endnu

### Kitos-matching fejler for mange systemer

Hvis "without match" er højt i loggen, skyldes det at systemerne i DBS ikke har et `kitos_uuid`, eller at det ikke matcher nogen eksisterende aktiver i OS2compliance. Dette er forventeligt for systemer der ikke er oprettet via KITOS-integrationen.

## Nødprocedure: Genstart sync fra bunden

Hvis det er nødvendigt at køre sync helt forfra:

1. Slet sync-timestampet fra `settings`-tabellen: `DELETE FROM settings WHERE setting_key = 'dbs_platform_last_sync';`
2. Sæt `backfillFrom` til den ønskede startdato
3. Ved næste cron-kørsel hentes alt fra den dato igen (idempotent)