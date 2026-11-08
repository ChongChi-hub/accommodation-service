package vn.cozynest.identity.interfaces.rest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.cozynest.common.dto.ApiResponse;
import vn.cozynest.identity.application.dto.LandlordKycStatusResponse;
import vn.cozynest.identity.application.usecase.LandlordKycUseCase;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/landlord/kyc")
@PreAuthorize("hasRole('LANDLORD')")
public class LandlordKycController {

    private final LandlordKycUseCase landlordKycUseCase;

    public LandlordKycController(LandlordKycUseCase landlordKycUseCase) {
        this.landlordKycUseCase = landlordKycUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<LandlordKycStatusResponse>> getKycStatus(Authentication authentication) {
        String landlordId = (String) authentication.getPrincipal();
        LandlordKycStatusResponse response = landlordKycUseCase.getKycStatus(landlordId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LandlordKycStatusResponse>> submitKyc(
            Authentication authentication,
            @RequestParam("front") MultipartFile front,
            @RequestParam("back") MultipartFile back,
            @RequestParam("fullNameOnId") String fullNameOnId,
            @RequestParam("idNumber") String idNumber,
            @RequestParam("dateOfBirth") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam("issueDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam("issuePlace") String issuePlace,
            @RequestParam("consent") boolean consent) {
        String landlordId = (String) authentication.getPrincipal();
        LandlordKycStatusResponse response = landlordKycUseCase.submitKyc(
                landlordId, front, back, fullNameOnId, idNumber, dateOfBirth, issueDate, issuePlace, consent
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nop ho so xac minh danh tinh thanh cong", response));
    }

    @GetMapping("/images/{side}")
    public ResponseEntity<byte[]> streamKycImage(
            Authentication authentication,
            @PathVariable("side") String side) {
        String landlordId = (String) authentication.getPrincipal();
        byte[] imageBytes = landlordKycUseCase.streamKycImage(landlordId, side);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("image/webp"));
        headers.set(HttpHeaders.CACHE_CONTROL, "no-store, private, max-age=0");
        headers.set("X-Content-Type-Options", "nosniff");

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }
}

