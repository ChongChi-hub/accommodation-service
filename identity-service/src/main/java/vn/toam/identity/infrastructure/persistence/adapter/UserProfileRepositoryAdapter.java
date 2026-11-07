package vn.toam.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import vn.toam.identity.domain.model.UserProfile;
import vn.toam.identity.domain.repository.UserProfileRepository;
import vn.toam.identity.infrastructure.persistence.entity.UserProfileJpaEntity;
import vn.toam.identity.infrastructure.persistence.repository.SpringDataUserProfileRepository;

import java.util.Optional;

@Component
public class UserProfileRepositoryAdapter implements UserProfileRepository {

    private final SpringDataUserProfileRepository springRepo;

    public UserProfileRepositoryAdapter(SpringDataUserProfileRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<UserProfile> findById(String id) {
        return springRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        return springRepo.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Optional<UserProfile> findByPhone(String phone) {
        return springRepo.findByPhone(phone).map(this::toDomain);
    }

    @Override
    public boolean existsByPhoneAndUserIdNot(String phone, String userId) {
        return springRepo.existsByPhoneAndUserIdNot(phone, userId);
    }

    @Override
    public UserProfile save(UserProfile userProfile) {
        UserProfileJpaEntity entity = toEntity(userProfile);
        UserProfileJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    private UserProfile toDomain(UserProfileJpaEntity entity) {
        return new UserProfile(
                entity.getId(),
                entity.getUserId(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getGender(),
                entity.getDateOfBirth(),
                entity.getPermanentAddress(),
                entity.getAddressDetail(),
                entity.getProvinceCode(),
                entity.getWardId(),
                entity.getOccupation(),
                entity.getBio(),
                entity.getLanguage(),
                entity.getLocale(),
                entity.getTimezone(),
                entity.getAvatarKey(),
                entity.getAvatarThumbKey(),
                entity.getAvatarMediumKey(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private UserProfileJpaEntity toEntity(UserProfile domain) {
        UserProfileJpaEntity entity = new UserProfileJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setFullName(domain.getFullName());
        entity.setPhone(domain.getPhone());
        entity.setGender(domain.getGender());
        entity.setDateOfBirth(domain.getDateOfBirth());
        entity.setPermanentAddress(domain.getPermanentAddress());
        entity.setAddressDetail(domain.getAddressDetail());
        entity.setProvinceCode(domain.getProvinceCode());
        entity.setWardId(domain.getWardId());
        entity.setOccupation(domain.getOccupation());
        entity.setBio(domain.getBio());
        entity.setLanguage(domain.getLanguage());
        entity.setLocale(domain.getLocale());
        entity.setTimezone(domain.getTimezone());
        entity.setAvatarKey(domain.getAvatarKey());
        entity.setAvatarThumbKey(domain.getAvatarThumbKey());
        entity.setAvatarMediumKey(domain.getAvatarMediumKey());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

