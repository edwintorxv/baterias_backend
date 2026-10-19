package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas.CatalogosCarga.Opcion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio.CiudadMunicipioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio.CiudadMunicipioJpaResository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.departamento.DepartamentoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.departamento.DepartamentoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.estadocivil.EstadoCivilJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.estratosocioeconomico.EstratoSocioeconomicoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.horalabor.HoraLaborJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelacademico.NivelAcademicoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tiempocargo.TiempoCargoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo.TipoCargoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocontrato.TipoContratoJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tiposalario.TipoSalarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipovivienda.TipoViviendaJpaRepository;
import com.riesgopsicosocial.shared.catalogo.CatalogoJpaRepository;
import com.riesgopsicosocial.shared.catalogo.Identificable;
import com.riesgopsicosocial.shared.catalogo.Nombrable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/** Carga en bloque los catálogos de la plantilla: en el orden de la batería, y las ciudades por nombre. */
@Component
class CatalogosCargaLoader {

    private final Map<ListaPlantilla, CatalogoJpaRepository<? extends Nombrable>> repositorios = new EnumMap<>(ListaPlantilla.class);
    private final CiudadMunicipioJpaResository ciudadRepository;
    private final DepartamentoJpaRepository departamentoRepository;

    CatalogosCargaLoader(SexoJpaRepository sexo, EstadoCivilJpaRepository estadoCivil, NivelAcademicoJpaRepository nivelAcademico,
                         EstratoSocioeconomicoJpaRepository estrato, TipoViviendaJpaRepository tipoVivienda,
                         TipoCargoJpaRepository tipoCargo, TiempoCargoJpaRepository tiempoCargo,
                         TipoContratoJpaRepository tipoContrato, HoraLaborJpaRepository horasLabor,
                         TipoSalarioJpaRepository tipoSalario, CiudadMunicipioJpaResository ciudadRepository,
                         DepartamentoJpaRepository departamentoRepository) {
        repositorios.put(ListaPlantilla.SEXO, sexo);
        repositorios.put(ListaPlantilla.ESTADO_CIVIL, estadoCivil);
        repositorios.put(ListaPlantilla.NIVEL_ACADEMICO, nivelAcademico);
        repositorios.put(ListaPlantilla.ESTRATO, estrato);
        repositorios.put(ListaPlantilla.TIPO_VIVIENDA, tipoVivienda);
        repositorios.put(ListaPlantilla.TIPO_CARGO, tipoCargo);
        repositorios.put(ListaPlantilla.TIEMPO_CARGO, tiempoCargo);
        repositorios.put(ListaPlantilla.TIPO_CONTRATO, tipoContrato);
        repositorios.put(ListaPlantilla.HORAS_LABOR, horasLabor);
        repositorios.put(ListaPlantilla.TIPO_SALARIO, tipoSalario);
        this.ciudadRepository = ciudadRepository;
        this.departamentoRepository = departamentoRepository;
    }

    CatalogosCarga cargar() {
        Map<ListaPlantilla, List<Opcion>> opciones = new EnumMap<>(ListaPlantilla.class);
        repositorios.forEach((lista, repositorio) -> opciones.put(lista, opciones(repositorio)));

        Map<Long, String> departamentos = departamentoRepository.findAll().stream()
                .collect(Collectors.toMap(DepartamentoEntity::getId, DepartamentoEntity::getNombre));
        List<CiudadMunicipioEntity> ciudades = ciudadRepository.findAll();
        opciones.put(ListaPlantilla.CIUDAD, ciudades.stream()
                .map(c -> new Opcion(c.getId(), CatalogosCarga.etiquetaCiudad(c.getNombre(), departamentos.get(c.getFkDepartamento()))))
                .sorted(Comparator.comparing(Opcion::etiqueta))
                .toList());
        Map<String, List<Long>> porNombre = ciudades.stream()
                .collect(Collectors.groupingBy(CiudadMunicipioEntity::getNombre,
                        Collectors.mapping(CiudadMunicipioEntity::getId, Collectors.toList())));
        return new CatalogosCarga(opciones, porNombre);
    }

    private static List<Opcion> opciones(CatalogoJpaRepository<? extends Nombrable> repositorio) {
        return repositorio.findAll().stream()
                .map(c -> new Opcion(((Identificable) c).getId(), c.getNombre()))
                .sorted(Comparator.comparing(Opcion::id))
                .toList();
    }

}
