package petitus.petcareplus.controller;

import lombok.SneakyThrows;
import org.apache.coyote.BadRequestException;
import petitus.petcareplus.service.MessageSourceService;

import java.util.Arrays;

public abstract class BaseController {
    @SneakyThrows
    protected void sortColumnCheck(final MessageSourceService messageSourceService,
                                   final String[] sortColumns,
                                   final String sortBy) {
        if (sortBy != null && !Arrays.asList(sortColumns).contains(sortBy)) {
            throw new BadRequestException(messageSourceService.get("invalid_sort_column"));
        }
    }
}