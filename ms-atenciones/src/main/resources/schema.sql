CREATE TABLE IF NOT EXISTS atencion (
    id                  BIGSERIAL    PRIMARY KEY,
    codigo              VARCHAR(20)  NOT NULL,
    paciente_id         BIGINT       NOT NULL,
    cita_id             BIGINT       NOT NULL,
    area                VARCHAR(60)  NOT NULL,
    observacion         VARCHAR(250),
    fecha_atencion      TIMESTAMP    NOT NULL,
    fecha_actualizacion TIMESTAMP    NOT NULL,
    estado              VARCHAR(12)  NOT NULL,
    CONSTRAINT uk_atencion_codigo UNIQUE (codigo),
    CONSTRAINT uk_atencion_cita UNIQUE (cita_id),
    CONSTRAINT ck_atencion_estado CHECK (estado IN ('EN_CURSO', 'FINALIZADA', 'ANULADA'))
);

CREATE INDEX IF NOT EXISTS idx_atencion_paciente ON atencion (paciente_id);
