-- =====================================================================
-- V13: conclusiones y plan de acción del informe de empresa
--   Una fila por cliente y año, escrita por el profesional. El Word del
--   informe la muestra (si no hay, deja los recuadros en blanco) y la API
--   la expone; antes solo se escribía a mano en el Word y se perdía al
--   volver a descargarlo.
--   fk_evaluador: profesional responsable del informe (nombre, tarjeta,
--   licencia y firma salen de evaluador). Opcional.
-- =====================================================================

CREATE TABLE informe_empresa (
    id                      BIGSERIAL PRIMARY KEY,
    fk_cliente              BIGINT  NOT NULL,
    anio                    INTEGER NOT NULL,
    conclusiones            TEXT,
    acciones_corto_plazo    TEXT,
    acciones_mediano_plazo  TEXT,
    acciones_largo_plazo    TEXT,
    fk_evaluador            BIGINT,
    fecha_creado            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificado        TIMESTAMP,
    usuario_crea            VARCHAR(100),
    usuario_modifica        VARCHAR(100),
    CONSTRAINT fk_informe_empresa_cliente FOREIGN KEY (fk_cliente) REFERENCES cliente(id),
    CONSTRAINT fk_informe_empresa_evaluador FOREIGN KEY (fk_evaluador) REFERENCES evaluador(id),
    CONSTRAINT uk_informe_empresa_cliente_anio UNIQUE (fk_cliente, anio)
);
