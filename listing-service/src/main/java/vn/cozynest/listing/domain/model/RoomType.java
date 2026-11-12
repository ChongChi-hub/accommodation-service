package vn.cozynest.listing.domain.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
}
