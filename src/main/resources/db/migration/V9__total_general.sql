-- =====================================================================
-- V9: puntaje total general de factores de riesgo psicosocial
--     (intralaboral A o B + extralaboral C)
--   Bruto = bruto total intralaboral + bruto total extralaboral.
--   Factor = suma de los factores de ambos cuestionarios (616 = 492 + 124 para A,
--   512 = 388 + 124 para B; Tabla 15 del manual extralaboral), por eso no se guarda.
--   El baremo depende solo de la forma intralaboral (Tabla 34), no del grupo
--   ocupacional: la forma A o B ya lo distingue.
-- =====================================================================

CREATE TABLE baremo_total_general (
    id BIGSERIAL PRIMARY KEY,
    fk_cuestionario_intralaboral BIGINT NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    valor_minimo NUMERIC(5,2) NOT NULL,
    valor_maximo NUMERIC(5,2) NOT NULL,
    CONSTRAINT fk_baremo_total_general_cuestionario
        FOREIGN KEY (fk_cuestionario_intralaboral) REFERENCES cuestionario(id),
    CONSTRAINT fk_baremo_total_general_nivel
        FOREIGN KEY (fk_nivel_riesgo) REFERENCES nivel_riesgo(id),
    CONSTRAINT uk_baremo_total_general
        UNIQUE (fk_cuestionario_intralaboral, fk_nivel_riesgo)
);

CREATE TABLE resultado_total_general (
    id BIGSERIAL PRIMARY KEY,
    fk_aplicacion BIGINT NOT NULL,
    fk_cuestionario_intralaboral BIGINT NOT NULL,
    puntaje_bruto NUMERIC(10,2) NOT NULL,
    puntaje_transformado NUMERIC(10,2) NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    fecha_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resultado_total_general_aplicacion
        FOREIGN KEY (fk_aplicacion) REFERENCES aplicacion(id),
    CONSTRAINT fk_resultado_total_general_cuestionario
        FOREIGN KEY (fk_cuestionario_intralaboral) REFERENCES cuestionario(id),
    CONSTRAINT fk_resultado_total_general_nivel
        FOREIGN KEY (fk_nivel_riesgo) REFERENCES nivel_riesgo(id),
    CONSTRAINT uk_resultado_total_general
        UNIQUE (fk_aplicacion)
);

-- Tabla 34. Baremos para el puntaje total general (formas A y B).
-- Sobre una BD sin datos maestros no inserta nada (JOIN con cuestionario).
INSERT INTO baremo_total_general (fk_cuestionario_intralaboral, fk_nivel_riesgo, valor_minimo, valor_maximo)
SELECT c.id, v.nivel, v.minimo, v.maximo
FROM cuestionario c
JOIN (VALUES
    -- Forma A + extralaboral
    (1, 1,  0.0,  18.8),
    (1, 2, 18.9,  24.4),
    (1, 3, 24.5,  29.5),
    (1, 4, 29.6,  35.4),
    (1, 5, 35.5, 100.0),
    -- Forma B + extralaboral
    (2, 1,  0.0,  19.9),
    (2, 2, 20.0,  24.8),
    (2, 3, 24.9,  29.5),
    (2, 4, 29.6,  35.4),
    (2, 5, 35.5, 100.0)
) AS v (cuestionario, nivel, minimo, maximo) ON v.cuestionario = c.id;
