package com.pixoto.notifications

import com.pixoto.notifications.api.NotificationUserProvider
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDateTime

/**
 * Core notification service. Any plugin or host app service can inject this
 * to create notifications for users.
 *
 * Usage from another plugin:
 *   @Autowired(required = false)
 *   NotificationService notificationService
 *
 *   notificationService?.notify(scopeId, userId, 'message', 'New message from Alice', 'Hey!', '/messaging/42',
 *       [sourceUserId: senderId, sourceEntityType: 'conversation', sourceEntityId: '42', iconUrl: avatarUrl])
 */
@Slf4j
@Transactional
class NotificationService {

    @Autowired(required = false)
    NotificationUserProvider notificationUserProvider

    /**
     * Create and persist a notification.
     *
     * @param scopeId       Tenant/scope identifier (nullable for global notifications)
     * @param userId        Recipient user ID
     * @param type          Notification type (e.g. 'message', 'mention', 'community_post')
     * @param title         Short title
     * @param body          Preview body text (nullable)
     * @param link          URL to navigate to on click (nullable)
     * @param opts          Optional: sourceUserId, sourceEntityType, sourceEntityId, iconUrl
     * @return              The created Notification, or null if user has disabled this type
     */
    Notification notify(String scopeId, String userId, String type, String title, String body, String link, Map opts = [:]) {
        if (!userId || !type || !title) return null

        // Check user preferences
        if (!isTypeEnabled(userId, scopeId, type)) return null

        // Coalesce: if there's an existing unread notification for the same entity, update it
        // instead of creating a duplicate (e.g., 5 messages in one conversation = 1 notification)
        Notification notification = null
        if (opts.sourceEntityType && opts.sourceEntityId) {
            notification = Notification.createCriteria().get {
                eq('userId', userId)
                eq('type', type)
                eq('sourceEntityType', opts.sourceEntityType)
                eq('sourceEntityId', opts.sourceEntityId.toString())
                eq('read', false)
            }
        }

        if (notification) {
            // Update existing — freshen the body/timestamp, bump count
            notification.coalescedCount = (notification.coalescedCount ?: 1) + 1
            notification.body = body ? truncate(body, 500) : null
            notification.iconUrl = opts.iconUrl
            notification.sourceUserId = opts.sourceUserId
            if (opts.titlePlural) notification.titlePlural = opts.titlePlural
        } else {
            notification = new Notification(
                scopeId: scopeId,
                userId: userId,
                type: type,
                title: title,
                titlePlural: opts.titlePlural,
                body: body ? truncate(body, 500) : null,
                link: link,
                iconUrl: opts.iconUrl,
                sourceUserId: opts.sourceUserId,
                sourceEntityType: opts.sourceEntityType,
                sourceEntityId: opts.sourceEntityId?.toString()
            )
        }

        notification.save(flush: true, failOnError: true)
        return notification
    }

    /**
     * Get notifications for a user with cursor-based pagination.
     *
     * @param userId    Recipient user ID
     * @param scopeId   Scope (nullable for all scopes)
     * @param beforeId  Load notifications older than this ID (cursor)
     * @param max       Maximum results to return
     * @return          List of notifications, newest first
     */
    List<Notification> getNotifications(String userId, String scopeId, Long beforeId = null, int max = 20) {
        def c = Notification.createCriteria()
        c.list(max: max) {
            eq('userId', userId)
            if (scopeId) eq('scopeId', scopeId)
            if (beforeId) lt('id', beforeId)
            order('created', 'desc')
        }
    }

    /**
     * Get the count of unread notifications.
     */
    int getUnreadCount(String userId, String scopeId = null) {
        def c = Notification.createCriteria()
        c.count {
            eq('userId', userId)
            if (scopeId) eq('scopeId', scopeId)
            eq('read', false)
        }
    }

    /**
     * Mark a single notification as read.
     */
    Notification markAsRead(Serializable notificationId, String userId) {
        Notification n = Notification.get(notificationId)
        if (!n || n.userId != userId) return null
        if (!n.read) {
            n.read = true
            n.readAt = LocalDateTime.now()
            n.save(flush: true)
        }
        return n
    }

    /**
     * Mark all notifications as read for a user in a scope.
     */
    int markAllAsRead(String userId, String scopeId = null) {
        def unread = Notification.createCriteria().list {
            eq('userId', userId)
            if (scopeId) eq('scopeId', scopeId)
            eq('read', false)
        }
        LocalDateTime now = LocalDateTime.now()
        int count = 0
        unread.each { Notification n ->
            n.read = true
            n.readAt = now
            n.save(flush: true)
            count++
        }
        return count
    }

