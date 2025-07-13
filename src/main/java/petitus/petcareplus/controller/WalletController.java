package petitus.petcareplus.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import petitus.petcareplus.dto.response.wallet.WalletResponse;
import petitus.petcareplus.security.jwt.JwtUserDetails;
import petitus.petcareplus.service.WalletService;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "APIs for managing wallets")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Get my wallet", description = "Retrieve the wallet details of the current user")
    public ResponseEntity<WalletResponse> getMyWallet(
            @AuthenticationPrincipal JwtUserDetails userDetails) {
        WalletResponse walletResponse = walletService.getWalletByUser(userDetails.getId());
        return ResponseEntity.ok(walletResponse);
    }

    @PostMapping("/me")
    @PreAuthorize("hasAuthority('SERVICE_PROVIDER')")
    @Operation(summary = "Create my wallet", description = "Create a wallet for the current user")
    public ResponseEntity<WalletResponse> createMyWallet(
            @AuthenticationPrincipal JwtUserDetails userDetails) {

        WalletResponse walletResponse = walletService.createWallet(userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(walletResponse);
    }

}
