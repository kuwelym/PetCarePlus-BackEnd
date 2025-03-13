package petitus.petcareplus.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import petitus.petcareplus.controller.BaseController;
import petitus.petcareplus.dto.request.auth.UpdateUserRequest;
import petitus.petcareplus.dto.response.PaginationResponse;
import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.UserCriteria;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.service.UserService;
import petitus.petcareplus.utils.Constants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing user profiles, preferences and settings")
public class UserController extends BaseController {
        private static final String[] SORT_COLUMNS = new String[] { "id", "email", "name", "lastName", "blockedAt",
                        "createdAt", "updatedAt", "deletedAt" };

        private final UserService userService;

        private final MessageSourceService messageSourceService;

        @GetMapping
        public ResponseEntity<PaginationResponse<UserResponse>> list(
                        @RequestParam(required = false) final List<String> roles,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtStart,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtEnd,

                        @RequestParam(required = false) final Boolean isBlocked,

                        @RequestParam(required = false) final String query,

                        @RequestParam(defaultValue = "1", required = false) final Integer page,

                        @RequestParam(defaultValue = "10", required = false) final Integer size,

                        @RequestParam(defaultValue = "createdAt", required = false) final String sortBy,

                        @RequestParam(defaultValue = "asc", required = false) @Pattern(regexp = "asc|desc") final String sort

        ) throws BadRequestException {
                sortColumnCheck(messageSourceService, SORT_COLUMNS, sortBy);

                Page<User> users = userService.findAll(
                                UserCriteria.builder()
                                                .roles(roles != null ? roles.stream().map(Constants.RoleEnum::get)
                                                                .collect(Collectors.toList()) : null)
                                                .createdAtStart(createdAtStart)
                                                .createdAtEnd(createdAtEnd)
                                                .isBlocked(isBlocked)
                                                .query(query)
                                                .build(),
                                PaginationCriteria.builder()
                                                .page(page)
                                                .size(size)
                                                .sortBy(sortBy)
                                                .sort(sort)
                                                .columns(SORT_COLUMNS)
                                                .build());

                return ResponseEntity.ok(new PaginationResponse<>(users, users.getContent().stream()
                                .map(UserResponse::convert)
                                .collect(Collectors.toList())));
        }

        @GetMapping("/{id}")
        public ResponseEntity<UserResponse> get(@PathVariable final String id) throws BadRequestException {
                return ResponseEntity.ok(UserResponse.convert(userService.findById(id)));
        }

        @PutMapping("/{id}")
        public ResponseEntity<UserResponse> update(@PathVariable final String id,
                        @RequestBody @Valid final UpdateUserRequest user) throws BindException {
                return ResponseEntity.ok(UserResponse.convert(userService.update(id, user)));
        }
}
