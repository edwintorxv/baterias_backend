-- =====================================================================
-- V7: preguntas filtro de las formas A y B, guardadas como foto de la aplicación
--   atiende_clientes: "En mi trabajo debo brindar servicio a clientes o usuarios"
--                     (antecede a los ítems 106-114 de A y 89-97 de B).
--   es_jefe:          "Soy jefe de otras personas en mi trabajo"
--                     (antecede a los ítems 115-123 de A).
-- NULL = no registrado: el motor sigue exigiendo todas las respuestas.
-- FALSE = no aplica: la dimensión correspondiente vale puntaje bruto 0.
-- =====================================================================

ALTER TABLE aplicacion
    ADD COLUMN atiende_clientes BOOLEAN,
    ADD COLUMN es_jefe          BOOLEAN;
