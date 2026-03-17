package com.pixoto.notifications

import grails.converters.JSON
import org.springframework.security.access.annotation.Secured

@Secured("isAuthenticated()")
class NotificationController {

    static namespace = 'notifications'

    NotificationService notificationService
    PushSubscriptionService pushSubscriptionService

    protected String getScopeId() {
        request.getAttribute('notificationsScopeId')?.toString()
    }

    protected String getUserId() {
        request.getAttribute('notificationsUserId')?.toString()
    }

    /**
     * GET /notifications — Full notification list page
     */
    def index() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        // Show all notifications for the user (no scope filter) — notifications
        // may come from different plugins with different or null scopeIds.
        List<Notification> notifications = notificationService.getNotifications(userId, null, null, 30)
        List<Map> enriched = notificationService.enrichNotifications(notifications)
        int unreadCount = notificationService.getUnreadCount(userId)
        Long oldestId = notifications ? notifications.last().id : null
        boolean hasMore = notifications.size() >= 30

        [
            notifications  : enriched,
            unreadCount    : unreadCount,
            notificationsBase: '/notifications',
            userId         : userId,
            oldestId       : oldestId,
            hasMore        : hasMore
        ]
    }

    /**
     * GET /notifications/list — JSON list for AJAX / dropdown loading
     */
    def list() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        int max = Math.min(params.int('max', 20), 50)
        Long beforeId = params.long('beforeId')

        // No scope filter — bell dropdown shows all notifications
        List<Notification> notifications = notificationService.getNotifications(userId, null, beforeId, max)
        List<Map> enriched = notificationService.enrichNotifications(notifications)

        render([
            notifications: enriched,
            hasMore      : notifications.size() >= max
        ] as JSON)
    }

    /**
     * GET /notifications/unread — Unread count for polling
     */
    def unreadCount() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        // No scope filter — badge shows total unread across all sources
        int count = notificationService.getUnreadCount(userId)
        render([count: count] as JSON)
    }

    /**
     * POST /notifications/:id/read — Mark single notification as read
     */
    def markRead() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        def id = params.id
        Notification n = notificationService.markAsRead(id, userId)
        if (!n) { render(status: 404); return }

        render([success: true, link: n.link] as JSON)
    }

    /**
     * POST /notifications/read-all — Mark all as read
     */
    def markAllRead() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        int count = notificationService.markAllAsRead(userId, getScopeId())
        render([success: true, count: count] as JSON)
    }

    /**
     * DELETE /notifications/:id — Delete a notification
     */
    def deleteNotification() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        def id = params.id
        boolean deleted = notificationService.deleteNotification(id, userId)
        if (!deleted) { render(status: 404); return }

        render([success: true] as JSON)
    }

    /**
     * GET /notifications/preferences — Get user preferences
     */
    def preferences() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        List<NotificationPreference> prefs = notificationService.getPreferences(userId, getScopeId())
        render(prefs.collect { [
            type          : it.type,
            scopeId       : it.scopeId,
            enabled       : it.enabled,
            browserEnabled: it.browserEnabled
        ] } as JSON)
    }

    /**
     * POST /notifications/preferences — Update a preference
     */
    def updatePreference() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        String type = params.type
        if (!type) { render(status: 400); return }

        Boolean enabled = params.enabled != null ? params.boolean('enabled') : null
        Boolean browserEnabled = params.browserEnabled != null ? params.boolean('browserEnabled') : null

        NotificationPreference pref = notificationService.updatePreference(
            userId, getScopeId(), type, enabled, browserEnabled
        )
        render([success: true, type: pref.type, enabled: pref.enabled, browserEnabled: pref.browserEnabled] as JSON)
    }

    /**
     * POST /notifications/push/subscribe — Register a push subscription
     */
    def pushSubscribe() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        String endpoint = params.endpoint ?: request.JSON?.endpoint
        String p256dhKey = params.p256dhKey ?: request.JSON?.p256dhKey
        String authKey = params.authKey ?: request.JSON?.authKey

        if (!endpoint || !p256dhKey || !authKey) {
            render([error: 'Missing required fields: endpoint, p256dhKey, authKey'] as JSON)
            response.status = 400
            return
        }

        PushSubscription sub = pushSubscriptionService.subscribe(
            userId, getScopeId(), endpoint, p256dhKey, authKey,
            request.getHeader('User-Agent')
        )
        render([success: true, id: sub.id] as JSON)
    }

    /**
     * POST /notifications/push/unsubscribe — Remove a push subscription
     */
    def pushUnsubscribe() {
        String userId = getUserId()
        if (!userId) { render(status: 401); return }

        String endpoint = params.endpoint ?: request.JSON?.endpoint
        if (!endpoint) {
            render([error: 'Missing required field: endpoint'] as JSON)
            response.status = 400
            return
        }

        boolean removed = pushSubscriptionService.unsubscribe(userId, endpoint)
        render([success: removed] as JSON)
    }
}
