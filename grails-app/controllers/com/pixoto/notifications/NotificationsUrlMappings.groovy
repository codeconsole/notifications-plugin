package com.pixoto.notifications

class NotificationsUrlMappings {

    static mappings = {
        // Full page
        '/notifications'(namespace: 'notifications', controller: 'notification', action: 'index')

        // JSON API
        get '/notifications/list'(namespace: 'notifications', controller: 'notification', action: 'list')
        get '/notifications/unread'(namespace: 'notifications', controller: 'notification', action: 'unreadCount')
        post '/notifications/read-all'(namespace: 'notifications', controller: 'notification', action: 'markAllRead')

        // Single notification actions
        post "/notifications/$id/read"(namespace: 'notifications', controller: 'notification', action: 'markRead')
        delete "/notifications/$id"(namespace: 'notifications', controller: 'notification', action: 'deleteNotification')

        // Preferences
        get '/notifications/preferences'(namespace: 'notifications', controller: 'notification', action: 'preferences')
        post '/notifications/preferences'(namespace: 'notifications', controller: 'notification', action: 'updatePreference')

        // Push subscription
        post '/notifications/push/subscribe'(namespace: 'notifications', controller: 'notification', action: 'pushSubscribe')
        post '/notifications/push/unsubscribe'(namespace: 'notifications', controller: 'notification', action: 'pushUnsubscribe')
    }
}
