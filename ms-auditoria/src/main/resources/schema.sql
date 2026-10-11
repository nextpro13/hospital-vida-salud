CREATE TABLE IF NOT EXISTS registro_auditoria (
    id              BIGSERIAL    PRIMARY KEY,
    evento_id       VARCHAR(40)  NOT NULL,
    tipo_evento     VARCHAR(60)  NOT NULL,
    recurso         VARCHAR(30)  NOT NULL,
    recurso_id      VARCHAR(40)  NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo    VARCHAR(20),
    origen          VARCHAR(40)  NOT NULL,
    fecha_evento    TIMESTAMP    NOT NULL,
    fecha_registro  TIMESTAMP    NOT NULL,
    detalle         TEXT,
    CONSTRAINT uk_auditoria_evento UNIQUE (evento_id)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_recurso ON registro_auditoria (recurso, recurso_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON registro_auditoria (fecha_evento);
