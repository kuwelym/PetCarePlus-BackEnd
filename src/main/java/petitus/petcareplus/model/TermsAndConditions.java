package petitus.petcareplus.model;

import jakarta.persistence.*;
import lombok.*;
import petitus.petcareplus.utils.enums.TermsType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "terms_and_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TermsAndConditions {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TermsType type;

    @Column(nullable = false, length = 5)
    private String language; // 'en' or 'vi'

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}