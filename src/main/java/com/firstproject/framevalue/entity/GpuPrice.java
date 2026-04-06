package com.firstproject.framevalue.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GpuPrice - Tracks GPU price changes from Israeli retailers.
 * Updated weekly by PriceScraperScheduler with percentage change calculation.
 */

@Entity
@Table(name = "gpu_prices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpuPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id", nullable = false)
    private GpuModel gpu;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal previousPrice;

    @Column
    private Double priceChangePercent;

    @Column(length = 500)
    private String sourceUrl;

    @Column(length = 500)
    private String israelProductUrl;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}