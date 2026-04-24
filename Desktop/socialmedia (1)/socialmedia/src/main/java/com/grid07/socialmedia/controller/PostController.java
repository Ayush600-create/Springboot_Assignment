package com.grid07.socialmedia.controller;

import com.grid07.socialmedia.entity.Comment;
import com.grid07.socialmedia.entity.Post;
import com.grid07.socialmedia.service.NotificationService;
import com.grid07.socialmedia.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final NotificationService notificationService;

    public PostController(PostService postService,
                          NotificationService notificationService) {
        this.postService = postService;
        this.notificationService = notificationService;
    }

    // ── POST /api/posts ──────────────────────────────
    // Create a new post
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody Post post) {
        try {
            Post saved = postService.createPost(post);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    // ── POST /api/posts/{postId}/like ────────────────
    // Like a post
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId) {
        try {
            String result = postService.likePost(postId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    // ── POST /api/posts/{postId}/comments ────────────
    // Add a comment to a post
    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long postId,
                                        @RequestBody Comment comment) {
        try {
            Comment saved = postService.addComment(postId, comment);

            // if bot commented → handle notification
            if ("BOT".equals(comment.getAuthorType())) {
                notificationService.handleBotNotification(
                        comment.getAuthorId(),
                        "Bot " + comment.getAuthorId()
                );
            }

            return ResponseEntity.ok(saved);

        } catch (RuntimeException e) {
            String message = e.getMessage();

            // return 429 for bot limit exceeded
            if (message.contains("BOT_LIMIT_EXCEEDED")) {
                return ResponseEntity
                        .status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(message);
            }

            // return 400 for other guardrail violations
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(message);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}