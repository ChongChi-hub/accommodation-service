package vn.cozynest.listing.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {

    private Long id;
    private String code;
    private String name;
    private String icon;
    private String category;
}
