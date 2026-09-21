#!/usr/bin/env bash
# Rejser et isoleret e2e-miljø: egen MariaDB på 3307 og appen på http://localhost:8444.
# Rører aldrig udviklingsdatabasen på 3306.
set -euo pipefail
cd "$(dirname "$0")/../.."

DB_CONTAINER=os2compliance-e2e-db
DB_NAME=os2compliance_e2e
DB_PORT=3307
APP_PORT=8444
LOG=e2e/runs/app.log

nulstil=${1:-}
if [[ -n "$nulstil" && "$nulstil" != "--nulstil" ]]; then
  echo "Ukendt argument: $nulstil (kun --nulstil findes)" >&2
  exit 1
fi

# En app der stadig lytter ville besvare parathedstjekket, så kørslen ramte den gamle build.
if curl -sf "http://localhost:${APP_PORT}/login" -o /dev/null 2>&1; then
  echo "Der kører allerede noget på port ${APP_PORT} — stop det først med e2e/bin/stop-miljoe.sh" >&2
  exit 1
fi

if [[ "$nulstil" == "--nulstil" ]] || ! docker ps -q -f name="^${DB_CONTAINER}$" | grep -q .; then
  echo "==> Starter frisk database ($DB_CONTAINER på $DB_PORT)"
  docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true
  # Samme image og samme standardkollation som docker-compose.yml: tabeller uden egen
  # collate arver serverens, og V1_124 sammenligner en af dem med en CAST.
  docker run -d --name "$DB_CONTAINER" \
    -e MARIADB_ROOT_PASSWORD=Test1234 \
    -e MARIADB_DATABASE="$DB_NAME" \
    -p "${DB_PORT}:3306" \
    mariadb:10.6.14 >/dev/null
fi

echo "==> Venter på database"
for i in $(seq 1 60); do
  docker exec "$DB_CONTAINER" mariadb -uroot -pTest1234 -e "SELECT 1" >/dev/null 2>&1 && break
  sleep 2
  [[ $i == 60 ]] && { echo "Databasen kom aldrig op"; exit 1; }
done

echo "==> Oversætter (også src/test — loginet til kørslen ligger der)"
./mvnw -q -o test-compile

echo "==> Starter app på http://localhost:${APP_PORT} (log: $LOG)"
mkdir -p e2e/runs
# Harniskets login ligger på /e2e/** og skal kunne nås uden session og uden csrf-token.
NONSECURED='/,/error,/manage/**,/webjars/**,/login,/login/**,/css/**,/vendor/**,/js/**,/img/**,/favicon.ico,/toastr.js.map,/api/**,/e2e/**'
CSRF_BYPASS='/,/manage/**,/webjars/**,/css/**,/vendor/**,/js/**,/img/**,/favicon.ico,/toastr.js.map,/api/**,/e2e/**'
DB_URL="jdbc:mysql://localhost:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=UTC" \
DB_USERNAME=root DB_PASSWORD=Test1234 \
SSL_ENABLED=false SERVER_PORT="$APP_PORT" \
SCHEDULING_ENABLED=false DEVELOPMENT_MODE=true \
KLECLIENT_ENABLED=false INTEGRATION_MAIL_ENABLED=false \
MUNICIPAL_NAME="E2E Testorganisation" \
nohup ./mvnw -q -o spring-boot:run \
  -Dspring-boot.run.profiles=locallogin \
  -Dspring-boot.run.additional-classpath-elements=target/test-classes \
  -Dspring-boot.run.jvmArguments="-Ddi.saml.pages.nonsecured=$NONSECURED -Ddi.saml.pages.csrfBypass=$CSRF_BYPASS" \
  > "$LOG" 2>&1 &
# target/test-classes med på stien, fordi LocalLoginController kun findes under src/test — den må ikke kunne havne i en release.
echo $! > e2e/runs/app.pid

echo "==> Venter på at appen svarer (Flyway kører 130+ migreringer, det tager et minut)"
for i in $(seq 1 120); do
  if curl -sf "http://localhost:${APP_PORT}/login" -o /dev/null; then
    echo "==> Appen er oppe"
    break
  fi
  if ! kill -0 "$(cat e2e/runs/app.pid)" 2>/dev/null; then
    echo "Appen døde under opstart — se $LOG"; tail -40 "$LOG"; exit 1
  fi
  sleep 3
  [[ $i == 120 ]] && { echo "Appen kom aldrig op — se $LOG"; tail -40 "$LOG"; exit 1; }
done

echo "==> Indlæser e2e-brugere"
shopt -s nullglob
for f in e2e/seed/*.sql; do
  docker exec -i "$DB_CONTAINER" mariadb -uroot -pTest1234 "$DB_NAME" < "$f"
  echo "    $f"
done

cat <<TXT

Klar.
  URL:      http://localhost:${APP_PORT}/e2e/login
  Login:    e2e-admin / E2E-test1234   (også e2e-super og e2e-bruger)
  Stop med: e2e/bin/stop-miljoe.sh
TXT
