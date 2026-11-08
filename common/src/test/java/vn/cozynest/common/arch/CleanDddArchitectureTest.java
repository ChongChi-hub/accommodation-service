package vn.cozynest.common.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class CleanDddArchitectureTest {

    @Test
    @DisplayName("Domain layer must not depend on application, infrastructure, or interfaces layers")
    void domainMustNotDependOnOuterLayers() {
        JavaClasses classes = new ClassFileImporter().importPackages("vn.cozynest");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..", "org.springframework..", "jakarta.persistence..");

        rule.allowEmptyShould(true).check(classes);
    }

    @Test
    @DisplayName("Application layer must not depend on infrastructure or interfaces layers")
    void applicationMustNotDependOnInfrastructureOrInterfaces() {
        JavaClasses classes = new ClassFileImporter().importPackages("vn.cozynest");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..interfaces..");

        rule.allowEmptyShould(true).check(classes);
    }
}

