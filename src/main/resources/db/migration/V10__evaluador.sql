-- =====================================================================
-- V10: evaluador (psicólogo) y textos del informe individual
--   Los formatos modelo de informe de la batería (Anexos 4 y 5 del
--   manual intralaboral, Anexo 2 del extralaboral) exigen los datos del
--   evaluador: "Todo informe que carezca de estos datos no será válido".
--   Sin DELETE: un evaluador se retira con activo = FALSE y sus informes
--   históricos se siguen generando.
--   aplicacion.fk_evaluador es nullable por las aplicaciones existentes;
--   el informe en archivo lo exige.
--   aplicacion.recomendaciones acompaña a observaciones (ya existe): un
--   juego por aplicación, para el documento unificado.
-- =====================================================================

CREATE TABLE evaluador (
    id                          BIGSERIAL PRIMARY KEY,
    numero_identificacion       VARCHAR(30)  NOT NULL,
    nombre                      VARCHAR(200) NOT NULL,
    profesion                   VARCHAR(200) NOT NULL,
    posgrado                    VARCHAR(200),
    tarjeta_profesional         VARCHAR(50)  NOT NULL,
    licencia_salud_ocupacional  VARCHAR(50)  NOT NULL,
    fecha_expedicion_licencia   DATE         NOT NULL,
    firma                       BYTEA,
    firma_tipo_contenido        VARCHAR(50),
    activo                      BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creado                TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificado            TIMESTAMP,
    usuario_crea                VARCHAR(100),
    usuario_modifica            VARCHAR(100),
    CONSTRAINT uk_evaluador_documento UNIQUE (numero_identificacion)
);

ALTER TABLE aplicacion
    ADD COLUMN fk_evaluador    BIGINT,
    ADD COLUMN recomendaciones TEXT,
    ADD CONSTRAINT fk_aplicacion_evaluador
        FOREIGN KEY (fk_evaluador) REFERENCES evaluador(id);
