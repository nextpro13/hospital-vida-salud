CREATE TABLE IF NOT EXISTS paciente (
    id                  BIGSERIAL    PRIMARY KEY,
    tipo_documento      VARCHAR(10)  NOT NULL,
    numero_documento    VARCHAR(20)  NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    fecha_nacimiento    DATE         NOT NULL,
    telefono            VARCHAR(20)  NOT NULL,
    correo              VARCHAR(120) NOT NULL,
    direccion           VARCHAR(200),
    fecha_registro      TIMESTAMP    NOT NULL,
    fecha_actualizacion TIMESTAMP,
    estado              VARCHAR(10)  NOT NULL,
    CONSTRAINT uk_paciente_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_paciente_tipo_documento CHECK (tipo_documento IN ('DNI', 'CE', 'PASAPORTE')),
    CONSTRAINT ck_paciente_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX IF NOT EXISTS idx_paciente_apellidos ON paciente (apellidos);
