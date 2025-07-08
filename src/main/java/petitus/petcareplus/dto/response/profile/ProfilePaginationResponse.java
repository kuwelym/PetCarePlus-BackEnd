package petitus.petcareplus.dto.response.profile;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import petitus.petcareplus.dto.response.ResponseObject;

import java.util.List;

@Getter
@Setter
public class ProfilePaginationResponse<T> extends ResponseObject {
    // List of items on the current page
    private List<T> data;

    private PagingResponse paging;

    public ProfilePaginationResponse(final Page<?> pageModel, final List<T> items) {
        this.data = items;
        this.paging = new PagingResponse(pageModel);
    }
}
