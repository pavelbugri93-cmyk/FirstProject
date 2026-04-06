package com.firstproject.framevalue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * UserSubmission - Temporary storage for community FPS reports (3 reports/day/game limit).
 * Validated for max 40% deviation before saving, then processed into CommunityAverage by scheduler.
 */

@Entity
@Table(name = "user_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id", nullable = false)
    private GpuModel gpu;

    @Column(nullable = false, length = 100)
    private String gameName;

    @Column(nullable = false, length = 10)
    private String mode;

    @Column(nullable = false, length = 20)
    private String resolution;

    @Column(nullable = false)
    private Integer reportedFps;

    @Column(nullable = false, length = 50)
    private String userIp;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = submittedAt.plusHours(24);
        }
    }
}