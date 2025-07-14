package petitus.petcareplus.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.controller.BaseController;
import petitus.petcareplus.dto.request.auth.ChangeUserRoleRequest;
import petitus.petcareplus.dto.request.auth.UpdateUserRequest;
import petitus.petcareplus.dto.response.StandardPaginationResponse;
import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.model.User;
import petitus.petcareplus.model.spec.criteria.PaginationCriteria;
import petitus.petcareplus.model.spec.criteria.UserCriteria;
import petitus.petcareplus.service.AdminService;
import petitus.petcareplus.service.MessageSourceService;
import petitus.petcareplus.service.UserService;
import petitus.petcareplus.utils.Constants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@Tag(name = "Admin", description = "APIs for Admin")
public class AdminUserController extends BaseController {
        private final String[] SORT_COLUMNS = new String[] { "id", "email", "name", "lastName", "blockedAt",
                        "createdAt", "updatedAt", "deletedAt" };

        private final UserService userService;
        private final AdminService adminService;

        private final MessageSourceService messageSourceService;

        @GetMapping
        @Operation(summary = "Get all users", description = "API để toàn bộ người dùng", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<StandardPaginationResponse<UserResponse>> list(
                        @RequestParam(required = false) final List<String> roles,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtStart,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime createdAtEnd,

                        @RequestParam(required = false) final Boolean isBlocked,

                        @RequestParam(required = false) final Boolean isEmailActivated,

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
                                                .isEmailActivated(isEmailActivated)
                                                .query(query)
                                                .build(),
                                PaginationCriteria.builder()
                                                .page(page)
                                                .size(size)
                                                .sortBy(sortBy)
                                                .sort(sort)
                                                .columns(SORT_COLUMNS)
                                                .build());

                StandardPaginationResponse<UserResponse> response = new StandardPaginationResponse<>(
                                users,
                                users.getContent().stream()
                                                .map(UserResponse::convert)
                                                .collect(Collectors.toList()));

                return ResponseEntity.ok(response);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get an user", description = "API để lấy thông tin một người dùng", security = {
                        @SecurityRequirement(name = "bearerAuth") })
        public ResponseEntity<UserResponse> get(@PathVariable final String id) throws BadRequestException {
                return ResponseEntity.ok(UserResponse.convert(userService.findById(id)));
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update information user", description = "Cập nhật thông tin người dùng", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<UserResponse> update(@PathVariable final String id,
                        @RequestBody @Valid final UpdateUserRequest user) throws BindException {
                return ResponseEntity.ok(UserResponse.convert(userService.update(id, user)));
        }

        @PutMapping("/{id}/role")
        @Operation(summary = "Change user role", description = "API để admin đổi role của user")
        @PreAuthorize("hasAuthority('ADMIN')")
        public ResponseEntity<UserResponse> changeUserRole(
                        @PathVariable final String id,
                        @RequestBody @Valid final ChangeUserRoleRequest request) {

                User updatedUser = adminService.changeUserRole(id, request.getRole());
                return ResponseEntity.ok(UserResponse.convert(updatedUser));
        }

        @PatchMapping("/{id}/block")
        @Operation(summary = "Block user", description = "API để admin block user")
        @PreAuthorize("hasAuthority('ADMIN')")
        public ResponseEntity<UserResponse> blockUser(
                        @PathVariable final String id,
                        // add reason if needed
                        @RequestBody(required = false) String reason) {

                User updatedUser = adminService.blockUser(id);
                return ResponseEntity.ok(UserResponse.convert(updatedUser));
        }

        @PatchMapping("/{id}/unblock")
        @Operation(summary = "Unblock user", description = "API để admin unblock user")
        @PreAuthorize("hasAuthority('ADMIN')")
        public ResponseEntity<UserResponse> unblockUser(
                        @PathVariable final String id,
                        @RequestBody(required = false) String reason) {

                User updatedUser = adminService.unblockUser(id);
                return ResponseEntity.ok(UserResponse.convert(updatedUser));
        }
}
