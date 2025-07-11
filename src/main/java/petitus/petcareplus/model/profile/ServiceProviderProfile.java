package petitus.petcareplus.model.profile;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import petitus.petcareplus.model.AbstractBaseEntity;

import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "service_provider_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderProfile extends AbstractBaseEntity{

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    @JsonBackReference
    private Profile profile;

    @ElementCollection
    @CollectionTable(name = "service_provider_image_urls", joinColumns = @JoinColumn(name = "service_provider_profile_id"))
    @Column(name = "image_url")
    private Set<String> imageUrls;

    private String businessName;

    private String businessBio;

    @Column(name = "business_address")
    private String businessAddress;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "available_time", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> availableTime;

    @Column(name = "rating", nullable = false)
    @Builder.Default
    private Double rating = 0.0;
}
