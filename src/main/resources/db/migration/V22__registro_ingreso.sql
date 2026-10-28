-- =====================================================================
-- V22: registro de los intentos de inicio de sesión
--   Una fila por intento: EXITOSO, FALLIDO (contraseña errada, usuario
--   inexistente o inactivo) o BLOQUEADO (solo el primer rechazo de cada
--   bloqueo por fuerza bruta, para que insistir no llene la tabla).
--   correo: el que se escribió, normalizado, aunque no exista.
--   fk_usuario: si el correo corresponde a un usuario.
--   ip: la que ve el backend (detrás del proxy de Render, la del cliente).
--   navegador: User-Agent recortado a 255 caracteres.
--   Nunca se guarda la contraseña. Retención: se borra lo que tenga más de
--   12 meses (app.seguridad.ingresos.retencion); la IP es dato personal.
-- =====================================================================

CREATE TABLE registro_ingreso (
    id          BIGSERIAL PRIMARY KEY,
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correo      VARCHAR(100) NOT NULL,
    fk_usuario  BIGINT,
    ip          VARCHAR(45)  NOT NULL,
    navegador   VARCHAR(255),
    resultado   VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_registro_ingreso_usuario FOREIGN KEY (fk_usuario) REFERENCES usuario(id),
    CONSTRAINT ck_registro_ingreso_resultado CHECK (resultado IN ('EXITOSO', 'FALLIDO', 'BLOQUEADO'))
);

-- Para la limpieza por antigüedad y las consultas por fecha.
CREATE INDEX ix_registro_ingreso_fecha ON registro_ingreso (fecha);
