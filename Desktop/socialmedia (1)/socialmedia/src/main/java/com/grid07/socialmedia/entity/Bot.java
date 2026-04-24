package com.grid07.socialmedia.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "bots")
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "persona_description", columnDefinition = "TEXT")
    private String personaDescription;
}