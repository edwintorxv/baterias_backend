-- =====================================================================
-- V3: correcciones de datos maestros detectadas al revisar contra el manual
--   1. Valores de las escalas 1 y 2 estaban invertidos (Tabla 21 forma A:
--      los ítems positivos de la escala 1 deben calificar Siempre=0 ... Nunca=4).
--   2. Recalcular respuesta.valor_obtenido, que se guardó con la escala invertida.
--   3. Forma C: los ítems 18, 19, 20, 21 y 23 pertenecen a "Comunicación y
--      relaciones interpersonales" (dimensión 22), no a "Relaciones familiares"
--      (dimensión 21), que solo agrupa 22, 25 y 27.
-- Sobre una BD sin datos maestros no modifica nada.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. escala_detalle (opción 1=Siempre ... 5=Nunca)
-- ---------------------------------------------------------------------
UPDATE escala_detalle
SET valor = fk_opcion_respuesta - 1
WHERE fk_escala = 1 AND fk_opcion_respuesta BETWEEN 1 AND 5;

UPDATE escala_detalle
SET valor = 5 - fk_opcion_respuesta
WHERE fk_escala = 2 AND fk_opcion_respuesta BETWEEN 1 AND 5;

-- ---------------------------------------------------------------------
-- 2. respuesta.valor_obtenido según la escala corregida
-- ---------------------------------------------------------------------
UPDATE respuesta r
SET valor_obtenido = ed.valor
FROM pregunta p
JOIN escala_detalle ed ON ed.fk_escala = p.fk_escala
WHERE p.id = r.fk_pregunta
  AND ed.fk_opcion_respuesta = r.fk_opcion_respuesta
  AND p.fk_escala IN (1, 2)
  AND r.valor_obtenido <> ed.valor;

-- ---------------------------------------------------------------------
-- 3. Forma C: ítems de comunicación a su dimensión
-- ---------------------------------------------------------------------
UPDATE pregunta p
SET fk_dimension_cuestionario = destino.id
FROM dimension_cuestionario origen,
     dimension_cuestionario destino
WHERE p.fk_dimension_cuestionario = origen.id
  AND origen.fk_cuestionario = 3 AND origen.fk_dimension = 21
  AND destino.fk_cuestionario = 3 AND destino.fk_dimension = 22
  AND p.numero IN (18, 19, 20, 21, 23);
