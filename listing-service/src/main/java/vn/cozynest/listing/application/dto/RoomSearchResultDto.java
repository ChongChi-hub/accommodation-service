package vn.cozynest.listing.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSearchResultDto {
    private List<RoomDto> items;
    private SearchMetaDto meta;
    private RoomFacetsDto facets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetaDto {
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
    }
}

