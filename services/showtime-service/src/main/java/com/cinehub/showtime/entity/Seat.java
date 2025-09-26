package com.cinehub.showtime.entity;

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
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Seat {
    @Id
    private String id;

    private String seatNumber;
    private String rowLabel;
    private int columnIndex;
    private String type; //NORMAL, VIP, COUPlE

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
}
