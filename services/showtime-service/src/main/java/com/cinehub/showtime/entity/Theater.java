package com.cinehub.showtime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "theater")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "name_en", columnDefinition = "TEXT")
    private String nameEn;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "address_en", columnDefinition = "TEXT")
    private String addressEn;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(length = 500)
    private String theaterImageUrl;
}
