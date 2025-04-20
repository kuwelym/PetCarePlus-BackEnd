package petitus.petcareplus.dto.request.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ChatMessageRequest {
    
    @NotNull
    private UUID recipientId;
    
    @NotBlank
    private String content;
} 