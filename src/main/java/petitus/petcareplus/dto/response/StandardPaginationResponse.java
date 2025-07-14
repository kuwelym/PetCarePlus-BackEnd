package petitus.petcareplus.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class StandardPaginationResponse<T> extends ResponseObject {
    // List of items on the current page
    private List<T> data;

    private PagingInfo paging;

    public StandardPaginationResponse(final Page<?> pageModel, final List<T> items) {
        this.data = items;
        this.paging = new PagingInfo(pageModel);
    }

    @Getter
    @Setter
    public static class PagingInfo {
        // Current page number (1-based)
        private Integer pageNumber;

        // Total number of pages
        private Integer totalPage;

        // Number of items in a page
        private Integer pageSize;

        // Total number of items
        private Long totalItem;

        public PagingInfo(final Page<?> pageModel) {
            this.pageNumber = pageModel.getNumber() + 1;
            this.totalPage = pageModel.getTotalPages();
            this.pageSize = pageModel.getSize();
            this.totalItem = pageModel.getTotalElements();
        }
    }
}