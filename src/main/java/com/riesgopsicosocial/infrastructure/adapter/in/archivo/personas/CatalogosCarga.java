package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Valores de las listas de la plantilla y la búsqueda de lo que viene escrito en el archivo (sin tildes,
 * mayúsculas ni espacios). Las ciudades se muestran como "MEDELLIN – ANTIOQUIA"; también se aceptan
 * solo por su nombre si no se repite en otro departamento.
 */
public final class CatalogosCarga {

    static final String SEPARADOR_CIUDAD = " – ";

    public record Opcion(Long id, String etiqueta) {
    }

    /** Búsqueda: el id, o el mensaje de por qué no se encontró. */
    public record Busqueda(Long id, String error) {
    }

    private final Map<ListaPlantilla, List<Opcion>> opciones;
    private final Map<ListaPlantilla, Map<String, Long>> porEtiqueta = new EnumMap<>(ListaPlantilla.class);
    /** Ciudad por su solo nombre: los ids de todas las que se llaman así. */
    private final Map<String, List<Long>> ciudadesPorNombre;

    /**
     * @param ciudadesPorNombre ciudades por su nombre sin departamento (puede haber varias con el mismo)
     */
    public CatalogosCarga(Map<ListaPlantilla, List<Opcion>> opciones, Map<String, List<Long>> ciudadesPorNombre) {
        this.opciones = new EnumMap<>(opciones);
        this.opciones.forEach((lista, valores) -> porEtiqueta.put(lista, valores.stream()
                .collect(Collectors.toMap(o -> TextoNormalizado.de(o.etiqueta()), Opcion::id, (a, b) -> a))));
        this.ciudadesPorNombre = ciudadesPorNombre.entrySet().stream()
                .collect(Collectors.toMap(e -> TextoNormalizado.de(e.getKey()), Map.Entry::getValue,
                        (a, b) -> { List<Long> todas = new ArrayList<>(a); todas.addAll(b); return todas; }));
    }

    public List<Opcion> opciones(ListaPlantilla lista) {
        return opciones.getOrDefault(lista, List.of());
    }

    public static String etiquetaCiudad(String ciudad, String departamento) {
        return ciudad + SEPARADOR_CIUDAD + departamento;
    }

    Busqueda buscar(ListaPlantilla lista, String texto) {
        String clave = TextoNormalizado.de(texto);
        Long id = porEtiqueta.getOrDefault(lista, Map.of()).get(clave);
        if (id != null) {
            return new Busqueda(id, null);
        }
        if (lista == ListaPlantilla.CIUDAD) {
            List<Long> ciudades = ciudadesPorNombre.getOrDefault(clave, List.of());
            if (ciudades.size() == 1) {
                return new Busqueda(ciudades.get(0), null);
            }
            if (ciudades.size() > 1) {
                return new Busqueda(null, "'" + texto + "' existe en varios departamentos: escríbala como 'CIUDAD"
                        + SEPARADOR_CIUDAD + "DEPARTAMENTO'");
            }
        }
        return new Busqueda(null, "'" + texto + "' no está en la lista");
    }

}
