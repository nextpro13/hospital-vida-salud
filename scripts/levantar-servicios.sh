#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p logs

if [ "${1:-}" = "detener" ]; then
  for f in logs/*.pid; do
    [ -f "$f" ] && kill "$(cat "$f")" 2>/dev/null && echo "Detenido $(basename "$f" .pid)"
    rm -f "$f"
  done
  exit 0
fi

if [ ! -f .env ]; then
  echo "Falta el archivo .env. Copie .env.example como .env y complete las claves." >&2
  exit 1
fi
set -a; source .env; set +a

if [ -f ./mvnw ]; then MVN=./mvnw; else MVN=mvn; fi
$MVN -q -DskipTests package

iniciar() {
  local modulo=$1
  local jar
  jar=$(ls "$modulo"/target/"$modulo"-*.jar | grep -v original | head -1)
  nohup java -jar "$jar" > "logs/$modulo.log" 2>&1 &
  echo $! > "logs/$modulo.pid"
  echo "Iniciado $modulo (pid $!) -> logs/$modulo.log"
}

iniciar eureka-server
echo "Esperando a Eureka (15 s)..."; sleep 15
for m in ms-pacientes ms-citas ms-atenciones ms-pagos ms-notificaciones ms-auditoria api-gateway; do
  iniciar "$m"
done
echo
echo "Listo. Panel de Eureka: http://localhost:8761  |  Gateway: http://localhost:8080"
echo "Espere ~30 s a que todos se registren antes de ejecutar ./scripts/demo-flujo.sh"
