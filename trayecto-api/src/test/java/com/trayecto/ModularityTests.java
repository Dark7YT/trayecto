package com.trayecto;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    private static final ApplicationModules MODULES =
        ApplicationModules.of(TrayectoApiApplication.class);

    @Test
    void verifyModularBoundaries() {
        MODULES.verify();
    }

    @Test
    void writeDocumentationSnippets() {
        new Documenter(MODULES)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }
}
