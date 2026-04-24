package com.grid07.socialmedia.service;

import com.grid07.socialmedia.entity.Comment;
import com.grid07.socialmedia.entity.Post;
import com.grid07.socialmedia.repository.CommentRepository;
import com.grid07.socialmedia.repository.PostRepository;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RedisService redisService;

    public PostService(PostRepository postRepository,
                       CommentRepository commentRepository,
                       RedisService redisService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.redisService = redisService;
    }

    // ─── Create Post ────────────────────────────────
    public Post createPost(Post post) {
        Post saved = postRepository.save(post);

        // initialize virality score in Redis
        String viralityKey = "post:" + saved.getId() + ":virality_score";
        redisService.increment(viralityKey);
        redisService.increment(viralityKey);
        // set it to 0 by deleting and not touching
        redisService.delete(viralityKey);

        return saved;
    }

    // ─── Like a Post ────────────────────────────────
    public String likePost(Long postId) {
        // Human like = +20 virality points
        String viralityKey = "post:" + postId + ":virality_score";
        Long newScore = redisService.increment(viralityKey);
        // increment 20 times or just add 20
        for (int i = 0; i < 19; i++) {
            redisService.increment(viralityKey);
        }
        return "Post liked! Virality score updated.";
    }

    // ─── Add Comment ─────────────────────────────────
    public Comment addComment(Long postId, Comment comment) {

        comment.setPostId(postId);

        // ── Guardrail 1: Vertical Cap (max depth 20) ──
        if (comment.getDepthLevel() > 20) {
            throw new RuntimeException("DEPTH_EXCEEDED: Comment thread too deep!");
        }

        // ── Guardrail 2: Horizontal Cap (max 100 bot replies) ──
        if ("BOT".equals(comment.getAuthorType())) {
            String botCountKey = "post:" + postId + ":bot_count";
            Long botCount = redisService.increment(botCountKey);

            if (botCount > 100) {
                // decrement back since we over-counted
                redisService.increment(botCountKey);
                throw new RuntimeException("BOT_LIMIT_EXCEEDED: Too many bot replies!");
            }

            // ── Guardrail 3: Cooldown Cap (10 min per bot per human) ──
            String cooldownKey = "cooldown:bot_" + comment.getAuthorId()
                                 + ":post_" + postId;
            if (redisService.exists(cooldownKey)) {
                throw new RuntimeException("COOLDOWN_ACTIVE: Bot is on cooldown!");
            }

            // set 10 minute cooldown
            redisService.setWithTTL(cooldownKey, "1", 10);

            // Bot reply = +1 virality point
            String viralityKey = "post:" + postId + ":virality_score";
            redisService.increment(viralityKey);
        }

        // ── Human Comment = +50 virality points ──
        if ("USER".equals(comment.getAuthorType())) {
            String viralityKey = "post:" + postId + ":virality_score";
            for (int i = 0; i < 50; i++) {
                redisService.increment(viralityKey);
            }
        }

        // save to PostgreSQL only if all guardrails passed
        return commentRepository.save(comment);
    }
}