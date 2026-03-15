package com.pixoto.notifications

/**
 * Maps notification types to Bootstrap Icons CSS classes.
 * Extensible — add new types as needed.
 */
class NotificationIconUtil {

    private static final Map<String, String> TYPE_ICONS = [
        'message'        : 'bi-chat-dots-fill',
        'mention'        : 'bi-at',
        'community_post' : 'bi-megaphone-fill',
        'community_reply': 'bi-reply-fill',
        'event_reminder' : 'bi-calendar-event-fill',
        'event_invite'   : 'bi-calendar-plus-fill',
        'share'          : 'bi-share-fill',
        'system'         : 'bi-info-circle-fill',
    ]

    static String iconForType(String type) {
        TYPE_ICONS.getOrDefault(type, 'bi-bell-fill')
    }
}
