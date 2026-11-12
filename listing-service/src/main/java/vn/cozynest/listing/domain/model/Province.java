package vn.cozynest.listing.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Province {

    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String slug;
    private String fullName;
    private String fullNameEn;
    private String imageUrl;
}
