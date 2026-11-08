package vn.cozynest.listing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cozynest.listing.application.dto.LocationDto.*;
import vn.cozynest.listing.domain.repository.ProvinceRepository;
import vn.cozynest.listing.domain.repository.DistrictRepository;
import vn.cozynest.listing.domain.repository.WardRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;

    public List<ProvinceDto> getProvinces() {
        return provinceRepository.findAllByOrderByNameAsc().stream()
                .map(p -> ProvinceDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .fullName(p.getFullName())
                        .build())
                .collect(Collectors.toList());
    }

    public List<DistrictDto> getDistricts(String provinceCode) {
        return districtRepository.findByProvinceCodeOrderByNameAsc(provinceCode).stream()
                .map(d -> DistrictDto.builder()
                        .id(d.getId())
                        .code(d.getCode())
                        .name(d.getName())
                        .fullName(d.getFullName())
                        .provinceCode(d.getProvinceCode())
                        .build())
                .collect(Collectors.toList());
    }

    public List<WardDto> getWardsByProvince(String provinceCode) {
        return wardRepository.findByProvinceCodeOrderByNameAsc(provinceCode).stream()
                .map(w -> WardDto.builder()
                        .id(w.getId())
                        .code(w.getCode())
                        .name(w.getName())
                        .fullName(w.getFullName())
                        .provinceCode(w.getProvinceCode())
                        .districtCode(w.getDistrictCode())
                        .build())
                .collect(Collectors.toList());
    }

    public List<WardDto> getWards(String districtCode) {
        return wardRepository.findByDistrictCodeOrderByNameAsc(districtCode).stream()
                .map(w -> WardDto.builder()
                        .id(w.getId())
                        .code(w.getCode())
                        .name(w.getName())
                        .fullName(w.getFullName())
                        .provinceCode(w.getProvinceCode())
                        .districtCode(w.getDistrictCode())
                        .build())
                .collect(Collectors.toList());
    }
}

