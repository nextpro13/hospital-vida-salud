CREATE TABLE IF NOT EXISTS pago (
    id                  BIGSERIAL     PRIMARY KEY,
    numero_operacion    VARCHAR(30)   NOT NULL,
    paciente_id         BIGINT        NOT NULL,
    atencion_id         BIGINT,
    concepto            VARCHAR(120)  NOT NULL,
    monto               NUMERIC(10,2) NOT NULL,
    medio_pago          VARCHAR(20)   NOT NULL,
    fecha_pago          TIMESTAMP     NOT NULL,
    fecha_actualizacion TIMESTAMP     NOT NULL,
    estado              VARCHAR(12)   NOT NULL,
    CONSTRAINT uk_pago_numero_operacion UNIQUE (numero_operacion),
    CONSTRAINT ck_pago_monto_positivo CHECK (monto > 0),
    CONSTRAINT ck_pago_medio CHECK (medio_pago IN ('EFECTIVO', 'TARJETA_DEBITO', 'TARJETA_CREDITO', 'TRANSFERENCIA')),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('REGISTRADO', 'ANULADO'))
);

CREATE INDEX IF NOT EXISTS idx_pago_paciente ON pago (paciente_id);
