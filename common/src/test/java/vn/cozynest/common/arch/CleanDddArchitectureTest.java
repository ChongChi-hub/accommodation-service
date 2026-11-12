package vn.cozynest.common.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "vn.cozynest.common", importOptions = {ImportOption.DoNotIncludeTests.class})
public class CleanDddArchitectureTest {

    @ArchTest
    static final ArchRule commonMustNotDependOnJpa =
            noClasses().that().resideInAPackage("vn.cozynest.common..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule commonMustNotDependOnMicroservices =
            noClasses().that().resideInAPackage("vn.cozynest.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "vn.cozynest.identity..",
                            "vn.cozynest.listing..",
                            "vn.cozynest.booking..",
                            "vn.cozynest.billing..",
                            "vn.cozynest.communication..",
                            "vn.cozynest.admin.."
                    );
}
