-- =====================================================================
-- V2: soporte para el cálculo de los cuestionarios C (extralaboral) y D (estrés)
--   1. Grupo ocupacional: define qué juego de baremos aplica
--      (jefes/profesionales/técnicos vs auxiliares/operarios).
--   2. Método de cálculo, factor total y peso por dimensión.
--   3. Unicidad de resultados por aplicación.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Catálogo grupo_ocupacional
-- ---------------------------------------------------------------------
CREATE TABLE grupo_ocupacional (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO grupo_ocupacional (id, nombre) VALUES
    (1, 'Jefes, profesionales y técnicos'),
    (2, 'Auxiliares y operarios');

SELECT setval('grupo_ocupacional_id_seq', (SELECT MAX(id) FROM grupo_ocupacional));

-- tipo_cargo -> grupo_ocupacional
ALTER TABLE tipo_cargo ADD COLUMN fk_grupo_ocupacional BIGINT;

UPDATE tipo_cargo SET fk_grupo_ocupacional = 1 WHERE id IN (1, 2);
UPDATE tipo_cargo SET fk_grupo_ocupacional = 2 WHERE id IN (3, 4);

ALTER TABLE tipo_cargo
    ALTER COLUMN fk_grupo_ocupacional SET NOT NULL,
    ADD CONSTRAINT fk_tipo_cargo_grupo_ocupacional
        FOREIGN KEY (fk_grupo_ocupacional) REFERENCES grupo_ocupacional(id);

-- aplicacion -> grupo_ocupacional (foto del grupo al momento de aplicar)
ALTER TABLE aplicacion ADD COLUMN fk_grupo_ocupacional BIGINT;

UPDATE aplicacion a
SET fk_grupo_ocupacional = tc.fk_grupo_ocupacional
FROM evaluado_cliente ec
JOIN tipo_cargo tc ON tc.id = ec.fk_tipo_cargo
WHERE ec.id = a.fk_evaluado_cliente;

ALTER TABLE aplicacion
    ALTER COLUMN fk_grupo_ocupacional SET NOT NULL,
    ADD CONSTRAINT fk_aplicacion_grupo_ocupacional
        FOREIGN KEY (fk_grupo_ocupacional) REFERENCES grupo_ocupacional(id);

-- ---------------------------------------------------------------------
-- 2. Baremos por grupo ocupacional
--    Los existentes se asignan así: forma A -> grupo 1, forma B -> grupo 2,
--    forma C (extralaboral) -> grupo 1 (los valores cargados corresponden a
--    la tabla de jefes, profesionales y técnicos; VERIFICAR contra el manual).
-- ---------------------------------------------------------------------
ALTER TABLE baremo_dimension    ADD COLUMN fk_grupo_ocupacional BIGINT;
ALTER TABLE baremo_dominio      ADD COLUMN fk_grupo_ocupacional BIGINT;
ALTER TABLE baremo_cuestionario ADD COLUMN fk_grupo_ocupacional BIGINT;

UPDATE baremo_dimension b
SET fk_grupo_ocupacional = CASE c.forma WHEN 'B' THEN 2 ELSE 1 END
FROM dimension_cuestionario dc
JOIN cuestionario c ON c.id = dc.fk_cuestionario
WHERE dc.id = b.fk_dimension_cuestionario;

UPDATE baremo_dominio b
SET fk_grupo_ocupacional = CASE c.forma WHEN 'B' THEN 2 ELSE 1 END
FROM dominio_cuestionario dc
JOIN cuestionario c ON c.id = dc.fk_cuestionario
WHERE dc.id = b.fk_dominio_cuestionario;

UPDATE baremo_cuestionario b
SET fk_grupo_ocupacional = CASE c.forma WHEN 'B' THEN 2 ELSE 1 END
FROM cuestionario c
WHERE c.id = b.fk_cuestionario;

ALTER TABLE baremo_dimension
    ALTER COLUMN fk_grupo_ocupacional SET NOT NULL,
    ADD CONSTRAINT fk_baremo_dimension_grupo
        FOREIGN KEY (fk_grupo_ocupacional) REFERENCES grupo_ocupacional(id);

ALTER TABLE baremo_dominio
    ALTER COLUMN fk_grupo_ocupacional SET NOT NULL,
    ADD CONSTRAINT fk_baremo_dominio_grupo
        FOREIGN KEY (fk_grupo_ocupacional) REFERENCES grupo_ocupacional(id);

ALTER TABLE baremo_cuestionario
    ALTER COLUMN fk_grupo_ocupacional SET NOT NULL,
    ADD CONSTRAINT fk_baremo_cuestionario_grupo
        FOREIGN KEY (fk_grupo_ocupacional) REFERENCES grupo_ocupacional(id);

-- ---------------------------------------------------------------------
-- 3. Método de cálculo y factor total por cuestionario
-- ---------------------------------------------------------------------
ALTER TABLE cuestionario
    ADD COLUMN factor_transformacion NUMERIC(10,2),
    ADD COLUMN metodo_calculo VARCHAR(30);

UPDATE cuestionario SET factor_transformacion = 492.00, metodo_calculo = 'SUMA_POR_DOMINIOS'  WHERE forma = 'A';
UPDATE cuestionario SET factor_transformacion = 388.00, metodo_calculo = 'SUMA_POR_DOMINIOS'  WHERE forma = 'B';
UPDATE cuestionario SET factor_transformacion = 124.00, metodo_calculo = 'SUMA_DIRECTA'       WHERE forma = 'C';
UPDATE cuestionario SET factor_transformacion = 61.16,  metodo_calculo = 'PROMEDIO_PONDERADO' WHERE forma = 'D';

ALTER TABLE cuestionario
    ALTER COLUMN factor_transformacion SET NOT NULL,
    ALTER COLUMN metodo_calculo SET NOT NULL,
    ADD CONSTRAINT ck_cuestionario_metodo_calculo
        CHECK (metodo_calculo IN ('SUMA_POR_DOMINIOS', 'SUMA_DIRECTA', 'PROMEDIO_PONDERADO'));

-- Peso de la dimensión (solo aplica al método PROMEDIO_PONDERADO, cuestionario D)
ALTER TABLE dimension_cuestionario ADD COLUMN peso NUMERIC(5,2);

UPDATE dimension_cuestionario dc
SET peso = CASE dc.fk_dimension
               WHEN 27 THEN 4   -- Síntomas fisiológicos (ítems 1-8)
               WHEN 28 THEN 3   -- Síntomas de comportamiento social (ítems 9-12)
               WHEN 29 THEN 2   -- Síntomas intelectuales y laborales (ítems 13-22)
               WHEN 30 THEN 1   -- Síntomas psicoemocionales (ítems 23-31)
           END
FROM cuestionario c
WHERE c.id = dc.fk_cuestionario
  AND c.forma = 'D';

-- ---------------------------------------------------------------------
-- 4. Unicidad de resultados por aplicación
-- ---------------------------------------------------------------------
ALTER TABLE resultado_dominio
    ADD CONSTRAINT uk_resultado_dominio UNIQUE (fk_aplicacion, fk_dominio_cuestionario);

ALTER TABLE resultado_cuestionario
    ADD CONSTRAINT uk_resultado_cuestionario UNIQUE (fk_aplicacion, fk_cuestionario);
