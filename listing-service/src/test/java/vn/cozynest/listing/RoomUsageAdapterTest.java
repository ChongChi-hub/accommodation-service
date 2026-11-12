package vn.cozynest.listing;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.cozynest.listing.domain.model.Room;
import vn.cozynest.listing.domain.repository.RoomRepository;
import vn.cozynest.listing.infrastructure.adapter.RoomUsageAdapter;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomUsageAdapterTest {

    @Test
    @DisplayName("LANDLORD-BE.1: Blocks delete when room is RENTED or booking-service reports active contract/appointment; fails closed on error")
    void shouldCheckBookingServiceUsageOverHttp() throws Exception {
        AtomicInteger status = new AtomicInteger(200);
        AtomicReference<String> body = new AtomicReference<>("{\"success\":true,\"data\":{\"inActiveUse\":true}}");

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/rooms/101/usage", ex -> {
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status.get(), bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();

        try {
            RoomRepository roomRepository = mock(RoomRepository.class);
            when(roomRepository.findById(101L)).thenReturn(Optional.of(Room.builder().id(101L).status("AVAILABLE").build()));

            RoomUsageAdapter adapter = new RoomUsageAdapter(
                    roomRepository, "http://localhost:" + server.getAddress().getPort(), "CozyNestInternalServiceKeySecret2026"
            );

            // Active contract or appointment in booking-service -> blocked
            assertThat(adapter.isRoomInActiveUse(101L)).isTrue();

            // Free in booking-service & AVAILABLE in listing -> deletable
            body.set("{\"success\":true,\"data\":{\"inActiveUse\":false}}");
            assertThat(adapter.isRoomInActiveUse(101L)).isFalse();

            // Booking-service unavailable (500) -> fail-closed blocks delete
            status.set(500);
            assertThat(adapter.isRoomInActiveUse(101L)).isTrue();
        } finally {
            server.stop(0);
        }
    }
}

