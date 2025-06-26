package petitus.petcareplus.controller.dev;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

import petitus.petcareplus.controller.BaseController;

import petitus.petcareplus.dto.request.auth.CreateAdminRequest;

import petitus.petcareplus.dto.response.user.UserResponse;
import petitus.petcareplus.model.User;

import petitus.petcareplus.service.AdminService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dev")
public class DevController extends BaseController {

    private final AdminService adminService;

    @PostMapping("/create-admin")
    @Operation(summary = "Create new admin", description = "API để admin tạo admin mới")
    public ResponseEntity<UserResponse> createAdmin(@RequestBody @Valid final CreateAdminRequest request)
            throws BindException {
        User newAdmin = adminService.createAdmin(request);
        return ResponseEntity.ok(UserResponse.convert(newAdmin));
    }
}
