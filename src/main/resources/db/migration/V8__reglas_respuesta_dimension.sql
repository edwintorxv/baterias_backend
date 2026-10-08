-- =====================================================================
-- V8: reglas del manual intralaboral sobre ítems sin respuesta, por dimensión
--   condicion_aplicacion: pregunta filtro de la aplicación de la que depende la
--       dimensión. Si la aplicación la tiene en FALSE, la dimensión no se
--       responde y vale puntaje bruto 0.
--         ATIENDE_CLIENTES -> demandas emocionales (A 106-114, B 89-97)
--         ES_JEFE          -> relación con los colaboradores (A 115-123)
--   max_items_sin_respuesta: ítems que pueden quedar sin respuesta sin invalidar
--       la dimensión. El manual admite 1 en características del liderazgo,
--       relaciones sociales en el trabajo, relación con los colaboradores y
--       demandas ambientales y de esfuerzo físico; 0 en el resto.
-- =====================================================================

ALTER TABLE dimension_cuestionario
    ADD COLUMN condicion_aplicacion    VARCHAR(30),
    ADD COLUMN max_items_sin_respuesta SMALLINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_dimension_cuestionario_condicion
        CHECK (condicion_aplicacion IN ('ATIENDE_CLIENTES', 'ES_JEFE')),
    ADD CONSTRAINT ck_dimension_cuestionario_max_sin_respuesta
        CHECK (max_items_sin_respuesta >= 0);

-- Dimensión 11 = demandas emocionales (formas A y B)
UPDATE dimension_cuestionario
SET condicion_aplicacion = 'ATIENDE_CLIENTES'
WHERE fk_cuestionario IN (1, 2)
  AND fk_dimension = 11;

-- Dimensión 4 = relación con los colaboradores (solo forma A)
UPDATE dimension_cuestionario
SET condicion_aplicacion = 'ES_JEFE'
WHERE fk_cuestionario = 1
  AND fk_dimension = 4;

-- Dimensiones 1, 2, 4, 10 = liderazgo, relaciones sociales, colaboradores, demandas ambientales
UPDATE dimension_cuestionario
SET max_items_sin_respuesta = 1
WHERE fk_cuestionario IN (1, 2)
  AND fk_dimension IN (1, 2, 4, 10);
