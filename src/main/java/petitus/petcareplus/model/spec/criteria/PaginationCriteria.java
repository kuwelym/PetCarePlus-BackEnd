package petitus.petcareplus.model.spec.criteria;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationCriteria  {
    private Integer page;

    private Integer size;

    private String sortBy;

    private String sort;

    private String[] columns;
}
