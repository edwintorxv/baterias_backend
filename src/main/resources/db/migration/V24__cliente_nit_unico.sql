-- El NIT identifica al cliente: hasta ahora solo lo validaba ClienteService (existsByNit).
-- Con la restricción, dos altas simultáneas con el mismo NIT también terminan en 409
-- (DataIntegrityViolationException en GlobalExceptionHandler).
-- Verificado antes de aplicar: sin NIT repetidos en la BD local (4 clientes) ni en Supabase (0).
ALTER TABLE cliente ADD CONSTRAINT uk_cliente_nit UNIQUE (nit);
