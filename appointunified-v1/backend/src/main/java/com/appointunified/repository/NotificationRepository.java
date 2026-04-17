package com.appointunified.repository;

import com.appointunified.entity.Notification;
import com.appointunified.entity.Notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    /**
     * Find paginated notifications for a user, ordered by newest first
     */
    Page<Notification> findByUserIdAndIsArchivedFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    /**
     * Find unread notifications for a user
     */
    List<Notification> findByUserIdAndIsReadFalseAndIsArchivedFalseOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Get count of unread notifications for a user
     */
    long countByUserIdAndIsReadFalseAndIsArchivedFalse(UUID userId);
    
    /**
     * Find notifications by type for a user
     */
    Page<Notification> findByUserIdAndTypeAndIsArchivedFalseOrderByCreatedAtDesc(
        UUID userId, NotificationType type, Pageable pageable);
    
    List<Notification> findByUserIdAndIsArchivedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
        UUID userId,
        OffsetDateTime createdAfter);
    
    /**
     * Mark a single notification as read
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP, n.updatedAt = CURRENT_TIMESTAMP
        WHERE n.id = :id
    """)
    void markAsRead(@Param("id") UUID id);
    
    /**
     * Mark all notifications for a user as read
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP, n.updatedAt = CURRENT_TIMESTAMP
        WHERE n.user.id = :userId AND n.isRead = false AND n.isArchived = false
    """)
    void markAllAsRead(@Param("userId") UUID userId);
    
    /**
     * Archive a notification
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.isArchived = true, n.updatedAt = CURRENT_TIMESTAMP
        WHERE n.id = :id
    """)
    void archiveNotification(@Param("id") UUID id);
    
    /**
     * Archive all notifications for a user
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE Notification n
        SET n.isArchived = true, n.updatedAt = CURRENT_TIMESTAMP
        WHERE n.user.id = :userId AND n.isArchived = false
    """)
    void archiveAllNotifications(@Param("userId") UUID userId);
}
