-- Errata en el catálogo de nivel académico: "Bachillerato in completo" → "Bachillerato incompleto".
-- Se filtra también por el nombre con la errata para no tocar nada si ya se corrigió a mano.
UPDATE nivel_academico
SET nombre = 'Bachillerato incompleto'
WHERE id = 4
  AND nombre = 'Bachillerato in completo';
