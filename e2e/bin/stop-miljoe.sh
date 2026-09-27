#!/usr/bin/env bash
# Stopper e2e-appen. Databasecontaineren bliver stående, så en kørsel kan genoptages.
# Kør med --slet-db for også at fjerne databasen.
set -uo pipefail
cd "$(dirname "$0")/../.."

if [[ -f e2e/runs/app.pid ]]; then
  pid=$(cat e2e/runs/app.pid)
  # Kun hvis pid'et stadig er vores egen mvnw — et genbrugt pid må ikke dræbes.
  if ps -p "$pid" -o args= 2>/dev/null | grep -q "spring-boot.run.profiles=locallogin"; then
    pkill -P "$pid" 2>/dev/null
    kill "$pid" 2>/dev/null
    echo "App stoppet."
  else
    echo "Pid $pid er ikke e2e-appen længere — springer over."
  fi
  rm -f e2e/runs/app.pid
else
  echo "Ingen app.pid — intet at stoppe."
fi

if [[ "${1:-}" == "--slet-db" ]]; then
  docker rm -f os2compliance-e2e-db >/dev/null 2>&1 && echo "Database fjernet."
fi
