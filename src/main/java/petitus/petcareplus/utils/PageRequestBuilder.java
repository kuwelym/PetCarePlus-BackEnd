package petitus.petcareplus.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import petitus.petcareplus.exceptions.BadRequestException;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.service.MessageSourceService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PageRequestBuilder {
    private static MessageSourceService messageSourceService;

    @Autowired
    public PageRequestBuilder(MessageSourceService messageSourceService) {
        PageRequestBuilder.messageSourceService = messageSourceService;
    }
    public static PageRequest build(final PaginationCriteria paginationCriteria) {
        if (paginationCriteria.getPage() == null || paginationCriteria.getPage() < 1) {
            throw new BadRequestException(messageSourceService.get("invalid_page"));
        }

        paginationCriteria.setPage(paginationCriteria.getPage() - 1);

        if (paginationCriteria.getSize() == null || paginationCriteria.getSize() < 1) {
            throw new BadRequestException(messageSourceService.get("invalid_page_size"));
        }

        PageRequest pageRequest = PageRequest.of(paginationCriteria.getPage(), paginationCriteria.getSize());

        if (paginationCriteria.getSortBy() != null && paginationCriteria.getSort() != null) {
            Sort.Direction direction = getDirection(paginationCriteria.getSort());

            List<String> columnsList = new ArrayList<>(Arrays.asList(paginationCriteria.getColumns()));
            if (columnsList.contains(paginationCriteria.getSortBy())) {
                return pageRequest.withSort(Sort.by(direction, paginationCriteria.getSortBy()));
            }
        }

        return pageRequest;
    }

    private static Sort.Direction getDirection(final String sort) {
        if ("desc".equalsIgnoreCase(sort)) {
            return Sort.Direction.DESC;
        }

        return Sort.Direction.ASC;
    }
}