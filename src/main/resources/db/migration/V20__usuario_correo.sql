-- =====================================================================
-- V20: se ingresa con el correo, no con un nombre de usuario
--   (decisión del usuario 2026-10-21: evita duplicidades como "admin2" o
--   "psicologa2" y deja listo el envío de correos: invitación, aviso de
--   cambio de contraseña y recuperación).
--   La columna usuario pasa a ser correo: en minúsculas, única y con
--   formato de correo. Los usuarios que ya existían sin "@" quedan con un
--   correo provisional <usuario>@baterias.local, que el administrador
--   reemplaza por el real desde Usuarios.
--   usuario_crea / usuario_modifica de todas las tablas siguen igual
--   (desde ahora guardan el correo de quien hace el cambio).
-- =====================================================================

ALTER TABLE usuario RENAME COLUMN usuario TO correo;

UPDATE usuario
SET correo = LOWER(TRIM(correo)) || CASE WHEN POSITION('@' IN correo) > 0 THEN '' ELSE '@baterias.local' END;

DROP INDEX uk_usuario_usuario;
CREATE UNIQUE INDEX uk_usuario_correo ON usuario (correo);

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_correo
    CHECK (correo = LOWER(correo) AND correo ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$');
