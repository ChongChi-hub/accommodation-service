package vn.toam.identity.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class IdentityArchitectureTest {

    @Test
    @DisplayName("Identity Domain layer must not depend on Application, Infrastructure, Interfaces or Spring")
    void domainMustNotDependOnOuterLayers() {
        JavaClasses classes = new ClassFileImporter().importPackages("vn.toam.identity");

        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..", "org.springframework..", "jakarta.persistence..");

        rule.allowEmptyShould(false).check(classes);
    }
}

