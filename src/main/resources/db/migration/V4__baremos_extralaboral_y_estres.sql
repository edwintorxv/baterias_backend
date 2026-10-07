-- =====================================================================
-- V4: baremos faltantes de los cuestionarios C (extralaboral) y D (estrés)
--   1. Totales de C y D en baremo_cuestionario, para los dos grupos
--      ocupacionales (manual extralaboral Tablas 17 y 18; manual de estrés
--      Tabla 6). Reemplaza las filas de C cargadas a mano con los rangos de la
--      Tabla 34, que corresponden al total general (intra + extra), no a C.
--   2. Dimensiones de C para el grupo 2 (Tabla 18). Las del grupo 1 ya
--      estaban cargadas y coinciden con la Tabla 17.
-- Los niveles de estrés (muy bajo, bajo, medio, alto, muy alto) se mapean por
-- posición a nivel_riesgo 1..5.
-- Sobre una BD sin datos maestros no inserta nada (los INSERT hacen JOIN con
-- cuestionario / dimension_cuestionario).
-- =====================================================================

-- Los datos maestros se cargaron con ids explícitos y las secuencias quedaron
-- atrasadas; se sincronizan antes de insertar.
SELECT setval('baremo_cuestionario_id_seq', COALESCE((SELECT MAX(id) FROM baremo_cuestionario), 0) + 1, false);
SELECT setval('baremo_dimension_id_seq', COALESCE((SELECT MAX(id) FROM baremo_dimension), 0) + 1, false);

-- ---------------------------------------------------------------------
-- 1. Totales C y D
-- ---------------------------------------------------------------------
DELETE FROM baremo_cuestionario WHERE fk_cuestionario IN (3, 4);

INSERT INTO baremo_cuestionario (fk_cuestionario, fk_grupo_ocupacional, fk_nivel_riesgo, valor_minimo, valor_maximo)
SELECT c.id, v.grupo, v.nivel, v.minimo, v.maximo
FROM cuestionario c
JOIN (VALUES
    -- C, jefes/profesionales/técnicos (Tabla 17)
    (3, 1, 1,  0.0,  11.3),
    (3, 1, 2, 11.4,  16.9),
    (3, 1, 3, 17.0,  22.6),
    (3, 1, 4, 22.7,  29.0),
    (3, 1, 5, 29.1, 100.0),
    -- C, auxiliares/operarios (Tabla 18)
    (3, 2, 1,  0.0,  12.9),
    (3, 2, 2, 13.0,  17.7),
    (3, 2, 3, 17.8,  24.2),
    (3, 2, 4, 24.3,  32.3),
    (3, 2, 5, 32.4, 100.0),
    -- D, jefes/profesionales/técnicos (Tabla 6)
    (4, 1, 1,  0.0,   7.8),
    (4, 1, 2,  7.9,  12.6),
    (4, 1, 3, 12.7,  17.7),
    (4, 1, 4, 17.8,  25.0),
    (4, 1, 5, 25.1, 100.0),
    -- D, auxiliares/operarios (Tabla 6)
    (4, 2, 1,  0.0,   6.5),
    (4, 2, 2,  6.6,  11.8),
    (4, 2, 3, 11.9,  17.0),
    (4, 2, 4, 17.1,  23.4),
    (4, 2, 5, 23.5, 100.0)
) AS v (cuestionario, grupo, nivel, minimo, maximo) ON v.cuestionario = c.id;

-- ---------------------------------------------------------------------
-- 2. Dimensiones de C, auxiliares/operarios (Tabla 18)
-- ---------------------------------------------------------------------
DELETE FROM baremo_dimension bd
USING dimension_cuestionario dc
WHERE dc.id = bd.fk_dimension_cuestionario
  AND dc.fk_cuestionario = 3
  AND bd.fk_grupo_ocupacional = 2;

INSERT INTO baremo_dimension (fk_dimension_cuestionario, fk_grupo_ocupacional, fk_nivel_riesgo, valor_minimo, valor_maximo)
SELECT dc.id, 2, v.nivel, v.minimo, v.maximo
FROM dimension_cuestionario dc
JOIN (VALUES
    -- Tiempo fuera del trabajo (balance vida laboral y familiar)
    (20, 1,  0.0,   6.3), (20, 2,  6.4,  25.0), (20, 3, 25.1,  37.5), (20, 4, 37.6,  50.0), (20, 5, 50.1, 100.0),
    -- Relaciones familiares
    (21, 1,  0.0,   8.3), (21, 2,  8.4,  25.0), (21, 3, 25.1,  33.3), (21, 4, 33.4,  50.0), (21, 5, 50.1, 100.0),
    -- Comunicación y relaciones interpersonales
    (22, 1,  0.0,   5.0), (22, 2,  5.1,  15.0), (22, 3, 15.1,  25.0), (22, 4, 25.1,  35.0), (22, 5, 35.1, 100.0),
    -- Situación económica del grupo familiar
    (23, 1,  0.0,  16.7), (23, 2, 16.8,  25.0), (23, 3, 25.1,  41.7), (23, 4, 41.8,  50.0), (23, 5, 50.1, 100.0),
    -- Características de la vivienda y de su entorno
    (24, 1,  0.0,   5.6), (24, 2,  5.7,  11.1), (24, 3, 11.2,  16.7), (24, 4, 16.8,  27.8), (24, 5, 27.9, 100.0),
    -- Influencia del entorno extralaboral sobre el trabajo
    (25, 1,  0.0,   0.9), (25, 2,  1.0,  16.7), (25, 3, 16.8,  25.0), (25, 4, 25.1,  41.7), (25, 5, 41.8, 100.0),
    -- Desplazamiento vivienda – trabajo – vivienda
    (26, 1,  0.0,   0.9), (26, 2,  1.0,  12.5), (26, 3, 12.6,  25.0), (26, 4, 25.1,  43.8), (26, 5, 43.9, 100.0)
) AS v (dimension, nivel, minimo, maximo) ON v.dimension = dc.fk_dimension
WHERE dc.fk_cuestionario = 3;
