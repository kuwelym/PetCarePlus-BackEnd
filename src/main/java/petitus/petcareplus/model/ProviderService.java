package petitus.petcareplus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "provider_services")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderService {
    @EmbeddedId
    private ProviderServiceId id;

    @ManyToOne
    @MapsId("providerId")
    @JoinColumn(name = "provider_id")
    private User provider;

    @ManyToOne
    @MapsId("serviceId")
    @JoinColumn(name = "service_id")
    private DefaultService service;

    @Column(name = "custom_price", precision = 8, scale = 2, nullable = false)
    private BigDecimal customPrice;

    @Column(name = "custom_description", columnDefinition = "TEXT")
    private String customDescription;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}