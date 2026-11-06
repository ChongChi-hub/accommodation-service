package com.service.room.service;

import com.service.common.exception.AppException;
import com.service.room.entity.ReviewEntity;
import com.service.room.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final JdbcTemplate jdbcTemplate;

    public ReviewService(ReviewRepository reviewRepository, JdbcTemplate jdbcTemplate) {
        this.reviewRepository = reviewRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String createReview(String userId, String roomId, int rating, String comment) {
        if (reviewRepository.findByRoomIdAndTenantId(roomId, userId).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "Bạn đã đánh giá phòng này");
        }

        String reviewId = UUID.randomUUID().toString();
        ReviewEntity review = ReviewEntity.builder()
                .id(reviewId)
                .roomId(roomId)
                .tenantId(userId)
                .rating(rating)
                .comment(comment)
                .build();
        reviewRepository.save(review);
        return reviewId;
    }

    public List<Map<String, Object>> listReviews(String roomId) {
        String sql = """
            SELECT r.id, r.rating, r.comment, r.created_at,
                   u.full_name as tenant_name, u.avatar_url as tenant_avatar
            FROM reviews r
            JOIN users u ON r.tenant_id = u.id
            WHERE r.room_id = ?
            ORDER BY r.created_at DESC
        """;
        return jdbcTemplate.queryForList(sql, roomId);
    }

    @Transactional
    public void deleteReview(String reviewId, String userId, boolean isAdmin) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));

        if (!isAdmin && !review.getTenantId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xoá đánh giá này");
        }

        reviewRepository.delete(review);
    }
}

