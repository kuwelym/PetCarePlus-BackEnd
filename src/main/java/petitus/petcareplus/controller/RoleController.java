package petitus.petcareplus.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import petitus.petcareplus.model.Role;
import petitus.petcareplus.service.RoleService;
import petitus.petcareplus.utils.Constants;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/{name}")
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
    public ResponseEntity<List<Role>> getRoles() {
        List<Role> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        Role createdRole = roleService.create(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }
}
