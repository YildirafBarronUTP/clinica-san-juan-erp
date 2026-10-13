-- Sprint 0/1: Seguridad, Auditoría, Pacientes y Farmacia.
-- Citas, Atención y Caja llegan en migraciones posteriores (V3, V4, ...).

-- ================= Seguridad =================
CREATE TABLE usuario (
    id                BIGSERIAL PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL UNIQUE,
    password_hash     VARCHAR(100) NOT NULL,
    nombre_completo   VARCHAR(120) NOT NULL,
    rol               VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN','ADMISION','MEDICO','FARMACIA','CAJA')),
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    intentos_fallidos INT          NOT NULL DEFAULT 0,
    bloqueado_hasta   TIMESTAMPTZ,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE auditoria (
    id         BIGSERIAL PRIMARY KEY,
    usuario    VARCHAR(50)  NOT NULL,
    accion     VARCHAR(30)  NOT NULL,
    entidad    VARCHAR(40)  NOT NULL,
    entidad_id VARCHAR(40),
    detalle    VARCHAR(500),
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_auditoria_entidad_fecha ON auditoria (entidad, fecha);

-- ================= Pacientes =================
CREATE SEQUENCE historia_clinica_seq START 1;

CREATE TABLE paciente (
    id               BIGSERIAL PRIMARY KEY,
    historia_clinica VARCHAR(12)  NOT NULL UNIQUE,
    dni              VARCHAR(8)   NOT NULL UNIQUE CHECK (dni ~ '^[0-9]{8}$'),
    nombres          VARCHAR(80)  NOT NULL,
    apellidos        VARCHAR(80)  NOT NULL,
    fecha_nacimiento DATE         NOT NULL,
    sexo             VARCHAR(1)   NOT NULL CHECK (sexo IN ('M','F')),
    telefono         VARCHAR(9),
    email            VARCHAR(120),
    direccion        VARCHAR(200),
    estado           VARCHAR(12)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','INACTIVO','FALLECIDO')),
    creado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_paciente_apellidos ON paciente (lower(apellidos));

CREATE TABLE alergia (
    id               BIGSERIAL PRIMARY KEY,
    paciente_id      BIGINT       NOT NULL REFERENCES paciente(id) ON DELETE CASCADE,
    principio_activo VARCHAR(80)  NOT NULL,
    reaccion         VARCHAR(200),
    UNIQUE (paciente_id, principio_activo)
);

-- ================= Farmacia e Inventario =================
CREATE TABLE producto (
    id               BIGSERIAL PRIMARY KEY,
    codigo           VARCHAR(20)   NOT NULL UNIQUE,
    nombre           VARCHAR(120)  NOT NULL,
    principio_activo VARCHAR(80)   NOT NULL,
    presentacion     VARCHAR(60)   NOT NULL,
    precio_venta     NUMERIC(10,2) NOT NULL CHECK (precio_venta > 0),
    costo_promedio   NUMERIC(12,4) NOT NULL DEFAULT 0,
    stock_minimo     INT           NOT NULL CHECK (stock_minimo >= 0),
    activo           BOOLEAN       NOT NULL DEFAULT TRUE,
    version          BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE lote (
    id               BIGSERIAL PRIMARY KEY,
    producto_id      BIGINT        NOT NULL REFERENCES producto(id),
    numero_lote      VARCHAR(30)   NOT NULL,
    fecha_vencimiento DATE         NOT NULL,
    cantidad_inicial INT           NOT NULL CHECK (cantidad_inicial > 0),
    cantidad_actual  INT           NOT NULL CHECK (cantidad_actual >= 0),
    costo_unitario   NUMERIC(10,4) NOT NULL CHECK (costo_unitario > 0),
    fecha_ingreso    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version          BIGINT        NOT NULL DEFAULT 0,
    UNIQUE (producto_id, numero_lote)
);
CREATE INDEX idx_lote_producto_venc ON lote (producto_id, fecha_vencimiento);

CREATE TABLE movimiento_kardex (
    id             BIGSERIAL PRIMARY KEY,
    producto_id    BIGINT        NOT NULL REFERENCES producto(id),
    lote_id        BIGINT        REFERENCES lote(id),
    tipo           VARCHAR(10)   NOT NULL CHECK (tipo IN ('ENTRADA','SALIDA','AJUSTE')),
    cantidad       INT           NOT NULL CHECK (cantidad > 0),
    saldo          INT           NOT NULL CHECK (saldo >= 0),
    costo_unitario NUMERIC(12,4) NOT NULL,
    referencia     VARCHAR(120)  NOT NULL,
    usuario        VARCHAR(50)   NOT NULL,
    fecha          TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_kardex_producto_fecha ON movimiento_kardex (producto_id, fecha, id);
