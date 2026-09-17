CREATE TABLE cuestionario (
    id BIGSERIAL PRIMARY KEY,
    forma VARCHAR(1) NOT NULL UNIQUE,
    descripcion VARCHAR(500) NOT NULL
);

CREATE TABLE dominio (
    id BIGSERIAL PRIMARY KEY,
    descripcion VARCHAR(300) NOT null,
    orden BIGINT
);


CREATE TABLE dimension (
    id BIGSERIAL PRIMARY KEY,
    fk_dominio BIGINT NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    CONSTRAINT fk_dimension_dominio
        FOREIGN KEY (fk_dominio)
        REFERENCES dominio(id)
);

CREATE TABLE opcion_respuesta (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE escala (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE escala_detalle (
    id BIGSERIAL PRIMARY KEY,
    fk_escala BIGINT NOT NULL,
    fk_opcion_respuesta BIGINT NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_escala_detalle_escala
        FOREIGN KEY (fk_escala)
        REFERENCES escala(id),
    CONSTRAINT fk_escala_detalle_opcion
        FOREIGN KEY (fk_opcion_respuesta)
        REFERENCES opcion_respuesta(id)
);

CREATE TABLE nivel_riesgo (
    id BIGSERIAL PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE dimension_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    fk_dimension BIGINT NOT NULL,
    fk_cuestionario BIGINT NOT NULL,
    factor_transformacion NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_dimension_cuestionario_dimension
        FOREIGN KEY (fk_dimension)
        REFERENCES dimension(id),
    CONSTRAINT fk_dimension_cuestionario_cuestionario
        FOREIGN KEY (fk_cuestionario)
        REFERENCES cuestionario(id),
    CONSTRAINT uk_dimension_cuestionario
        UNIQUE (fk_dimension, fk_cuestionario)
);

CREATE TABLE dominio_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    fk_dominio BIGINT NOT NULL,
    fk_cuestionario BIGINT NOT NULL,
    factor_transformacion NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_dominio_cuestionario_dominio
        FOREIGN KEY (fk_dominio)
        REFERENCES dominio(id),
    CONSTRAINT fk_dominio_cuestionario_cuestionario
        FOREIGN KEY (fk_cuestionario)
        REFERENCES cuestionario(id),
    CONSTRAINT uk_dominio_cuestionario
        UNIQUE (fk_dominio, fk_cuestionario)
);

CREATE TABLE baremo_dimension (
    id BIGSERIAL PRIMARY KEY,
    fk_dimension_cuestionario BIGINT NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    valor_minimo NUMERIC(5,2) NOT NULL,
    valor_maximo NUMERIC(5,2) NOT NULL,
    CONSTRAINT fk_baremo_dimension_dimension
        FOREIGN KEY (fk_dimension_cuestionario)
        REFERENCES dimension_cuestionario(id),
    CONSTRAINT fk_baremo_dimension_nivel
        FOREIGN KEY (fk_nivel_riesgo)
        REFERENCES nivel_riesgo(id)
);

CREATE TABLE baremo_dominio (
    id BIGSERIAL PRIMARY KEY,
    fk_dominio_cuestionario BIGINT NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    valor_minimo NUMERIC(5,2) NOT NULL,
    valor_maximo NUMERIC(5,2) NOT NULL,
    CONSTRAINT fk_baremo_dominio_dominio
        FOREIGN KEY (fk_dominio_cuestionario)
        REFERENCES dominio_cuestionario(id),
    CONSTRAINT fk_baremo_dominio_nivel
        FOREIGN KEY (fk_nivel_riesgo)
        REFERENCES nivel_riesgo(id)
);

CREATE TABLE baremo_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    fk_cuestionario BIGINT NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    valor_minimo NUMERIC(5,2) NOT NULL,
    valor_maximo NUMERIC(5,2) NOT NULL,
    CONSTRAINT fk_baremo_cuestionario
        FOREIGN KEY (fk_cuestionario)
        REFERENCES cuestionario(id),
    CONSTRAINT fk_baremo_cuestionario_nivel
        FOREIGN KEY (fk_nivel_riesgo)
        REFERENCES nivel_riesgo(id)
);

CREATE TABLE pregunta (
    id BIGSERIAL PRIMARY KEY,
    fk_dimension_cuestionario BIGINT NOT NULL,
    fk_escala BIGINT NOT NULL,
    numero INTEGER NOT NULL,
    descripcion TEXT NOT NULL,
    CONSTRAINT fk_pregunta_dimension_cuestionario
        FOREIGN KEY (fk_dimension_cuestionario)
        REFERENCES dimension_cuestionario (id),
    CONSTRAINT fk_pregunta_escala
        FOREIGN KEY (fk_escala)
        REFERENCES escala(id)
);

--tablas maestras evaluado y cliente

CREATE TABLE sexo (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
);


