package vn.cozynest.listing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.listing.application.dto.RoomTypeDto;
import vn.cozynest.listing.domain.repository.RoomTypeRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/room-types")
@RequiredArgsConstructor
@Tag(name = "Room Types", description = "Danh mục loại hình phòng trọ")
public class RoomTypeController {

    private final RoomTypeRepository roomTypeRepository;

    @GetMapping
    @Operation(summary = "Lấy danh mục tất cả loại hình phòng trọ")
    public ResponseEntity<ApiResponse<List<RoomTypeDto>>> getRoomTypes() {
        List<RoomTypeDto> items = roomTypeRepository.findAllByOrderByIdAsc().stream()
                .map(rt -> RoomTypeDto.builder()
                        .id(rt.getId())
                        .code(rt.getCode())
                        .name(rt.getName())
                        .description(rt.getDescription())
                        .icon(rt.getIcon())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}

