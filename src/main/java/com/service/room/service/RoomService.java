package com.service.room.service;

import com.service.common.dto.PaginationResponse;
import com.service.common.exception.AppException;
import com.service.room.dto.CreateRoomRequest;
import com.service.room.dto.RoomPriceDto;
import com.service.room.entity.RoomEntity;
import com.service.room.entity.RoomImageEntity;
import com.service.room.entity.RoomPriceEntity;
import com.service.room.repository.RoomImageRepository;
import com.service.room.repository.RoomPriceRepository;
import com.service.room.repository.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

@Service
public class RoomService {

    private final JdbcTemplate jdbcTemplate;
    private final RoomRepository roomRepository;
    private final RoomImageRepository imageRepository;
    private final RoomPriceRepository priceRepository;

    public RoomService(JdbcTemplate jdbcTemplate,
                       RoomRepository roomRepository,
                       RoomImageRepository imageRepository,
                       RoomPriceRepository priceRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.roomRepository = roomRepository;
        this.imageRepository = imageRepository;
        this.priceRepository = priceRepository;
    }

    private String generateSlug(String title) {
        String slug = Normalizer.normalize(title.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d")
                .replaceAll("Đ", "D")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
        return slug + "-" + Long.toString(System.currentTimeMillis(), 36);
    }

    public Map<String, Object> listRooms(
            int page, int limit, String search,
            String roomTypeId, Double priceMin, Double priceMax,
            Double areaMin, Double areaMax,
            String wardId, String districtId, String provinceId,
            Integer maxOccupants, String allowPet, String allowCooking,
            String amenities, String sortBy
    ) {
        int offset = (page - 1) * limit;
        List<String> conditions = new ArrayList<>();
        conditions.add("r.status = 'available'");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            conditions.add("(r.title LIKE ? OR r.address LIKE ? OR w.name LIKE ? OR d.name LIKE ? OR p.name LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (roomTypeId != null && !roomTypeId.isBlank()) {
            conditions.add("r.room_type_id = ?");
            params.add(roomTypeId);
        }
        if (priceMin != null) {
            conditions.add("r.price >= ?");
            params.add(priceMin);
        }
        if (priceMax != null) {
            conditions.add("r.price <= ?");
            params.add(priceMax);
        }
        if (areaMin != null) {
            conditions.add("r.area >= ?");
            params.add(areaMin);
        }
        if (areaMax != null) {
            conditions.add("r.area <= ?");
            params.add(areaMax);
        }
        if (maxOccupants != null) {
            conditions.add("r.max_occupants >= ?");
            params.add(maxOccupants);
        }
        if ("1".equals(allowPet) || "true".equalsIgnoreCase(allowPet)) {
            conditions.add("r.allow_pet = 1");
        }
        if ("1".equals(allowCooking) || "true".equalsIgnoreCase(allowCooking)) {
            conditions.add("r.allow_cooking = 1");
        }

        if (wardId != null && !wardId.isBlank()) {
            conditions.add("r.ward_id = ?");
            params.add(wardId);
        } else if (districtId != null && !districtId.isBlank()) {
            conditions.add("r.ward_id IN (SELECT id FROM wards WHERE district_id = ?)");
            params.add(districtId);
        } else if (provinceId != null && !provinceId.isBlank()) {
            conditions.add("r.ward_id IN (SELECT w.id FROM wards w JOIN districts d ON w.district_id = d.id WHERE d.province_id = ?)");
            params.add(provinceId);
        }

        if (amenities != null && !amenities.isBlank()) {
            String[] amenityList = amenities.split(",");
            String placeholders = String.join(",", Collections.nCopies(amenityList.length, "?"));
            conditions.add(String.format("""
                r.id IN (
                    SELECT ra.room_id FROM room_amenities ra
                    WHERE ra.amenity_id IN (%s)
                    GROUP BY ra.room_id HAVING COUNT(DISTINCT ra.amenity_id) = ?
                )
            """, placeholders));
            Collections.addAll(params, amenityList);
            params.add(amenityList.length);
        }

        String whereClause = "WHERE " + String.join(" AND ", conditions);

        String orderClause = switch (sortBy != null ? sortBy : "newest") {
            case "price_asc" -> "ORDER BY r.price ASC";
            case "price_desc" -> "ORDER BY r.price DESC";
            default -> "ORDER BY r.created_at DESC";
        };

        // Total count
        String countSql = String.format("""
            SELECT COUNT(*) as total FROM rooms r
            JOIN wards w ON r.ward_id = w.id
            JOIN districts d ON w.district_id = d.id
            JOIN provinces p ON d.province_id = p.id
            %s
        """, whereClause);

        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (total == null) total = 0L;

        // Query rooms
        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(limit);
        queryParams.add(offset);

        String selectSql = String.format("""
            SELECT r.id, r.title, r.slug, r.price, r.deposit, r.area, r.max_occupants,
                   r.address, r.latitude, r.longitude,
                   r.allow_pet, r.allow_cooking, r.live_with_owner,
                   r.available_from, r.status, r.created_at,
                   rt.name as room_type_name, rt.slug as room_type_slug,
                   u.id as landlord_id, u.full_name as landlord_name, u.avatar_url as landlord_avatar,
                   u.phone as landlord_phone, u.is_verified as landlord_verified,
                   w.name as ward_name, d.name as district_name, p.name as province_name,
                   (SELECT url FROM room_images ri WHERE ri.room_id = r.id AND ri.is_cover = 1 LIMIT 1) as cover_image
            FROM rooms r
            JOIN users u ON r.landlord_id = u.id
            JOIN room_types rt ON r.room_type_id = rt.id
            JOIN wards w ON r.ward_id = w.id
            JOIN districts d ON w.district_id = d.id
            JOIN provinces p ON d.province_id = p.id
            %s
            %s
            LIMIT ? OFFSET ?
        """, whereClause, orderClause);

        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(selectSql, queryParams.toArray());

        // Attach images, amenities, rating for each room
        for (Map<String, Object> room : rooms) {
            String roomId = (String) room.get("id");

            List<String> images = jdbcTemplate.query(
                    "SELECT url FROM room_images WHERE room_id = ? ORDER BY sort_order LIMIT 5",
                    (rs, rowNum) -> rs.getString("url"),
                    roomId
            );
            room.put("images", images);
            if (room.get("cover_image") == null && !images.isEmpty()) {
                room.put("cover_image", images.get(0));
            }

            List<Map<String, Object>> roomAmenities = jdbcTemplate.queryForList(
                    "SELECT a.id, a.name, a.icon FROM room_amenities ra JOIN amenities a ON ra.amenity_id = a.id WHERE ra.room_id = ?",
                    roomId
            );
            room.put("amenities", roomAmenities);

            Map<String, Object> ratingMap = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(AVG(rating), 0) as avg_rating, COUNT(*) as review_count FROM reviews WHERE room_id = ?",
                    roomId
            );
            room.put("avgRating", String.format("%.1f", Double.parseDouble(ratingMap.get("avg_rating").toString())));
            room.put("reviewCount", ((Number) ratingMap.get("review_count")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", rooms);
        result.put("pagination", PaginationResponse.of(page, limit, total));
        return result;
    }

    public Map<String, Object> getRoomById(String id) {
        String sql = """
            SELECT r.*,
                   rt.name as room_type_name, rt.slug as room_type_slug,
                   u.id as landlord_id_u, u.full_name as landlord_name, u.avatar_url as landlord_avatar,
                   u.phone as landlord_phone, u.email as landlord_email, u.is_verified as landlord_verified,
                   w.name as ward_name, d.name as district_name, p.name as province_name
            FROM rooms r
            JOIN users u ON r.landlord_id = u.id
            JOIN room_types rt ON r.room_type_id = rt.id
            JOIN wards w ON r.ward_id = w.id
            JOIN districts d ON w.district_id = d.id
            JOIN provinces p ON d.province_id = p.id
            WHERE r.id = ?
        """;

        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(sql, id);
        if (rooms.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng");
        }

        Map<String, Object> r = rooms.get(0);

        List<Map<String, Object>> images = jdbcTemplate.queryForList(
                "SELECT id, url, is_cover, sort_order FROM room_images WHERE room_id = ? ORDER BY sort_order",
                id
        );

        List<Map<String, Object>> amenities = jdbcTemplate.queryForList(
                "SELECT a.id, a.name, a.icon FROM room_amenities ra JOIN amenities a ON ra.amenity_id = a.id WHERE ra.room_id = ?",
                id
        );

        List<Map<String, Object>> prices = jdbcTemplate.queryForList(
                "SELECT id, label, price, unit, is_metered, meter_type FROM room_prices WHERE room_id = ?",
                id
        );

        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(
                """
                SELECT rv.id, rv.rating, rv.comment, rv.created_at,
                       u.full_name as tenant_name, u.avatar_url as tenant_avatar
                FROM reviews rv JOIN users u ON rv.tenant_id = u.id
                WHERE rv.room_id = ? ORDER BY rv.created_at DESC LIMIT 10
                """,
                id
        );

        Map<String, Object> ratingMap = jdbcTemplate.queryForMap(
                "SELECT COALESCE(AVG(rating), 0) as avg_rating, COUNT(*) as review_count FROM reviews WHERE room_id = ?",
                id
        );

        // Build room response
        Map<String, Object> roomData = new LinkedHashMap<>();
        roomData.put("id", r.get("id"));
        roomData.put("title", r.get("title"));
        roomData.put("slug", r.get("slug"));
        roomData.put("description", r.get("description"));
        roomData.put("address", r.get("address"));
        roomData.put("latitude", r.get("latitude"));
        roomData.put("longitude", r.get("longitude"));
        roomData.put("area", r.get("area"));
        roomData.put("price", r.get("price"));
        roomData.put("deposit", r.get("deposit"));
        roomData.put("maxOccupants", r.get("max_occupants"));
        roomData.put("availableFrom", r.get("available_from"));
        roomData.put("allowPet", Boolean.TRUE.equals(r.get("allow_pet")) || Integer.valueOf(1).equals(r.get("allow_pet")));
        roomData.put("allowCooking", Boolean.TRUE.equals(r.get("allow_cooking")) || Integer.valueOf(1).equals(r.get("allow_cooking")));
        roomData.put("liveWithOwner", Boolean.TRUE.equals(r.get("live_with_owner")) || Integer.valueOf(1).equals(r.get("live_with_owner")));
        roomData.put("curfewTime", r.get("curfew_time"));
        roomData.put("extraRules", r.get("extra_rules"));
        roomData.put("status", r.get("status"));
        roomData.put("createdAt", r.get("created_at"));
        roomData.put("updatedAt", r.get("updated_at"));

        roomData.put("roomType", Map.of(
                "name", r.get("room_type_name"),
                "slug", r.get("room_type_slug")
        ));
        roomData.put("ward", r.get("ward_name"));
        roomData.put("district", r.get("district_name"));
        roomData.put("province", r.get("province_name"));

        roomData.put("landlord", Map.of(
                "id", r.get("landlord_id_u"),
                "fullName", r.get("landlord_name"),
                "avatar", r.get("landlord_avatar") != null ? r.get("landlord_avatar") : "",
                "phone", r.get("landlord_phone"),
                "email", r.get("landlord_email"),
                "isVerified", Boolean.TRUE.equals(r.get("landlord_verified")) || Integer.valueOf(1).equals(r.get("landlord_verified"))
        ));

        roomData.put("images", images);
        roomData.put("amenities", amenities);
        roomData.put("prices", prices);
        roomData.put("reviews", reviews);
        roomData.put("avgRating", String.format("%.1f", Double.parseDouble(ratingMap.get("avg_rating").toString())));
        roomData.put("reviewCount", ((Number) ratingMap.get("review_count")).intValue());

        return roomData;
    }

    public List<Map<String, Object>> getMyRooms(String landlordId) {
        String sql = """
            SELECT r.*,
                   rt.name as room_type_name,
                   w.name as ward_name, d.name as district_name, p.name as province_name,
                   (SELECT url FROM room_images ri WHERE ri.room_id = r.id AND ri.is_cover = 1 LIMIT 1) as cover_image
            FROM rooms r
            JOIN room_types rt ON r.room_type_id = rt.id
            JOIN wards w ON r.ward_id = w.id
            JOIN districts d ON w.district_id = d.id
            JOIN provinces p ON d.province_id = p.id
            WHERE r.landlord_id = ?
            ORDER BY r.created_at DESC
        """;
        return jdbcTemplate.queryForList(sql, landlordId);
    }

    public List<Map<String, Object>> getRecommendations(String roomId) {
        String sql = """
            SELECT r.id, r.title, r.price, r.area, r.address,
                   (SELECT url FROM room_images ri WHERE ri.room_id = r.id AND ri.is_cover = 1 LIMIT 1) as cover_image
            FROM rooms r
            WHERE r.id != ? AND r.status = 'available'
            LIMIT 4
        """;
        return jdbcTemplate.queryForList(sql, roomId);
    }

    @Transactional
    public String createRoom(String landlordId, String userRole, CreateRoomRequest request) {
        // Enforce KYC check for landlord
        if ("landlord".equals(userRole)) {
            String kycStatus = jdbcTemplate.queryForObject(
                    "SELECT kyc_status FROM users WHERE id = ?",
                    String.class,
                    landlordId
            );
            if (!"approved".equals(kycStatus)) {
                throw new AppException(HttpStatus.FORBIDDEN, "Bạn cần được xác thực danh tính (KYC) trước khi đăng phòng");
            }
        }

        String roomId = UUID.randomUUID().toString();
        String slug = generateSlug(request.getTitle());

        RoomEntity room = RoomEntity.builder()
                .id(roomId)
                .landlordId(landlordId)
                .roomTypeId(request.getRoomTypeId())
                .wardId(request.getWardId())
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription() != null ? request.getDescription() : "")
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .area(request.getArea())
                .price(request.getPrice())
                .deposit(request.getDeposit() != null ? request.getDeposit() : request.getPrice())
                .maxOccupants(request.getMaxOccupants() != null ? request.getMaxOccupants() : 1)
                .allowPet(Boolean.TRUE.equals(request.getAllowPet()))
                .allowCooking(Boolean.TRUE.equals(request.getAllowCooking()))
                .liveWithOwner(Boolean.TRUE.equals(request.getLiveWithOwner()))
                .curfewTime(request.getCurfewTime())
                .extraRules(request.getExtraRules())
                .availableFrom(request.getAvailableFrom())
                .status("available")
                .viewCount(0)
                .build();

        roomRepository.save(room);

        // Images
        if (request.getImages() != null) {
            for (int i = 0; i < request.getImages().size(); i++) {
                String imgUrl = request.getImages().get(i);
                RoomImageEntity image = RoomImageEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .roomId(roomId)
                        .url(imgUrl)
                        .isCover(i == 0)
                        .sortOrder(i)
                        .build();
                imageRepository.save(image);
            }
        }

        // Amenities
        List<String> amenities = request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()
                ? request.getAmenityIds()
                : (request.getAmenities() != null ? request.getAmenities() : List.of());

        for (String amenityId : amenities) {
            jdbcTemplate.update("INSERT IGNORE INTO room_amenities (room_id, amenity_id) VALUES (?, ?)", roomId, amenityId);
        }

        // Prices / Utility services
        if (request.getPrices() != null) {
            for (RoomPriceDto priceDto : request.getPrices()) {
                RoomPriceEntity price = RoomPriceEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .roomId(roomId)
                        .label(priceDto.getLabel())
                        .price(priceDto.getPrice())
                        .unit(priceDto.getUnit())
                        .isMetered(Boolean.TRUE.equals(priceDto.getIsMetered()))
                        .meterType(priceDto.getMeterType())
                        .build();
                priceRepository.save(price);
            }
        }

        return roomId;
    }

    @Transactional
    public void updateRoom(String roomId, String userId, boolean isAdmin, CreateRoomRequest request) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        if (!isAdmin && !room.getLandlordId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền sửa phòng này");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            room.setTitle(request.getTitle());
            room.setSlug(generateSlug(request.getTitle()));
        }
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getRoomTypeId() != null) room.setRoomTypeId(request.getRoomTypeId());
        if (request.getWardId() != null) room.setWardId(request.getWardId());
        if (request.getAddress() != null) room.setAddress(request.getAddress());
        if (request.getLatitude() != null) room.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) room.setLongitude(request.getLongitude());
        if (request.getArea() != null) room.setArea(request.getArea());
        if (request.getPrice() != null) room.setPrice(request.getPrice());
        if (request.getDeposit() != null) room.setDeposit(request.getDeposit());
        if (request.getMaxOccupants() != null) room.setMaxOccupants(request.getMaxOccupants());
        if (request.getAllowPet() != null) room.setAllowPet(request.getAllowPet());
        if (request.getAllowCooking() != null) room.setAllowCooking(request.getAllowCooking());
        if (request.getLiveWithOwner() != null) room.setLiveWithOwner(request.getLiveWithOwner());
        if (request.getCurfewTime() != null) room.setCurfewTime(request.getCurfewTime());
        if (request.getExtraRules() != null) room.setExtraRules(request.getExtraRules());
        if (request.getAvailableFrom() != null) room.setAvailableFrom(request.getAvailableFrom());