CREATE TABLE estado_civil (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE nivel_academico (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL
);

CREATE TABLE estrato_socioeconomico (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
);

CREATE TABLE tipo_vivienda (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE tipo_cargo (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL
);

CREATE TABLE tiempo_cargo (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE tipo_contrato (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL
);


CREATE TABLE horas_labor (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE tipo_salario (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

--Ubicacion geografica

CREATE TABLE departamento (
    id BIGSERIAL PRIMARY KEY,
    codigo_dane VARCHAR(10) UNIQUE,
    nombre VARCHAR(150) NOT NULL
);


CREATE TABLE ciudad_municipio (
    id BIGSERIAL PRIMARY KEY,
    fk_departamento BIGINT NOT NULL,
    codigo_dane VARCHAR(10) UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    CONSTRAINT fk_ciudad_departamento
        FOREIGN KEY (fk_departamento)
        REFERENCES departamento(id)
);

--Sectores Industria

CREATE TABLE sector_economico (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL
);

CREATE TABLE industria (
    id BIGSERIAL PRIMARY KEY,
    fk_sector_economico BIGINT NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    CONSTRAINT fk_industria_sector
        FOREIGN KEY (fk_sector_economico)
        REFERENCES sector_economico(id)
);

--Cliente

CREATE TABLE cliente (
    id BIGSERIAL PRIMARY KEY,
    fk_industria BIGINT NOT NULL,
    fk_ciudad_municipio BIGINT,
    nit VARCHAR(50) NOT NULL,
    nombre VARCHAR(300) NOT NULL,
    direccion VARCHAR(300),
    telefono VARCHAR(50),
    nombre_contacto VARCHAR(200),
    cargo_contacto VARCHAR(150),
    telefono_contacto VARCHAR(50),
    fecha_creado TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificado TIMESTAMP,
    usuario_crea VARCHAR(100),
    usuario_modifica VARCHAR(100),
    CONSTRAINT fk_cliente_industria
        FOREIGN KEY (fk_industria)
        REFERENCES industria(id),
    CONSTRAINT fk_cliente_ciudad
        FOREIGN KEY (fk_ciudad_municipio)
        REFERENCES ciudad_municipio(id)
);

--Evaluado


CREATE TABLE evaluado (
id BIGSERIAL PRIMARY KEY,
numero_identificacion VARCHAR(30) NOT NULL,
nombre VARCHAR(150) NOT NULL,
apellido VARCHAR(150) NOT NULL,
fk_sexo BIGINT,
anio_nacimiento INTEGER,
fk_estado_civil BIGINT,
fk_nivel_academico BIGINT,
fk_ciudad_residencia BIGINT,
fk_estrato_socioeconomico BIGINT,
fk_tipo_vivienda BIGINT,
ocupacion_profesion VARCHAR(200),
familiares_dependientes_economicamente INTEGER,
fecha_creado TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
fecha_modificado TIMESTAMP,
usuario_crea VARCHAR(100),
usuario_modifica VARCHAR(100),
CONSTRAINT uk_evaluado_documento UNIQUE(numero_identificacion),
CONSTRAINT fk_evaluado_sexo
	FOREIGN KEY (fk_sexo)
	REFERENCES sexo(id),
CONSTRAINT fk_evaluado_estado_civil
	FOREIGN KEY (fk_estado_civil)
	REFERENCES estado_civil(id),
CONSTRAINT fk_evaluado_nivel_academico
	FOREIGN KEY (fk_nivel_academico)
	REFERENCES nivel_academico(id),
CONSTRAINT fk_evaluado_ciudad_residencia
	FOREIGN KEY (fk_ciudad_residencia)
	REFERENCES ciudad_municipio(id),
CONSTRAINT fk_evaluado_estrato
	FOREIGN KEY (fk_estrato_socioeconomico)
	REFERENCES estrato_socioeconomico(id),
CONSTRAINT fk_evaluado_vivienda
	FOREIGN KEY (fk_tipo_vivienda)
	REFERENCES tipo_vivienda(id)
);


CREATE TABLE evaluado_cliente (
id BIGSERIAL PRIMARY KEY,
fk_evaluado BIGINT NOT NULL,
fk_cliente BIGINT NOT NULL,
fk_ciudad_trabajo BIGINT,
antiguedad_empresa VARCHAR(100),
nombre_cargo VARCHAR(200),
fk_tipo_cargo BIGINT,
fk_tiempo_cargo BIGINT,
nombre_area VARCHAR(200),
fk_tipo_contrato BIGINT,
fk_horas_labor BIGINT,
fk_tipo_salario BIGINT,
activo BOOLEAN DEFAULT TRUE,
fecha_creado TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
fecha_modificado TIMESTAMP,
usuario_crea VARCHAR(100),
usuario_modifica VARCHAR(100),
CONSTRAINT fk_eval_cliente_persona
	FOREIGN KEY (fk_evaluado)
	REFERENCES evaluado(id),
CONSTRAINT fk_eval_cliente_cliente
	FOREIGN KEY (fk_cliente)
	REFERENCES cliente(id),
CONSTRAINT fk_eval_cliente_ciudad
	FOREIGN KEY (fk_ciudad_trabajo)
	REFERENCES ciudad_municipio(id),
CONSTRAINT fk_eval_cliente_tipo_cargo
	FOREIGN KEY (fk_tipo_cargo)
	REFERENCES tipo_cargo(id),
CONSTRAINT fk_eval_cliente_tiempo_cargo
	FOREIGN KEY (fk_tiempo_cargo)
	REFERENCES tiempo_cargo(id),
CONSTRAINT fk_eval_cliente_contrato
	FOREIGN KEY (fk_tipo_contrato)
	REFERENCES tipo_contrato(id),
CONSTRAINT fk_eval_cliente_horas
	FOREIGN KEY (fk_horas_labor)
	REFERENCES horas_labor(id),
CONSTRAINT fk_eval_cliente_salario
	FOREIGN KEY (fk_tipo_salario)
	REFERENCES tipo_salario(id)
);

CREATE TABLE aplicacion (
    id BIGSERIAL PRIMARY KEY,
    fk_evaluado_cliente BIGINT NOT NULL,
    fecha_aplicacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observaciones TEXT,
    estado VARCHAR(20) DEFAULT 'FINALIZADA',
    fecha_creado TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificado TIMESTAMP,
    usuario_crea VARCHAR(100),
    usuario_modifica VARCHAR(100),
    CONSTRAINT fk_aplicacion_evaluado_cliente
        FOREIGN KEY (fk_evaluado_cliente)
        REFERENCES evaluado_cliente(id)
);

CREATE TABLE respuesta (
    id BIGSERIAL PRIMARY KEY,
    fk_aplicacion BIGINT NOT NULL,
    fk_pregunta BIGINT NOT NULL,
    fk_opcion_respuesta BIGINT NOT NULL,
    valor_obtenido NUMERIC(10,2) NOT NULL,
    fecha_respuesta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_respuesta_aplicacion
        FOREIGN KEY (fk_aplicacion)
        REFERENCES aplicacion(id),
    CONSTRAINT fk_respuesta_pregunta
        FOREIGN KEY (fk_pregunta)
        REFERENCES pregunta(id),
    CONSTRAINT fk_respuesta_opcion
        FOREIGN KEY (fk_opcion_respuesta)
        REFERENCES opcion_respuesta(id)
);

CREATE TABLE resultado_dimension (
    id BIGSERIAL PRIMARY KEY,
    fk_aplicacion BIGINT NOT NULL,
    fk_dimension_cuestionario BIGINT NOT NULL,
    puntaje_bruto NUMERIC(10,2) NOT NULL,
    puntaje_transformado NUMERIC(10,2) NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    fecha_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resultado_dimension_aplicacion
    	FOREIGN KEY (fk_aplicacion)
    	REFERENCES aplicacion(id),
    CONSTRAINT fk_resultado_dimension_dimcuest
    	FOREIGN KEY (fk_dimension_cuestionario)
    	REFERENCES dimension_cuestionario(id),
    CONSTRAINT fk_resultado_dimension_nivel
    	FOREIGN KEY (fk_nivel_riesgo)
    	REFERENCES nivel_riesgo(id),
    CONSTRAINT uk_resultado_dimension
    	UNIQUE (fk_aplicacion, fk_dimension_cuestionario)
);


CREATE TABLE resultado_dominio (
    id BIGSERIAL PRIMARY KEY,
    fk_aplicacion BIGINT NOT NULL,
    fk_dominio_cuestionario BIGINT NOT NULL,
    puntaje_bruto NUMERIC(10,2) NOT NULL,
    puntaje_transformado NUMERIC(10,2) NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    fecha_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resultado_dominio_aplicacion
        FOREIGN KEY (fk_aplicacion)
        REFERENCES aplicacion(id),
    CONSTRAINT fk_resultado_dominio_cuestionario
        FOREIGN KEY (fk_dominio_cuestionario)
        REFERENCES dominio_cuestionario(id),
   	CONSTRAINT fk_resultado_dominio_nivel
        FOREIGN KEY (fk_nivel_riesgo)
        REFERENCES nivel_riesgo(id)
);

CREATE TABLE resultado_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    fk_aplicacion BIGINT NOT NULL,
    fk_cuestionario BIGINT NOT NULL,
    puntaje_bruto NUMERIC(10,2) NOT NULL,
    puntaje_transformado NUMERIC(10,2) NOT NULL,
    fk_nivel_riesgo BIGINT NOT NULL,
    fecha_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resultado_cuestionario_aplicacion
        FOREIGN KEY (fk_aplicacion)
        REFERENCES aplicacion(id),
    CONSTRAINT fk_resultado_cuestionario
        FOREIGN KEY (fk_cuestionario)
        REFERENCES cuestionario(id),
    CONSTRAINT fk_resultado_cuestionario_nivel
        FOREIGN KEY (fk_nivel_riesgo)
        REFERENCES nivel_riesgo(id)
);