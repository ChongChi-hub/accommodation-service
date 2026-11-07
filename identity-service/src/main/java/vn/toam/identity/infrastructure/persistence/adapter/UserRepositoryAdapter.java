package vn.toam.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.toam.identity.domain.model.User;
import vn.toam.identity.domain.model.UserRoleType;
import vn.toam.identity.domain.model.UserStatus;
import vn.toam.identity.domain.repository.UserRepository;
import vn.toam.identity.infrastructure.persistence.entity.RoleJpaEntity;
import vn.toam.identity.infrastructure.persistence.entity.UserJpaEntity;
import vn.toam.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import vn.toam.identity.infrastructure.persistence.repository.SpringDataUserRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataRoleRepository springDataRoleRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository,
                                 SpringDataRoleRepository springDataRoleRepository) {
        this.springDataUserRepository = springDataUserRepository;
        this.springDataRoleRepository = springDataRoleRepository;
    }

    @Override
    public Optional<User> findById(String id) {
        return springDataUserRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return springDataUserRepository.findByPhone(phone).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return springDataUserRepository.existsByPhone(phone);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = springDataUserRepository.save(entity);
        return toDomain(saved);
    }

    private User toDomain(UserJpaEntity entity) {
        Set<UserRoleType> roles = entity.getRoles().stream()
                .map(r -> UserRoleType.fromString(r.getName()))
                .collect(Collectors.toSet());

        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getAvatarUrl(),
                entity.getGender(),
                entity.getDateOfBirth(),
                UserStatus.valueOf(entity.getStatus()),
                entity.getEmailVerifiedAt(),
                entity.getLastLoginAt(),
                roles,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private UserJpaEntity toEntity(User domain) {
        Set<RoleJpaEntity> roleEntities = domain.getRoles().stream()
                .map(roleType -> springDataRoleRepository.findByName(roleType.name())
                        .orElseGet(() -> springDataRoleRepository.save(
                                RoleJpaEntity.builder()
                                        .id(UUID.randomUUID().toString())
                                        .name(roleType.name())
                                        .description("Role " + roleType.name())
                                        .createdAt(Instant.now())
                                        .build()
                        )))
                .collect(Collectors.toSet());

        return UserJpaEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .passwordHash(domain.getPasswordHash())
                .fullName(domain.getFullName())
                .avatarUrl(domain.getAvatarUrl())
                .gender(domain.getGender())
                .dateOfBirth(domain.getDateOfBirth())
                .status(domain.getStatus().name())
                .emailVerifiedAt(domain.getEmailVerifiedAt())
                .lastLoginAt(domain.getLastLoginAt())
                .roles(roleEntities)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}

