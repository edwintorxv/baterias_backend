-- Una sola respuesta por pregunta en cada aplicación: al digitar en bloque, guardar de nuevo un
-- cuestionario reemplaza sus respuestas en vez de duplicarlas.
ALTER TABLE respuesta
    ADD CONSTRAINT uk_respuesta_aplicacion_pregunta UNIQUE (fk_aplicacion, fk_pregunta);
