package petitus.petcareplus.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@DiscriminatorValue("IMAGE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatImageMessage extends ChatMessage {

    // Optional caption for the image (can be null/empty)
    @Column(columnDefinition = "TEXT")
    private String caption;

    // Image-related fields
    @NotBlank(message = "Image URL is required for image messages")
    private String imageUrl;
    
    private String publicId; // Cloudinary public ID
    private String imageName;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String thumbnailUrl;
    private String mediumUrl;
    private String largeUrl;

    @Override
    public MessageType getMessageType() {
        return MessageType.IMAGE;
    }

    @Override
    public String getDisplayContent() {
        if (caption != null && !caption.trim().isEmpty()) {
            return caption;
        } else {
            return "Sent an image";
        }
    }

    @Override
    public String getContent() {
        if (caption != null && !caption.trim().isEmpty()) {
            return caption;
        }
        return super.getContent();
    }

}
