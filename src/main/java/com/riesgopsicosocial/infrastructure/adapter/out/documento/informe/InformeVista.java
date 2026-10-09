package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import java.util.List;

/**
 * Informe individual ya preparado para mostrar (textos y números formateados). Lo comparten
 * los generadores PDF y Word, para que ambos documentos digan exactamente lo mismo.
 */
public record InformeVista(
        String empresa,
        String nitEmpresa,
        String nombreTrabajador,
        String identificacion,
        String sexo,
        String fechaElaboracion,
        List<AplicacionVista> aplicaciones
) {

    /**
     * @param totalGeneral {@code null} si la aplicación no tiene intralaboral y extralaboral
     * @param incluyeEstres para agregar la interpretación de los niveles de estrés
     */
    public record AplicacionVista(
            String fechaAplicacion,
            String cargo,
            String area,
            String edad,
            String grupoOcupacional,
            EvaluadorVista evaluador,
            List<CuestionarioVista> cuestionarios,
            FilaVista totalGeneral,
            boolean incluyeEstres,
            String observaciones,
            String recomendaciones
    ) {
    }

    /** @param firmaDataUri imagen como {@code data:} URI; {@code null} si no hay firma */
    public record EvaluadorVista(
            String nombre,
            String identificacion,
            String profesion,
            String posgrado,
            String tarjetaProfesional,
            String licenciaSaludOcupacional,
            String fechaExpedicionLicencia,
            String firmaDataUri,
            byte[] firma
    ) {
    }

    /**
     * @param dominios    solo intralaboral (A/B)
     * @param dimensiones solo extralaboral (C)
     * @param tituloNivel "Nivel de riesgo" o, para estrés, "Nivel de síntomas de estrés"
     */
    public record CuestionarioVista(
            String titulo,
            TipoCuestionario tipo,
            List<DominioVista> dominios,
            List<FilaVista> dimensiones,
            FilaVista total,
            String tituloNivel
    ) {
    }

    public record DominioVista(String nombre, List<FilaVista> dimensiones, FilaVista total) {
    }

    /** @param idNivel 1..5, para el color de la celda */
    public record FilaVista(String nombre, String puntaje, String nivel, long idNivel) {
    }

    public enum TipoCuestionario {
        INTRALABORAL, EXTRALABORAL, ESTRES
    }

}
