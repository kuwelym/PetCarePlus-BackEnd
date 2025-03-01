package petitus.petcareplus.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class PaginationResponse<T> extends ResponseObject {
    // Current page number (1-based)
    private Integer page;

    // Total number of pages
    private Integer pages;

    // Number of items in a page
    private Integer size;

    // Total number of items
    private Long total;

    // List of items on the current page
    private List<T> items;

    public PaginationResponse(final Page<?> pageModel, final List<T> items) {
        this.page = pageModel.getNumber() + 1;
        this.pages = pageModel.getTotalPages();
        this.size = pageModel.getSize();
        this.total = pageModel.getTotalElements();
        this.items = items;
    }
}