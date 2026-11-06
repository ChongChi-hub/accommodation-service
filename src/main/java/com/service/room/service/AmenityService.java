package com.service.room.service;

import com.service.common.exception.AppException;
import com.service.room.entity.AmenityEntity;
import com.service.room.entity.RoomTypeEntity;
import com.service.room.repository.AmenityRepository;
import com.service.room.repository.RoomTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final RoomTypeRepository roomTypeRepository;

    public AmenityService(AmenityRepository amenityRepository, RoomTypeRepository roomTypeRepository) {
        this.amenityRepository = amenityRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    public List<AmenityEntity> listAmenities() {
        return amenityRepository.findAll();
    }

    public List<RoomTypeEntity> listRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public void createAmenity(String name, String icon) {
        if (name == null || name.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu tên tiện nghi");
        }

        AmenityEntity amenity = AmenityEntity.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .icon(icon)
                .build();
        amenityRepository.save(amenity);
    }
}

