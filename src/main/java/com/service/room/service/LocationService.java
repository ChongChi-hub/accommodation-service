package com.service.room.service;

import com.service.common.exception.AppException;
import com.service.room.entity.DistrictEntity;
import com.service.room.entity.ProvinceEntity;
import com.service.room.entity.WardEntity;
import com.service.room.repository.DistrictRepository;
import com.service.room.repository.ProvinceRepository;
import com.service.room.repository.WardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LocationService {

    private final JdbcTemplate jdbcTemplate;

    public LocationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getProvinces() {
        String sql = """
            SELECT MIN(id) as id, name, code
            FROM provinces
            GROUP BY code, name
            ORDER BY name
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getDistricts(String provinceId) {
        if (provinceId == null || provinceId.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu provinceId");
        }

        // Handle possible duplicate province codes
        List<String> provCodes = jdbcTemplate.query(
                "SELECT code FROM provinces WHERE id = ?",
                (rs, rowNum) -> rs.getString("code"),
                provinceId
        );
        if (provCodes.isEmpty()) return List.of();
        String code = provCodes.get(0);

        List<String> provIds = jdbcTemplate.query(
                "SELECT id FROM provinces WHERE code = ?",
                (rs, rowNum) -> rs.getString("id"),
                code
        );
        if (provIds.isEmpty()) return List.of();

        String inClause = String.join(",", provIds.stream().map(id -> "'" + id + "'").toList());
        String sql = String.format("""
            SELECT MIN(id) as id, name, code
            FROM districts
            WHERE province_id IN (%s)
            GROUP BY code, name
            ORDER BY name
        """, inClause);

        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getWards(String districtId) {
        if (districtId == null || districtId.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu districtId");
        }

        List<String> distCodes = jdbcTemplate.query(
                "SELECT code FROM districts WHERE id = ?",
                (rs, rowNum) -> rs.getString("code"),
                districtId
        );
        if (distCodes.isEmpty()) return List.of();
        String code = distCodes.get(0);

        List<String> distIds = jdbcTemplate.query(
                "SELECT id FROM districts WHERE code = ?",
                (rs, rowNum) -> rs.getString("id"),
                code
        );
        if (distIds.isEmpty()) return List.of();

        String inClause = String.join(",", distIds.stream().map(id -> "'" + id + "'").toList());
        String sql = String.format("""
            SELECT MIN(id) as id, name, code
            FROM wards
            WHERE district_id IN (%s)
            GROUP BY code, name
            ORDER BY name
        """, inClause);

        return jdbcTemplate.queryForList(sql);
    }

    @Transactional
    public void seedLocations() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://provinces.open-api.vn/api/?depth=3"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể kết nối đến API địa giới hành chính");
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> provData = mapper.readValue(response.body(), List.class);

            for (Map<String, Object> prov : provData) {
                String provName = (String) prov.get("name");
                String provCode = String.valueOf(prov.get("code"));
                String provId = UUID.randomUUID().toString();

                jdbcTemplate.update(
                        "INSERT IGNORE INTO provinces (id, name, code) VALUES (?, ?, ?)",
                        provId, provName, provCode
                );

                List<String> provDbIds = jdbcTemplate.query(
                        "SELECT id FROM provinces WHERE code = ?",
                        (rs, rowNum) -> rs.getString("id"),
                        provCode
                );
                if (provDbIds.isEmpty()) continue;
                String dbProvId = provDbIds.get(0);

                List<Map<String, Object>> districts = (List<Map<String, Object>>) prov.get("districts");
                if (districts != null && !districts.isEmpty()) {
                    for (Map<String, Object> dist : districts) {
                        String distName = (String) dist.get("name");
                        String distCode = String.valueOf(dist.get("code"));
                        String distId = UUID.randomUUID().toString();

                        jdbcTemplate.update(
                                "INSERT IGNORE INTO districts (id, province_id, name, code) VALUES (?, ?, ?, ?)",
                                distId, dbProvId, distName, distCode
                        );

                        List<String> distDbIds = jdbcTemplate.query(
                                "SELECT id FROM districts WHERE code = ? AND province_id = ?",
                                (rs, rowNum) -> rs.getString("id"),
                                distCode, dbProvId
                        );
                        if (distDbIds.isEmpty()) continue;
                        String dbDistId = distDbIds.get(0);

                        List<Map<String, Object>> wards = (List<Map<String, Object>>) dist.get("wards");
                        if (wards != null && !wards.isEmpty()) {
                            List<Object[]> batchWards = new ArrayList<>();
                            for (Map<String, Object> ward : wards) {
                                batchWards.add(new Object[]{
                                        UUID.randomUUID().toString(),
                                        dbDistId,
                                        ward.get("name"),
                                        String.valueOf(ward.get("code"))
                                });
                            }
                            jdbcTemplate.batchUpdate(
                                    "INSERT IGNORE INTO wards (id, district_id, name, code) VALUES (?, ?, ?, ?)",
                                    batchWards
                            );
                        }
                    }
                }
            }
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi nạp dữ liệu địa giới: " + e.getMessage());
        }
    }
}

