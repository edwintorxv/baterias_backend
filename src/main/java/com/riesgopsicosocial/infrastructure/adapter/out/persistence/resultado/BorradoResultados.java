package com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadocuestionario.ResultadoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodimension.ResultadoDimensionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodominio.ResultadoDominioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadototalgeneral.ResultadoTotalGeneralJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Borra los resultados de una aplicación cuando cambia algo de lo que salen (respuestas, preguntas filtro,
 * grupo ocupacional): así nunca queda un informe con resultados que no corresponden. La aplicación vuelve
 * a "pendiente de cálculo".
 */
@Component
public class BorradoResultados {

    private final ResultadoCuestionarioJpaRepository resultadoCuestionarioRepository;
    private final ResultadoDominioJpaRepository resultadoDominioRepository;
    private final ResultadoDimensionJpaRepository resultadoDimensionRepository;
    private final ResultadoTotalGeneralJpaRepository resultadoTotalGeneralRepository;

    public BorradoResultados(ResultadoCuestionarioJpaRepository resultadoCuestionarioRepository,
                             ResultadoDominioJpaRepository resultadoDominioRepository,
                             ResultadoDimensionJpaRepository resultadoDimensionRepository,
                             ResultadoTotalGeneralJpaRepository resultadoTotalGeneralRepository) {
        this.resultadoCuestionarioRepository = resultadoCuestionarioRepository;
        this.resultadoDominioRepository = resultadoDominioRepository;
        this.resultadoDimensionRepository = resultadoDimensionRepository;
        this.resultadoTotalGeneralRepository = resultadoTotalGeneralRepository;
    }

    public boolean tieneResultados(Long idAplicacion) {
        return !resultadoCuestionarioRepository.findByFkAplicacion(idAplicacion).isEmpty();
    }

    /** @return si tenía resultados (para avisar que hay que volver a calcular) */
    public boolean borrar(Long idAplicacion) {
        if (!tieneResultados(idAplicacion)) {
            return false;
        }
        resultadoDimensionRepository.eliminarPorAplicacion(idAplicacion);
        resultadoDominioRepository.eliminarPorAplicacion(idAplicacion);
        resultadoCuestionarioRepository.eliminarPorAplicacion(idAplicacion);
        resultadoTotalGeneralRepository.eliminarPorAplicacion(idAplicacion);
        return true;
    }

}
