package petitus.petcareplus.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cloudinary.Transformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Upload image to Cloudinary from MultipartFile
     */
    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        try {
            String publicId = generatePublicId(folder);
            
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder,
                    "resource_type", "image"
            );

            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            
            return result;
            
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage());
            throw new IOException("Failed to upload image to Cloudinary", e);
        }
    }

    /**
     * Upload image to Cloudinary from byte array (for WebSocket usage)
     */
    public Map<String, Object> uploadImage(byte[] imageBytes, String folder) throws IOException {
        try {
            String publicId = generatePublicId(folder);
            
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder,
                    "resource_type", "image"
            );

            Map<String, Object> result = cloudinary.uploader().upload(imageBytes, uploadParams);

            return result;
            
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage());
            throw new IOException("Failed to upload image to Cloudinary", e);
        }
    }

    /**
     * Delete image from Cloudinary
     */
    public Map<String, Object> deleteImage(String publicId) throws IOException {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Successfully deleted image from Cloudinary with public_id: {}", publicId);
            return result;
            
        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary: {}", e.getMessage());
            throw new IOException("Failed to delete image from Cloudinary", e);
        }
    }

    /**
     * Generate optimized image URL
     */
    public String generateOptimizedUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new Transformation()
                        .width(width)
                        .height(height)
                        .crop("fill")
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    /**
     * Get image URL without transformation
     */
    public String getImageUrl(String publicId) {
        return cloudinary.url().generate(publicId);
    }

    private String generatePublicId(String folder) {
        return folder + "/" + UUID.randomUUID();
    }
} 
