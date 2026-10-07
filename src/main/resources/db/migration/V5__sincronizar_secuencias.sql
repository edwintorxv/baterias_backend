-- =====================================================================
-- V5: sincroniza las secuencias de id con el MAX(id) de cada tabla.
-- Los datos maestros y de prueba se cargaron con ids explícitos, lo que dejó
-- secuencias atrasadas (aplicacion, evaluado_cliente, pregunta,
-- baremo_dominio, sector_economico) y hacía fallar los INSERT por la API con
-- llave duplicada. Solo adelanta secuencias; nunca las retrocede.
-- =====================================================================
DO $$
DECLARE
    tabla     TEXT;
    secuencia TEXT;
    maximo    BIGINT;
BEGIN
    FOR tabla IN
        SELECT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND c.column_name = 'id'
          AND t.table_type = 'BASE TABLE'
    LOOP
        secuencia := pg_get_serial_sequence(format('public.%I', tabla), 'id');
        CONTINUE WHEN secuencia IS NULL;

        EXECUTE format('SELECT MAX(id) FROM public.%I', tabla) INTO maximo;
        CONTINUE WHEN maximo IS NULL;

        EXECUTE format(
            'SELECT setval(%L, GREATEST(%s, (SELECT CASE WHEN is_called THEN last_value ELSE last_value - 1 END FROM %s)) + 1, false)',
            secuencia, maximo, secuencia);
    END LOOP;
END $$;
