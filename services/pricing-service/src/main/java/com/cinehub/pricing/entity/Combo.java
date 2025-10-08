package com.cinehub.pricing.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "combo")
public class Combo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "combo_name")
    private String comboName; 

    private BigDecimal price;    

    private String description;   
    
    private String type;           // "COMBO", "FOOD", "DRINK"
}
