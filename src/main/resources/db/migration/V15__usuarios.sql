-- =====================================================================
-- V15: usuarios de la API y clientes asignados a cada psicólogo
--   Roles:
--     ADMIN      usuarios, asignaciones y configuración; no ve informes.
--     PSICOLOGO  trabaja sobre sus clientes asignados (usuario_cliente);
--                ligado a su evaluador (datos y firma de los informes).
--     CLIENTE    ve el informe de SU empresa (fk_cliente); varios por empresa.
--     EVALUADO   previsto para cuando el trabajador responda en línea
--                (fk_evaluado); por ahora sin permisos.
--   Cada rol se liga solo a lo suyo (ck_usuario_vinculo). Sin DELETE: un
--   usuario se retira con activo = FALSE.
--   contrasena_hash: BCrypt, nunca la contraseña.
--   usuario: único sin distinguir mayúsculas (índice sobre LOWER).
--   usuario_cliente solo debe tener psicólogos: lo valida el servicio
--   (un CHECK no puede mirar otra tabla).
-- =====================================================================

CREATE TABLE usuario (
    id                BIGSERIAL PRIMARY KEY,
    usuario           VARCHAR(100) NOT NULL,
    contrasena_hash   VARCHAR(100) NOT NULL,
    nombre            VARCHAR(200) NOT NULL,
    rol               VARCHAR(20)  NOT NULL,
    fk_evaluador      BIGINT,
    fk_cliente        BIGINT,
    fk_evaluado       BIGINT,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creado      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificado  TIMESTAMP,
    usuario_crea      VARCHAR(100),
    usuario_modifica  VARCHAR(100),
    CONSTRAINT fk_usuario_evaluador FOREIGN KEY (fk_evaluador) REFERENCES evaluador(id),
    CONSTRAINT fk_usuario_cliente   FOREIGN KEY (fk_cliente)   REFERENCES cliente(id),
    CONSTRAINT fk_usuario_evaluado  FOREIGN KEY (fk_evaluado)  REFERENCES evaluado(id),
    CONSTRAINT uk_usuario_evaluador UNIQUE (fk_evaluador),
    CONSTRAINT uk_usuario_evaluado  UNIQUE (fk_evaluado),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN', 'PSICOLOGO', 'CLIENTE', 'EVALUADO')),
    CONSTRAINT ck_usuario_vinculo CHECK (
        (rol = 'ADMIN'     AND fk_evaluador IS NULL     AND fk_cliente IS NULL     AND fk_evaluado IS NULL) OR
        (rol = 'PSICOLOGO' AND fk_evaluador IS NOT NULL AND fk_cliente IS NULL     AND fk_evaluado IS NULL) OR
        (rol = 'CLIENTE'   AND fk_evaluador IS NULL     AND fk_cliente IS NOT NULL AND fk_evaluado IS NULL) OR
        (rol = 'EVALUADO'  AND fk_evaluador IS NULL     AND fk_cliente IS NULL     AND fk_evaluado IS NOT NULL))
);

CREATE UNIQUE INDEX uk_usuario_usuario ON usuario (LOWER(usuario));

CREATE TABLE usuario_cliente (
    fk_usuario    BIGINT NOT NULL,
    fk_cliente    BIGINT NOT NULL,
    fecha_creado  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_crea  VARCHAR(100),
    CONSTRAINT pk_usuario_cliente PRIMARY KEY (fk_usuario, fk_cliente),
    CONSTRAINT fk_usuario_cliente_usuario FOREIGN KEY (fk_usuario) REFERENCES usuario(id),
    CONSTRAINT fk_usuario_cliente_cliente FOREIGN KEY (fk_cliente) REFERENCES cliente(id)
);

-- Para filtrar rápido "los psicólogos de este cliente" (la PK ya cubre por usuario).
CREATE INDEX ix_usuario_cliente_cliente ON usuario_cliente (fk_cliente);
