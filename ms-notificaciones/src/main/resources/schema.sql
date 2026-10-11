CREATE TABLE IF NOT EXISTS notificacion (
    id             BIGSERIAL    PRIMARY KEY,
    evento_id      VARCHAR(40)  NOT NULL,
    tipo_evento    VARCHAR(60)  NOT NULL,
    paciente_id    BIGINT,
    canal          VARCHAR(20)  NOT NULL,
    mensaje        VARCHAR(500) NOT NULL,
    estado         VARCHAR(20)  NOT NULL,
    fecha_creacion TIMESTAMP    NOT NULL,
    CONSTRAINT uk_notificacion_evento UNIQUE (evento_id)
);

CREATE INDEX IF NOT EXISTS idx_notificacion_paciente ON notificacion (paciente_id);