    /**
     * Delete a notification (hard delete — notifications are ephemeral).
     */
    boolean deleteNotification(Serializable notificationId, String userId) {
        Notification n = Notification.get(notificationId)
        if (!n || n.userId != userId) return false
        n.delete(flush: true)
        return true
    }

    /**
     * Check if a notification type is enabled for a user.
     * Falls back to true if no preference is set (opt-out model).
     */
    boolean isTypeEnabled(String userId, String scopeId, String type) {
        // Check scope-specific preference first
        NotificationPreference pref = null
        if (scopeId) {
            pref = NotificationPreference.createCriteria().get {
                eq('userId', userId)
                eq('scopeId', scopeId)
                eq('type', type)
            }
        }
        // Fall back to global preference
        if (!pref) {
            pref = NotificationPreference.createCriteria().get {
                eq('userId', userId)
                isNull('scopeId')
                eq('type', type)
            }
        }
        // Default: enabled
        return pref == null || pref.enabled
    }

    /**
     * Check if browser notifications are enabled for a user and type.
     */
    boolean isBrowserEnabled(String userId, String scopeId, String type) {
        NotificationPreference pref = null
        if (scopeId) {
            pref = NotificationPreference.createCriteria().get {
                eq('userId', userId)
                eq('scopeId', scopeId)
                eq('type', type)
            }
        }
        if (!pref) {
            pref = NotificationPreference.createCriteria().get {
                eq('userId', userId)
                isNull('scopeId')
                eq('type', type)
            }
        }
        return pref != null && pref.browserEnabled
    }

    /**
     * Get all preferences for a user, optionally scoped.
     */
    List<NotificationPreference> getPreferences(String userId, String scopeId = null) {
        NotificationPreference.createCriteria().list {
            eq('userId', userId)
            if (scopeId) {
                or {
                    eq('scopeId', scopeId)
                    isNull('scopeId')
                }
            }
        }
    }

    /**
     * Update or create a notification preference.
     */
    NotificationPreference updatePreference(String userId, String scopeId, String type, Boolean enabled, Boolean browserEnabled) {
        NotificationPreference pref = NotificationPreference.createCriteria().get {
            eq('userId', userId)
            if (scopeId) eq('scopeId', scopeId)
            else isNull('scopeId')
            eq('type', type)
        }
        if (!pref) {
            pref = new NotificationPreference(userId: userId, scopeId: scopeId, type: type)
        }
        if (enabled != null) pref.enabled = enabled
        if (browserEnabled != null) pref.browserEnabled = browserEnabled
        pref.save(flush: true, failOnError: true)
        return pref
    }

    /**
     * Enrich notifications with source user information for rendering.
     */
    List<Map> enrichNotifications(List<Notification> notifications) {
        if (!notifications) return []

        // Batch load source user info
        Set<Serializable> userIds = notifications.findAll { it.sourceUserId }
            .collect { it.sourceUserId as Serializable } as Set
        Map<Serializable, ?> users = [:]
        if (userIds && notificationUserProvider) {
            users = notificationUserProvider.getUsers(userIds)
        }

        notifications.collect { Notification n ->
            def user = n.sourceUserId ? users[n.sourceUserId] : null
            int count = n.coalescedCount ?: 1
            String resolvedTitle = n.title
            if (count > 1 && n.titlePlural) {
                resolvedTitle = n.titlePlural.replace('{count}', count.toString())
            }
            [
                id            : n.id,
                type          : n.type,
                title         : resolvedTitle,
                body          : n.body,
                link          : n.link,
                iconUrl       : n.iconUrl ?: user?.avatarUrl,
                sourceUserName: user?.displayName,
                read          : n.read,
                created       : n.created?.toString(),
                timeAgo       : formatTimeAgo(n.created)
            ]
        }
    }

    private static String truncate(String text, int maxLength) {
        if (!text || text.length() <= maxLength) return text
        text.substring(0, maxLength - 1) + '\u2026'
    }

    private static String formatTimeAgo(LocalDateTime dateTime) {
        if (!dateTime) return ''
        LocalDateTime now = LocalDateTime.now()
        long minutes = java.time.Duration.between(dateTime, now).toMinutes()
        if (minutes < 1) return 'just now'
        if (minutes < 60) return "${minutes}m ago"
        long hours = minutes / 60
        if (hours < 24) return "${hours}h ago"
        long days = hours / 24
        if (days < 7) return "${days}d ago"
        long weeks = days / 7
        if (weeks < 4) return "${weeks}w ago"
        return dateTime.toLocalDate().toString()
    }
}
