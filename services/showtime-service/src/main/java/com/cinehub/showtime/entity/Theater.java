package com.cinehub.showtime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theater")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Theater {
    @Id
    private String id; //UUID

    @ManyToOne
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    private String name;
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;
}
