package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto.VincularPersonaRequest;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.EvaluadoClienteEntity;

/** Una persona con su vinculación (formulario o fila del archivo) → entidades de persona y de relación. */
public final class VincularPersonaMapper {

    private VincularPersonaMapper() {
    }

    public static EvaluadoEntity toPersona(VincularPersonaRequest request) {
        EvaluadoEntity persona = new EvaluadoEntity();
        persona.setNumeroIdentificacion(request.numeroIdentificacion());
        persona.setNombre(request.nombre());
        persona.setApellido(request.apellido());
        persona.setFkSexo(request.fkSexo());
        persona.setAnioNacimiento(request.anioNacimiento());
        return persona;
    }

    public static EvaluadoClienteEntity toRelacion(VincularPersonaRequest request) {
        EvaluadoClienteEntity relacion = new EvaluadoClienteEntity();
        relacion.setFkEstadoCivil(request.fkEstadoCivil());
        relacion.setFkNivelAcademico(request.fkNivelAcademico());
        relacion.setFkCiudadResidencia(request.fkCiudadResidencia());
        relacion.setFkEstratoSocioeconomico(request.fkEstratoSocioeconomico());
        relacion.setFkTipoVivienda(request.fkTipoVivienda());
        relacion.setOcupacionProfesion(request.ocupacionProfesion());
        relacion.setFamiliaresDependientesEconomicamente(request.familiaresDependientesEconomicamente());
        relacion.setFkCiudadTrabajo(request.fkCiudadTrabajo());
        relacion.setAntiguedadEmpresa(request.antiguedadEmpresa());
        relacion.setNombreCargo(request.nombreCargo());
        relacion.setFkTipoCargo(request.fkTipoCargo());
        relacion.setFkTiempoCargo(request.fkTiempoCargo());
        relacion.setNombreArea(request.nombreArea());
        relacion.setFkTipoContrato(request.fkTipoContrato());
        relacion.setFkHorasLabor(request.fkHorasLabor());
        relacion.setFkTipoSalario(request.fkTipoSalario());
        relacion.setFechaVinculacion(request.fechaVinculacion());
        return relacion;
    }

}
