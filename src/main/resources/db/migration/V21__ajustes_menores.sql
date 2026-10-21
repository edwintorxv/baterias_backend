-- =====================================================================
-- V21: ajustes menores (2026-10-21)
--   1. aplicacion.estado era texto libre ("FINALIZADA", "En proceso") y ya
--      no se usa: el estado se calcula de las respuestas y los resultados
--      (EstadoAplicacion: sin respuestas, incompleta, pendiente de cálculo,
--      calculada).
--   2. Tilde en el tiempo en el cargo: "Más de un año".
-- =====================================================================

ALTER TABLE aplicacion DROP COLUMN estado;

UPDATE tiempo_cargo SET nombre = 'Más de un año' WHERE nombre = 'Mas de un año';
