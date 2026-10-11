CREATE TABLE IF NOT EXISTS cita (
    id                  BIGSERIAL    PRIMARY KEY,
    codigo              VARCHAR(20)  NOT NULL,
    paciente_id         BIGINT       NOT NULL,
    especialidad        VARCHAR(60)  NOT NULL,
    fecha_hora          TIMESTAMP    NOT NULL,
    profesional_codigo  VARCHAR(20)  NOT NULL,
    motivo              VARCHAR(200),
    estado              VARCHAR(12)  NOT NULL,
    fecha_creacion      TIMESTAMP    NOT NULL,
    fecha_actualizacion TIMESTAMP    NOT NULL,
    CONSTRAINT uk_cita_codigo UNIQUE (codigo),
    CONSTRAINT ck_cita_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'ATENDIDA', 'CANCELADA', 'AUSENTE'))
);

CREATE INDEX IF NOT EXISTS idx_cita_paciente ON cita (paciente_id);
CREATE INDEX IF NOT EXISTS idx_cita_estado ON cita (estado);

CREATE UNIQUE INDEX IF NOT EXISTS uk_cita_profesional_horario_activa
    ON cita (profesional_codigo, fecha_hora) WHERE estado IN ('PENDIENTE', 'CONFIRMADA');
CREATE UNIQUE INDEX IF NOT EXISTS uk_cita_paciente_horario_activa
    ON cita (paciente_id, fecha_hora) WHERE estado IN ('PENDIENTE', 'CONFIRMADA');
