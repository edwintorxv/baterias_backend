-- =====================================================================
-- V6: correcciones en dimension_cuestionario detectadas al comparar con el
-- manual intralaboral (Tablas 23 y 25)
--   1. Forma A, "Retroalimentación del desempeño" (ítems 90-94): el factor de
--      transformación es 20, no 28 (28 corresponde a "Claridad de rol").
--   2. Forma B: la fila con los ítems 74-78 debe apuntar a "Retroalimentación
--      del desempeño" (dimensión 3), no a "Relación con los colaboradores"
--      (dimensión 4), que no aplica a la forma B. Sus ítems, factor (20) y
--      baremos ya corresponden a retroalimentación (Tabla 30).
-- Sobre una BD sin datos maestros no modifica nada.
-- =====================================================================

UPDATE dimension_cuestionario
SET factor_transformacion = 20
WHERE fk_cuestionario = 1
  AND fk_dimension = 3;

UPDATE dimension_cuestionario
SET fk_dimension = 3
WHERE fk_cuestionario = 2
  AND fk_dimension = 4;
