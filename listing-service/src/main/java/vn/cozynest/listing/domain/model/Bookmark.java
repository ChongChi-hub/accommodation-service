package vn.cozynest.listing.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bookmark {

    private Long id;
    private String userId;
    private Long roomId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
