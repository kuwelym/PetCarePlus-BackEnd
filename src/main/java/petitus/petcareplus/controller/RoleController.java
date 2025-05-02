package petitus.petcareplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.model.Role;
import petitus.petcareplus.service.RoleService;
import petitus.petcareplus.utils.Constants;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Các API quản lý vai trò người dùng")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/{name}")
    @Operation(
            tags = { "Role Management" },
            summary = "Get name role",
            description = "API để lấy thông tin vai trò theo tên"
    )
    public ResponseEntity<Role> getRoleByName(@PathVariable String name) {
        Constants.RoleEnum roleEnum;
        try {
            roleEnum = Constants.RoleEnum.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        Role role = roleService.findByName(roleEnum);
        return ResponseEntity.ok(role);
    }

    @GetMapping
    @Operation(
            tags = { "Role Management" },
            summary = "Get list role",
            description = "API để lấy danh sách tất cả vai trò"
    )
    public ResponseEntity<List<Role>> getRoles() {
        List<Role> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    @Operation(
            tags = { "Role Management" },
            summary = "Add role",
            description = "API để thêm vai trò mới"
    )
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        Role createdRole = roleService.create(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }
}
