package petitus.petcareplus.dto.response.profile;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import petitus.petcareplus.dto.response.ResponseObject;

@Getter
@Setter
public class PagingResponse extends ResponseObject {
    // Current page number (1-based)
    private Integer pageNumber;

    // Total number of pages
    private Integer totalPage;

    // Number of items in a page
    private Integer pageSize;

    // Total number of items
    private Long totalItem;

    public PagingResponse(final Page<?> pageModel) {
        this.pageNumber = pageModel.getNumber() + 1;
        this.totalPage = pageModel.getTotalPages();
        this.pageSize = pageModel.getSize();
        this.totalItem = pageModel.getTotalElements();
    }
}
