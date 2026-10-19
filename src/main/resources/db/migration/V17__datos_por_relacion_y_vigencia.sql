-- =====================================================================
-- V17: datos que cambian, por relación; vigencia de la relación
--   1. Los datos sociodemográficos que cambian con el tiempo (estado civil,
--      nivel académico, residencia, estrato, vivienda, ocupación y
--      dependientes) pasan a evaluado_cliente: una foto por vinculación,
--      igual que cargo, área y ciudad de trabajo. Si la persona cambia de
--      empresa, la relación anterior conserva los datos con que se evaluó.
--      En evaluado quedan los que no cambian (documento, nombre, apellido,
--      sexo, año de nacimiento).
--      Las columnas viejas de evaluado NO se borran: la app ya no las usa,
--      pero las personas sin relación (datos de prueba) conservan los suyos.
--      Se borrarán en una migración posterior.
--   2. fecha_vinculacion / fecha_retiro: el informe de empresa de un año
--      cuenta a quien se evaluó ese año (aunque hoy esté inactivo) y espera
--      a quien estaba vigente al corte del año.
-- =====================================================================

ALTER TABLE evaluado_cliente
    ADD COLUMN fk_estado_civil                       BIGINT,
    ADD COLUMN fk_nivel_academico                    BIGINT,
    ADD COLUMN fk_ciudad_residencia                  BIGINT,
    ADD COLUMN fk_estrato_socioeconomico             BIGINT,
    ADD COLUMN fk_tipo_vivienda                      BIGINT,
    ADD COLUMN ocupacion_profesion                   VARCHAR(200),
    ADD COLUMN familiares_dependientes_economicamente INTEGER,
    ADD COLUMN fecha_vinculacion                     DATE,
    ADD COLUMN fecha_retiro                          DATE,
    ADD CONSTRAINT fk_eval_cliente_estado_civil FOREIGN KEY (fk_estado_civil) REFERENCES estado_civil(id),
    ADD CONSTRAINT fk_eval_cliente_nivel_academico FOREIGN KEY (fk_nivel_academico) REFERENCES nivel_academico(id),
    ADD CONSTRAINT fk_eval_cliente_ciudad_residencia FOREIGN KEY (fk_ciudad_residencia) REFERENCES ciudad_municipio(id),
    ADD CONSTRAINT fk_eval_cliente_estrato FOREIGN KEY (fk_estrato_socioeconomico) REFERENCES estrato_socioeconomico(id),
    ADD CONSTRAINT fk_eval_cliente_tipo_vivienda FOREIGN KEY (fk_tipo_vivienda) REFERENCES tipo_vivienda(id);

-- Foto inicial: los datos actuales de la persona en cada una de sus relaciones.
UPDATE evaluado_cliente ec
SET fk_estado_civil                        = e.fk_estado_civil,
    fk_nivel_academico                     = e.fk_nivel_academico,
    fk_ciudad_residencia                   = e.fk_ciudad_residencia,
    fk_estrato_socioeconomico              = e.fk_estrato_socioeconomico,
    fk_tipo_vivienda                       = e.fk_tipo_vivienda,
    ocupacion_profesion                    = e.ocupacion_profesion,
    familiares_dependientes_economicamente = e.familiares_dependientes_economicamente
FROM evaluado e
WHERE e.id = ec.fk_evaluado;

-- Vigencia: se vinculó cuando se creó el registro; las inactivas se retiraron en su última modificación.
UPDATE evaluado_cliente
SET fecha_vinculacion = COALESCE(fecha_creado, CURRENT_TIMESTAMP)::date,
    fecha_retiro      = CASE WHEN activo THEN NULL ELSE COALESCE(fecha_modificado, fecha_creado, CURRENT_TIMESTAMP)::date END;

ALTER TABLE evaluado_cliente
    ALTER COLUMN fecha_vinculacion SET DEFAULT CURRENT_DATE,
    ALTER COLUMN fecha_vinculacion SET NOT NULL,
    ADD CONSTRAINT ck_eval_cliente_vigencia CHECK (fecha_retiro IS NULL OR fecha_retiro >= fecha_vinculacion);

COMMENT ON COLUMN evaluado_cliente.fecha_retiro IS 'NULL = vigente; se llena al inactivar la relación';
