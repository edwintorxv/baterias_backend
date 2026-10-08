package com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado;

import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoAplicacionResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoCuestionarioResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoDetalleResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoTotalGeneralResponse;

import java.math.BigDecimal;

/** Convierte los resultados del dominio a DTOs; lo comparten los controladores de resultados e informes. */
public final class ResultadoResponseMapper {

    private ResultadoResponseMapper() {
    }

    public static ResultadoAplicacionResponse toResponse(ResultadoAplicacion resultado) {
        return new ResultadoAplicacionResponse(
                resultado.idAplicacion(),
                resultado.idGrupoOcupacional(),
                resultado.fechaCalculo(),
                resultado.cuestionarios().stream().map(ResultadoResponseMapper::toResponse).toList(),
                toResponse(resultado.totalGeneral())
        );
    }

    private static ResultadoTotalGeneralResponse toResponse(ResultadoTotalGeneral total) {
        if (total == null) {
            return null;
        }
        return new ResultadoTotalGeneralResponse(
                total.idCuestionarioIntralaboral(),
                total.formaIntralaboral(),
                total.puntajeBruto(),
                total.puntajeTransformado(),
                total.nivelRiesgo().id(),
                total.nivelRiesgo().nombre());
    }

    private static ResultadoCuestionarioResponse toResponse(ResultadoCuestionario rc) {
        return new ResultadoCuestionarioResponse(
                rc.idCuestionario(),
                rc.forma(),
                rc.puntajeBruto(),
                rc.puntajeTransformado(),
                rc.nivelRiesgo().id(),
                rc.nivelRiesgo().nombre(),
                rc.dominios().stream()
                        .map(d -> detalle(d.idDominioCuestionario(), d.nombre(), d.puntajeBruto(),
                                d.puntajeTransformado(), d.nivelRiesgo()))
                        .toList(),
                rc.dimensiones().stream()
                        .map(d -> detalle(d.idDimensionCuestionario(), d.nombre(), d.puntajeBruto(),
                                d.puntajeTransformado(), d.nivelRiesgo()))
                        .toList()
        );
    }

    private static ResultadoDetalleResponse detalle(Long id, String nombre, BigDecimal bruto,
                                                    BigDecimal transformado, NivelRiesgo nivel) {
        return new ResultadoDetalleResponse(id, nombre, bruto, transformado, nivel.id(), nivel.nombre());
    }

}
