#!/usr/bin/env bash
set -u
GW=${GW:-http://localhost:8080}

titulo() { printf '\n\033[1;34m== %s ==\033[0m\n' "$1"; }
llamar() {
  if [ $# -ge 3 ]; then
    curl -s -X "$1" "$GW$2" -H 'Content-Type: application/json' -d "$3" -w '\nHTTP %{http_code}\n'
  else
    curl -s -X "$1" "$GW$2" -w '\nHTTP %{http_code}\n'
  fi
}
id_de() { sed -n '1p' | sed -E 's/.*"id":([0-9]+).*/\1/'; }

DNI="7$(printf '%07d' $(( (RANDOM * 32768 + RANDOM) % 10000000 )))"
PROF="MED-$(printf '%03d' $(( RANDOM % 1000 )))"
HORA=$(printf '%02d' $(( 8 + RANDOM % 11 )))
FECHA="$(date -d 'next monday' +%Y-%m-%d)T${HORA}:30:00"

PACIENTE='{"tipoDocumento":"DNI","numeroDocumento":"'$DNI'","nombres":"Lucia","apellidos":"Ficticia Demo",
"fechaNacimiento":"1992-03-15","telefono":"999111222","correo":"lucia.demo@correo-ficticio.pe","direccion":"Calle Ejemplo 100"}'

titulo "1. Registrar paciente (esperado 201)"
R=$(llamar POST /api/pacientes "$PACIENTE"); echo "$R"; PID=$(echo "$R" | id_de)

titulo "2. Consultar paciente por id (esperado 200)"
llamar GET /api/pacientes/$PID

titulo "3. Registrar el mismo documento otra vez (esperado 409)"
llamar POST /api/pacientes "$PACIENTE"

titulo "4. Registrar cita para el paciente: citas consulta a pacientes de forma SINCRONA (esperado 201)"
CITA='{"pacienteId":'$PID',"especialidad":"Cardiologia","fechaHora":"'$FECHA'","profesionalCodigo":"'$PROF'","motivo":"Control administrativo"}'
R=$(llamar POST /api/citas "$CITA"); echo "$R"; CID=$(echo "$R" | id_de)

titulo "5. Cita para un paciente inexistente (esperado 404)"
llamar POST /api/citas '{"pacienteId":999999,"especialidad":"Cardiologia","fechaHora":"'$FECHA'","profesionalCodigo":"MED-999"}'

titulo "6. Consultar la cita (esperado 200, estado PENDIENTE)"
llamar GET /api/citas/$CID

titulo "7. Confirmar la cita -> publica evento CitaConfirmada en RabbitMQ (esperado 200)"
llamar PATCH /api/citas/$CID/confirmar

titulo "8. Transicion no permitida: confirmar de nuevo (esperado 409)"
llamar PATCH /api/citas/$CID/confirmar

titulo "9. Registrar atencion simulada de la cita confirmada (esperado 201; la cita pasa a ATENDIDA)"
R=$(llamar POST /api/atenciones '{"citaId":'$CID',"area":"Consulta externa","observacion":"Atencion administrativa de prueba"}'); echo "$R"
AID=$(echo "$R" | id_de)

titulo "10. Registrar pago simulado (esperado 201)"
OP="OP-$(date +%s)"
llamar POST /api/pagos '{"pacienteId":'$PID',"atencionId":'$AID',"concepto":"Consulta externa (simulada)","monto":80.00,"medioPago":"TARJETA_DEBITO","numeroOperacion":"'$OP'"}'

titulo "11. Pago con monto negativo (esperado 400)"
llamar POST /api/pagos '{"pacienteId":'$PID',"concepto":"Prueba","monto":-10,"medioPago":"EFECTIVO","numeroOperacion":"OP-NEG-0001"}'

titulo "12. Pago con numero de operacion repetido (esperado 409)"
llamar POST /api/pagos '{"pacienteId":'$PID',"concepto":"Prueba","monto":10,"medioPago":"EFECTIVO","numeroOperacion":"'$OP'"}'

sleep 2
titulo "13. Notificaciones generadas por eventos (consumidor ms-notificaciones)"
llamar GET /api/notificaciones/paciente/$PID

titulo "14. Historial de auditoria de la cita (consumidor ms-auditoria)"
llamar GET /api/auditoria/recurso/CITA/$CID

titulo "15. Recurso inexistente (esperado 404)"
llamar GET /api/citas/999999

echo; echo "Paciente=$PID  Cita=$CID  Atencion=$AID  Profesional=$PROF  Fecha=$FECHA"
