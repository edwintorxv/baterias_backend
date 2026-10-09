-- =====================================================================
-- V11: la dimensión 26 ("Desplazamiento vivienda – trabajo – vivienda")
-- tenía el guion guardado como U+0096 (carácter de control: un "–" de
-- Windows-1252 importado como Latin-1), que no se ve en el informe PDF.
-- Se revisaron todas las columnas de texto: es el único caso.
-- =====================================================================

UPDATE dimension
SET descripcion = replace(descripcion, U&'\0096', U&'\2013')
WHERE descripcion LIKE '%' || U&'\0096' || '%';
