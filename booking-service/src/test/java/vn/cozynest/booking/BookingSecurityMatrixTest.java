package vn.cozynest.booking;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import vn.cozynest.booking.interfaces.rest.ContractController;
import vn.cozynest.booking.interfaces.rest.RentalRequestController;
import vn.cozynest.booking.interfaces.rest.SagaController;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class BookingSecurityMatrixTest {

    @Test
    void shouldRequirePreAuthorizeOnBookingControllers() throws Exception {
        PreAuthorize sagaClassAuth = SagaController.class.getAnnotation(PreAuthorize.class);
        assertThat(sagaClassAuth).isNotNull();
        assertThat(sagaClassAuth.value()).contains("ADMIN");

        Method approveReq = RentalRequestController.class.getMethod("approveRentalRequest", String.class, String.class);
        assertThat(approveReq.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(approveReq.getAnnotation(PreAuthorize.class).value()).contains("LANDLORD");

        Method createContract = ContractController.class.getMethod("createContract",
                vn.cozynest.booking.application.dto.CreateContractDto.class);
        assertThat(createContract.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(createContract.getAnnotation(PreAuthorize.class).value()).contains("LANDLORD");
    }
}

