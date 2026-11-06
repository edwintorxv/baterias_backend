-- =====================================================================
-- V23: enlaces para definir la contraseña y cierre de sesiones
--   El administrador nunca ve ni escribe contraseñas: genera un enlace que
--   llega por correo al usuario (INVITACION al crearlo, RESTABLECER cuando
--   la olvida y llama por teléfono). Al generarlo, la contraseña actual deja
--   de servir (contrasena_hash = NULL), se cierran sus sesiones
--   (sesiones_desde) y se anulan sus enlaces anteriores sin usar.
--   codigo_hash: SHA-256 (hex) del código aleatorio; el código en claro solo
--   va en el correo. Vence a las 48 h y es de un solo uso (fecha_uso).
--   sesiones_desde: los tokens con iat anterior se rechazan (401).
-- =====================================================================

ALTER TABLE usuario ALTER COLUMN contrasena_hash DROP NOT NULL;
ALTER TABLE usuario ADD COLUMN sesiones_desde TIMESTAMP;

CREATE TABLE enlace_contrasena (
    id                BIGSERIAL PRIMARY KEY,
    fk_usuario        BIGINT      NOT NULL,
    codigo_hash       VARCHAR(64) NOT NULL,
    proposito         VARCHAR(20) NOT NULL,
    vence             TIMESTAMP   NOT NULL,
    fecha_uso         TIMESTAMP,
    anulado           BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_creado      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificado  TIMESTAMP,
    usuario_crea      VARCHAR(100),
    usuario_modifica  VARCHAR(100),
    CONSTRAINT fk_enlace_contrasena_usuario FOREIGN KEY (fk_usuario) REFERENCES usuario(id),
    CONSTRAINT uk_enlace_contrasena_codigo UNIQUE (codigo_hash),
    CONSTRAINT ck_enlace_contrasena_proposito CHECK (proposito IN ('INVITACION', 'RESTABLECER'))
);

-- Para anular rápido los enlaces sin usar de un usuario.
CREATE INDEX ix_enlace_contrasena_usuario ON enlace_contrasena (fk_usuario);
