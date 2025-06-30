package petitus.petcareplus.dto.request.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadRequest {
    
    private UUID senderId;
    
    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    @NotBlank(message = "Image name is required")
    private String imageName;

    private String mimeType; // image/jpeg, image/png, etc.

    @NotBlank(message = "Image data is required")
    @Size(max = 7000000, message = "Image data too large (max ~5MB after decoding)")
    private String imageData; // Base64 encoded image data

    private String caption; // Optional caption for the image
}
