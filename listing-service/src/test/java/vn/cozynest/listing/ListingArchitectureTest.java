package vn.cozynest.listing;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "vn.cozynest.listing", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ListingArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domainMustNotDependOnApplication =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..");

    @ArchTest
    static final ArchRule domainMustNotDependOnInterfaces =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule domainMustNotDependOnJakartaPersistence =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule domainMustNotDependOnSpringFramework =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnInfrastructure =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnJakartaPersistence =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");
}
