package petitus.petcareplus.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ImageUploadErrorEvent {
    private UUID userId;
    private String errorMessage;
} 