package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkStatusDto {
    private Long roomId;
    private boolean isBookmarked;
    private int bookmarkCount;
}

