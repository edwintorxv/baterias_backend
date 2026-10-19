package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

/**
 * Columnas de la hoja "Personas", en orden. {@code propiedad} es el campo de {@code VincularPersonaRequest}
 * (para poner los errores de validación en la columna que corresponde).
 */
public enum ColumnaPersona {

    DOCUMENTO("Documento", true, Tipo.TEXTO, null, "numeroIdentificacion", 16),
    NOMBRE("Nombres", true, Tipo.TEXTO, null, "nombre", 22),
    APELLIDO("Apellidos", true, Tipo.TEXTO, null, "apellido", 22),
    SEXO("Sexo", true, Tipo.LISTA, ListaPlantilla.SEXO, "fkSexo", 16),
    ANIO_NACIMIENTO("Año de nacimiento", true, Tipo.ENTERO, null, "anioNacimiento", 12),
    ESTADO_CIVIL("Estado civil", false, Tipo.LISTA, ListaPlantilla.ESTADO_CIVIL, "fkEstadoCivil", 16),
    NIVEL_ACADEMICO("Nivel académico", true, Tipo.LISTA, ListaPlantilla.NIVEL_ACADEMICO, "fkNivelAcademico", 28),
    CIUDAD_RESIDENCIA("Ciudad de residencia", false, Tipo.LISTA, ListaPlantilla.CIUDAD, "fkCiudadResidencia", 30),
    ESTRATO("Estrato", false, Tipo.LISTA, ListaPlantilla.ESTRATO, "fkEstratoSocioeconomico", 10),
    TIPO_VIVIENDA("Tipo de vivienda", false, Tipo.LISTA, ListaPlantilla.TIPO_VIVIENDA, "fkTipoVivienda", 14),
    OCUPACION("Ocupación o profesión", false, Tipo.TEXTO, null, "ocupacionProfesion", 22),
    PERSONAS_A_CARGO("Personas a cargo", false, Tipo.ENTERO, null, "familiaresDependientesEconomicamente", 12),
    CIUDAD_TRABAJO("Ciudad de trabajo", false, Tipo.LISTA, ListaPlantilla.CIUDAD, "fkCiudadTrabajo", 30),
    CARGO("Cargo", false, Tipo.TEXTO, null, "nombreCargo", 24),
    TIPO_CARGO("Tipo de cargo", true, Tipo.LISTA, ListaPlantilla.TIPO_CARGO, "fkTipoCargo", 34),
    AREA("Área", false, Tipo.TEXTO, null, "nombreArea", 20),
    ANTIGUEDAD("Antigüedad en la empresa", false, Tipo.TEXTO, null, "antiguedadEmpresa", 14),
    TIEMPO_CARGO("Tiempo en el cargo", false, Tipo.LISTA, ListaPlantilla.TIEMPO_CARGO, "fkTiempoCargo", 16),
    TIPO_CONTRATO("Tipo de contrato", false, Tipo.LISTA, ListaPlantilla.TIPO_CONTRATO, "fkTipoContrato", 26),
    HORAS("Horas de trabajo diarias", false, Tipo.LISTA, ListaPlantilla.HORAS_LABOR, "fkHorasLabor", 12),
    TIPO_SALARIO("Tipo de salario", false, Tipo.LISTA, ListaPlantilla.TIPO_SALARIO, "fkTipoSalario", 34),
    FECHA_VINCULACION("Fecha de vinculación", false, Tipo.FECHA, null, "fechaVinculacion", 14);

    public enum Tipo { TEXTO, ENTERO, LISTA, FECHA }

    private final String titulo;
    private final boolean obligatoria;
    private final Tipo tipo;
    private final ListaPlantilla lista;
    private final String propiedad;
    /** Ancho en caracteres. */
    private final int ancho;

    ColumnaPersona(String titulo, boolean obligatoria, Tipo tipo, ListaPlantilla lista, String propiedad, int ancho) {
        this.titulo = titulo;
        this.obligatoria = obligatoria;
        this.tipo = tipo;
        this.lista = lista;
        this.propiedad = propiedad;
        this.ancho = ancho;
    }

    public String titulo() {
        return titulo;
    }

    /** Lo que va en el encabezado: el título, con * si es obligatoria. */
    public String encabezado() {
        return obligatoria ? titulo + " *" : titulo;
    }

    public boolean obligatoria() {
        return obligatoria;
    }

    public Tipo tipo() {
        return tipo;
    }

    public ListaPlantilla lista() {
        return lista;
    }

    public String propiedad() {
        return propiedad;
    }

    public int ancho() {
        return ancho;
    }

}
