package vn.cozynest.identity.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.AdminUserItemDto;
import vn.cozynest.identity.domain.model.User;
import vn.cozynest.identity.domain.model.UserProfile;
import vn.cozynest.identity.domain.model.UserRoleType;
import vn.cozynest.identity.domain.model.UserStatus;
import vn.cozynest.identity.domain.repository.UserProfileRepository;
import vn.cozynest.identity.domain.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/v1/admin/users", "/api/v1/users/admin"})
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public AdminUserController(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserItemDto>>> listUsers() {
        List<User> users = userRepository.findAll();
        List<AdminUserItemDto> dtos = users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping("/{id}/toggle-lock")
    public ResponseEntity<ApiResponse<Map<String, String>>> toggleLock(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung: " + id));
        if (user.getStatus() == UserStatus.LOCKED) {
            user.unlock();
        } else {
            user.lock();
        }
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(
                "Cap nhat trang thai thanh cong",
                Map.of("id", user.getId(), "status", user.getStatus().name())
        ));
    }

    private AdminUserItemDto toDto(User user) {
        String fullName = user.getFullName();
        if (fullName == null || fullName.isBlank()) {
            Optional<UserProfile> profile = userProfileRepository.findByUserId(user.getId());
            if (profile.isPresent() && profile.get().getFullName() != null && !profile.get().getFullName().isBlank()) {
                fullName = profile.get().getFullName();
            } else {
                fullName = user.getEmail() != null ? user.getEmail().split("@")[0] : "Nguoi dung";
            }
        }

        String primaryRole = "TENANT";
        String roleText = "Nguoi thue";
        if (user.getRoles().contains(UserRoleType.ADMIN)) {
            primaryRole = "ADMIN";
            roleText = "Quan tri vien";
        } else if (user.getRoles().contains(UserRoleType.MODERATOR)) {
            primaryRole = "MODERATOR";
            roleText = "Kiem duyet vien";
        } else if (user.getRoles().contains(UserRoleType.LANDLORD)) {
            primaryRole = "LANDLORD";
            roleText = "Chu tro";
        }

        String phone = user.getPhone() != null ? user.getPhone() : "";
        return new AdminUserItemDto(
                user.getId(),
                fullName,
                user.getEmail(),
                phone,
                primaryRole,
                roleText,
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}

