package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informe;

import com.riesgopsicosocial.domain.model.informe.empresa.Categoria;
import com.riesgopsicosocial.domain.model.informe.empresa.FichaPersona;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelacademico.NivelAcademicoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocontrato.TipoContratoJpaRepository;
import com.riesgopsicosocial.shared.catalogo.CatalogoJpaRepository;
import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Datos generales de las personas de una empresa para describir la población del informe, cargados
 * en bloque: los evaluados de las relaciones y los catálogos (sexo, nivel académico, tipo de cargo y
 * tipo de contrato). Sexo y edad salen de la persona; nivel académico, cargo y contrato, de la relación
 * (la foto de esa vinculación).
 */
@Component
class FichasPersonaLoader {

    private final EvaluadoJpaRepository evaluadoRepository;
    private final SexoJpaRepository sexoRepository;
    private final NivelAcademicoJpaRepository nivelAcademicoRepository;
    private final TipoCargoJpaRepository tipoCargoRepository;
    private final TipoContratoJpaRepository tipoContratoRepository;

    FichasPersonaLoader(EvaluadoJpaRepository evaluadoRepository, SexoJpaRepository sexoRepository,
                        NivelAcademicoJpaRepository nivelAcademicoRepository, TipoCargoJpaRepository tipoCargoRepository,
                        TipoContratoJpaRepository tipoContratoRepository) {
        this.evaluadoRepository = evaluadoRepository;
        this.sexoRepository = sexoRepository;
        this.nivelAcademicoRepository = nivelAcademicoRepository;
        this.tipoCargoRepository = tipoCargoRepository;
        this.tipoContratoRepository = tipoContratoRepository;
    }

    /** Ficha de cada relación, por id de {@code evaluado_cliente}. */
    Map<Long, FichaPersona> cargar(List<EvaluadoClienteEntity> relaciones) {
        Map<Long, EvaluadoEntity> evaluados = evaluadoRepository.findAllById(relaciones.stream()
                        .map(EvaluadoClienteEntity::getFkEvaluado).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(EvaluadoEntity::getId, Function.identity()));
        Map<Long, Categoria> sexos = categorias(sexoRepository);
        Map<Long, Categoria> niveles = categorias(nivelAcademicoRepository);
        Map<Long, Categoria> cargos = categorias(tipoCargoRepository);
        Map<Long, Categoria> contratos = categorias(tipoContratoRepository);

        return relaciones.stream().collect(Collectors.toMap(EvaluadoClienteEntity::getId, ec -> {
            EvaluadoEntity evaluado = evaluados.get(ec.getFkEvaluado());
            return new FichaPersona(
                    evaluado == null ? null : buscar(sexos, evaluado.getFkSexo()),
                    evaluado == null ? null : evaluado.getAnioNacimiento(),
                    buscar(niveles, ec.getFkNivelAcademico()),
                    buscar(cargos, ec.getFkTipoCargo()),
                    buscar(contratos, ec.getFkTipoContrato()));
        }));
    }

    private static <T extends Identificable & Nombrable> Map<Long, Categoria> categorias(CatalogoJpaRepository<T> repository) {
        return repository.findAll().stream()
                .collect(Collectors.toMap(Identificable::getId, c -> new Categoria(c.getId(), c.getNombre())));
    }

    private static Categoria buscar(Map<Long, Categoria> catalogo, Long id) {
        return id == null ? null : catalogo.get(id);
    }

}
