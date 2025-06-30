package petitus.petcareplus.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import petitus.petcareplus.dto.response.chat.ImageUploadResponse;

@Data
@AllArgsConstructor
public class ImageUploadCompletedEvent {
    private ImageUploadResponse imageUploadResponse;
} 