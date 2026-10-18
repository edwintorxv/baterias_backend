-- Errata en el texto de las preguntas: al cargarlas, las comas quedaron como puntos
-- ("Cansancio. tedio o desgano." → "Cansancio, tedio o desgano."). Salen así en los informes.
-- Se filtra también por el texto con la errata para no tocar nada si ya se corrigió a mano
-- (y en una BD sin datos maestros no hace nada).
UPDATE pregunta p
SET descripcion = c.correcta
FROM (VALUES
          -- Forma A
          (10, 'En mi trabajo me preocupa estar expuesto a microbios. animales o plantas que afecten mi salud',
               'En mi trabajo me preocupa estar expuesto a microbios, animales o plantas que afecten mi salud'),
          (33, 'Mi trabajo me exige laborar en días de descanso. festivos o fines de semana',
               'Mi trabajo me exige laborar en días de descanso, festivos o fines de semana'),
          -- Forma B
          (133, 'En mi trabajo me preocupa estar expuesto a microbios. animales o plantas que afecten mi salud',
                'En mi trabajo me preocupa estar expuesto a microbios, animales o plantas que afecten mi salud'),
          (146, 'Mi trabajo me exige laborar en días de descanso. festivos o fines de semana',
                'Mi trabajo me exige laborar en días de descanso, festivos o fines de semana'),
          -- Estrés (D)
          (253, 'Problemas gastrointestinales. úlcera péptica. acidez. problemas digestivos o del colon.',
                'Problemas gastrointestinales, úlcera péptica, acidez, problemas digestivos o del colon.'),
          (259, 'Problemas relacionados con la función de los órganos genitales (impotencia. frigidez).',
                'Problemas relacionados con la función de los órganos genitales (impotencia, frigidez).'),
          (265, 'Dificultad para concentrarse. olvidos frecuentes.',
                'Dificultad para concentrarse, olvidos frecuentes.'),
          (267, 'Sentimiento de frustración. de no haber hecho lo que se quería en la vida.',
                'Sentimiento de frustración, de no haber hecho lo que se quería en la vida.'),
          (268, 'Cansancio. tedio o desgano.',
                'Cansancio, tedio o desgano.'),
          (275, 'Sentimiento de irritabilidad. actitudes y pensamientos negativos.',
                'Sentimiento de irritabilidad, actitudes y pensamientos negativos.'),
          (276, 'Sentimiento de angustia. preocupación o tristeza.',
                'Sentimiento de angustia, preocupación o tristeza.'),
          (278, 'Sentimientos de que "no vale nada". o " no sirve para nada".',
                'Sentimientos de que "no vale nada", o "no sirve para nada".'),
          (281, 'Comportamientos rígidos. obstinación o terquedad.',
                'Comportamientos rígidos, obstinación o terquedad.')
     ) AS c(id, errata, correcta)
WHERE p.id = c.id
  AND p.descripcion = c.errata;
