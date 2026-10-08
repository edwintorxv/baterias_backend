package com.riesgopsicosocial.arquitectura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas del hexágono (domain + application). Los catálogos y el CRUD usan a propósito el
 * patrón directo Controller → Service → JpaRepository, por eso no se validan aquí.
 */
@AnalyzeClasses(packages = "com.riesgopsicosocial", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaHexagonalTest {

    private static final String DOMINIO = "com.riesgopsicosocial.domain..";
    private static final String APLICACION = "com.riesgopsicosocial.application..";
    private static final String PUERTOS = "com.riesgopsicosocial.application.port..";
    private static final String SERVICIOS_APLICACION = "com.riesgopsicosocial.application.service..";
    private static final String INFRAESTRUCTURA = "com.riesgopsicosocial.infrastructure..";
    private static final String ADAPTADORES_ENTRADA = "com.riesgopsicosocial.infrastructure.adapter.in..";

    @ArchTest
    static final ArchRule dominioSoloDependeDeJavaYDeSiMismo = classes()
            .that().resideInAPackage(DOMINIO)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMINIO)
            .because("el dominio no debe conocer Spring, JPA, shared ni las otras capas");

    @ArchTest
    static final ArchRule aplicacionNoDependeDeInfraestructuraNiPersistencia = noClasses()
            .that().resideInAPackage(APLICACION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    INFRAESTRUCTURA, "jakarta.persistence..", "org.springframework.data..")
            .because("la aplicación accede a datos solo a través de sus puertos de salida");

    @ArchTest
    static final ArchRule puertosSoloUsanDominio = classes()
            .that().resideInAPackage(PUERTOS)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMINIO, PUERTOS)
            .because("los puertos se expresan con modelos del dominio, nunca con entidades o DTOs");

    @ArchTest
    static final ArchRule adaptadoresDeEntradaUsanCasosDeUso = noClasses()
            .that().resideInAPackage(ADAPTADORES_ENTRADA)
            .should().dependOnClassesThat().resideInAPackage(SERVICIOS_APLICACION)
            .because("los controladores entran por los puertos de entrada (casos de uso), no por la implementación");

}
