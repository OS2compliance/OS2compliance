---
description: Kør e2e-drejebogen i browseren og dan HTML-rapport med skærmbilleder og fund
---

Kør den manuelle e2e-drejebog i en rigtig browser.

Læs `e2e/README.md` og følg afsnittet "Når Claude kører den".

Argumenter (valgfrit): `$ARGUMENTS` — er der angivet modulkoder (fx `ADG AKT OPG`),
køres kun de moduler. Ellers køres hele drejebogen.

Kort:
1. `e2e/bin/stop-miljoe.sh` hvis noget kører, så `e2e/bin/start-miljoe.sh --nulstil`
   og `node e2e/bin/ny-koersel.mjs`.
2. Gennemgå tilfældene i nummerorden via Playwright mod http://localhost:8444
   i **2560x1440**. Login: `/e2e/login` med `e2e-admin` / `E2E-test1234`.
   Erstat `{KØRSEL}` med kørslens id.
3. Vent på at tabellerne er hentet før du vurderer et trin — de loader asynkront.
   Kig i `e2e/runs/app.log` ved et fejlet trin; fejlen står sjældent i brugerfladen.
4. Udfyld `observeret`, `status`, `skud` og `fund` i `e2e/runs/<id>/resultat.json`.
   Skærmbilleder gemmes i `e2e/runs/<id>/skud/` som `.jpg`.
5. `node e2e/bin/rapport.mjs <id>` og fortæl hvad der fejlede.

Ret ikke fejl undervejs — de skal stå i rapporten.
