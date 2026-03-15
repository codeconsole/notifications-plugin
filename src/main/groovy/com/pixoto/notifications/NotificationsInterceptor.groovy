package com.pixoto.notifications

import grails.artefact.Interceptor

/**
 * Abstract interceptor that host apps must subclass to provide
 * scope and user identity for notification endpoints.
 *
 * The subclass should set two request attributes:
 *   - notificationsScopeId: tenant/community/scope identifier (String)
 *   - notificationsUserId: authenticated user's ID (String)
 *
 * Example:
 *   class MyNotificationsInterceptor extends NotificationsInterceptor {
 *       boolean before() {
 *           request.setAttribute('notificationsScopeId', resolveScopeId())
 *           request.setAttribute('notificationsUserId', resolveUserId())
 *           true
 *       }
 *   }
 */
abstract class NotificationsInterceptor implements Interceptor {

    NotificationsInterceptor() {
        match(namespace: 'notifications')
    }
}
