-- =====================================================================
-- V18: nombres de departamentos sin guion bajo
--   La V16 cargó tres departamentos con guion bajo (N_DE_SANTANDER,
--   VALLE_DEL_CAUCA, SAN_ANDRES), que salían así en la plantilla de
--   personas ("CALI – VALLE_DEL_CAUCA") y en los informes. Norte de
--   Santander queda con su nombre completo (DANE), sin tildes como los
--   demás. Idempotente: solo cambia los nombres viejos (en la BD local
--   ya se habían corregido a mano, salvo "N DE SANTANDER").
-- =====================================================================

UPDATE departamento SET nombre = 'NORTE DE SANTANDER'
WHERE id = 18 AND nombre IN ('N_DE_SANTANDER', 'N DE SANTANDER');

UPDATE departamento SET nombre = 'VALLE DEL CAUCA'
WHERE id = 24 AND nombre = 'VALLE_DEL_CAUCA';

UPDATE departamento SET nombre = 'SAN ANDRES'
WHERE id = 28 AND nombre = 'SAN_ANDRES';
