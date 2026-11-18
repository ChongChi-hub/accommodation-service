package vn.cozynest.booking.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.cozynest.booking.application.dto.CompleteAppointmentRequest;
import vn.cozynest.booking.application.dto.CreateContractDto;
import vn.cozynest.booking.application.dto.RejectRentalRequestDto;
import vn.cozynest.booking.domain.repository.ViewingAppointmentRepository;
import vn.cozynest.booking.infrastructure.security.UserContextResolver;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BookingLifecycleSecurityMatrixTest {

    @Test
    void fixtureControllerBeanIsAbsentInProdAndWhenFlagDisabled() {
        ApplicationContextRunner prodRunner = new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod", "app.booking.fixtures.enabled=true")
                .withBean(ViewingAppointmentRepository.class, () -> mock(ViewingAppointmentRepository.class))
                .withBean(UserContextResolver.class, () -> mock(UserContextResolver.class))
                .withBean(Clock.class, Clock::systemUTC)
                .withUserConfiguration(DevAppointmentFixtureController.class);

        prodRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(DevAppointmentFixtureController.class));

        ApplicationContextRunner devDefaultRunner = new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=dev")
                .withBean(ViewingAppointmentRepository.class, () -> mock(ViewingAppointmentRepository.class))
                .withBean(UserContextResolver.class, () -> mock(UserContextResolver.class))
                .withBean(Clock.class, Clock::systemUTC)
                .withUserConfiguration(DevAppointmentFixtureController.class);

        devDefaultRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(DevAppointmentFixtureController.class));

        ApplicationContextRunner testEnabledRunner = new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=test", "app.booking.fixtures.enabled=true")
                .withBean(ViewingAppointmentRepository.class, () -> mock(ViewingAppointmentRepository.class))
                .withBean(UserContextResolver.class, () -> mock(UserContextResolver.class))
                .withBean(Clock.class, Clock::systemUTC)
                .withUserConfiguration(DevAppointmentFixtureController.class);

        testEnabledRunner.run(ctx -> assertThat(ctx).hasSingleBean(DevAppointmentFixtureController.class));
    }

    @Test
    void fiveRoleSecurityAnnotationsEnforcedOnH1AndH2Endpoints() throws Exception {
        // H1: LandlordAppointmentController requires LANDLORD role (Guest=401, Tenant/Moderator/Admin=403)
        PreAuthorize apptClassAuth = LandlordAppointmentController.class.getAnnotation(PreAuthorize.class);
        assertThat(apptClassAuth).isNotNull();
        assertThat(apptClassAuth.value()).isEqualTo("hasRole('LANDLORD')");

        Method patchComplete = LandlordAppointmentController.class.getMethod("patchCompleteAppointment", String.class, CompleteAppointmentRequest.class);
        Method patchNoShow = LandlordAppointmentController.class.getMethod("patchNoShowAppointment", String.class, CompleteAppointmentRequest.class);
        assertThat(patchComplete).isNotNull();
        assertThat(patchNoShow).isNotNull();

        // H2: reject & createContract require LANDLORD or ADMIN (Guest=401, Tenant/Moderator=403)
        Method rejectMethod = RentalRequestController.class.getMethod("rejectRentalRequest", String.class, RejectRentalRequestDto.class);
        PreAuthorize rejectAuth = rejectMethod.getAnnotation(PreAuthorize.class);
        assertThat(rejectAuth).isNotNull();
        assertThat(rejectAuth.value()).contains("LANDLORD").contains("ADMIN").doesNotContain("TENANT").doesNotContain("MODERATOR");

        Method createContract = ContractController.class.getMethod("createContract", CreateContractDto.class);
        PreAuthorize contractAuth = createContract.getAnnotation(PreAuthorize.class);
        assertThat(contractAuth).isNotNull();
        assertThat(contractAuth.value()).contains("LANDLORD").contains("ADMIN").doesNotContain("TENANT").doesNotContain("MODERATOR");

        // Fixture controller annotations
        assertThat(DevAppointmentFixtureController.class.getAnnotation(Profile.class)).isNotNull();
        assertThat(DevAppointmentFixtureController.class.getAnnotation(ConditionalOnProperty.class)).isNotNull();
    }
}
