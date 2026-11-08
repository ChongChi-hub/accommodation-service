package vn.cozynest.admin.interfaces.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import vn.cozynest.common.dto.ApiResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminHealthControllerTest {

    @Test
    @DisplayName("Verify Admin HealthController returns UP status and admin-service name")
    void testHealthEndpointReturnsUp() {
        HealthController controller = new HealthController();
        ResponseEntity<ApiResponse<Map<String, String>>> response = controller.health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).containsEntry("status", "UP");
        assertThat(response.getBody().getData()).containsEntry("service", "admin-service");
    }
}

