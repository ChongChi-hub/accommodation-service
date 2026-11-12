package vn.cozynest.listing.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "provinces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvinceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 100)
    private String slug;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "full_name_en", length = 150)
    private String fullNameEn;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
