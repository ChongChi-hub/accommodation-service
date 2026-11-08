package vn.cozynest.listing.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "full_name_en", length = 150)
    private String fullNameEn;

    @Column(name = "province_code", nullable = false, length = 20)
    private String provinceCode;
}

