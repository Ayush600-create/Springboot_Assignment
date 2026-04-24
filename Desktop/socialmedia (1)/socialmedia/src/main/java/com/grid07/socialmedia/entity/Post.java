package com.grid07.socialmedia.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "posts")
public class Post {

@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_type", nullable = false)
    private String authorType; 

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist  
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}