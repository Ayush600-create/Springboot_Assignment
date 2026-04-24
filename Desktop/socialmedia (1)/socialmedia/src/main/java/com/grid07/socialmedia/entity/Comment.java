package com.grid07.socialmedia.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_type", nullable = false)
    private String authorType; // "USER" or "BOT"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "depth_level", nullable = false)
    private int depthLevel = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}