        roomRepository.save(room);

        if (request.getAmenities() != null) {
            jdbcTemplate.update("DELETE FROM room_amenities WHERE room_id = ?", roomId);
            for (String amenityId : request.getAmenities()) {
                jdbcTemplate.update("INSERT INTO room_amenities (room_id, amenity_id) VALUES (?, ?)", roomId, amenityId);
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            jdbcTemplate.update("DELETE FROM room_images WHERE room_id = ?", roomId);
            List<RoomImageEntity> images = new ArrayList<>();
            for (int i = 0; i < request.getImages().size(); i++) {
                images.add(RoomImageEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .roomId(roomId)
                        .url(request.getImages().get(i))
                        .isCover(i == 0)
                        .sortOrder(i)
                        .build());
            }
            imageRepository.saveAll(images);
        }

        if (request.getPrices() != null && !request.getPrices().isEmpty()) {
            jdbcTemplate.update("DELETE FROM room_prices WHERE room_id = ?", roomId);
            List<RoomPriceEntity> prices = new ArrayList<>();
            for (RoomPriceDto p : request.getPrices()) {
                prices.add(RoomPriceEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .roomId(roomId)
                        .label(p.getLabel())
                        .price(p.getPrice())
                        .unit(p.getUnit())
                        .isMetered(Boolean.TRUE.equals(p.getIsMetered()))
                        .meterType(p.getMeterType())
                        .build());
            }
            priceRepository.saveAll(prices);
        }
    }

    @Transactional
    public void deleteRoom(String roomId, String userId, boolean isAdmin) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        if (!isAdmin && !room.getLandlordId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xoá phòng này");
        }

        imageRepository.deleteByRoomId(roomId);
        priceRepository.deleteByRoomId(roomId);
        jdbcTemplate.update("DELETE FROM room_amenities WHERE room_id = ?", roomId);
        roomRepository.delete(room);
    }

    @Transactional
    public void updateRoomStatus(String roomId, String userId, boolean isAdmin, String status) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng"));

        if (!isAdmin && !room.getLandlordId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền sửa trạng thái phòng này");
        }

        room.setStatus(status);
        roomRepository.save(room);
    }
}

