package com.pixoto.notifications

import grails.artefact.Interceptor
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Default interceptor that resolves the authenticated user's ID from Spring Security.
 * Sets notificationsUserId from the principal's id property.
 *
 * Works out of the box with any Spring Security setup where the principal has an 'id' property.
 * Host apps can override by registering a bean named 'notificationsInterceptor' in doWithSpring().
 */
class NotificationsInterceptor implements Interceptor {

    NotificationsInterceptor() {
        match(namespace: 'notifications')
    }

    @Override
    boolean before() {
        def principal = SecurityContextHolder.context.authentication?.principal
        if (principal?.hasProperty('id')) {
            request.setAttribute('notificationsUserId', principal.id?.toString())
        }
        return true
    }

    @Override
    boolean after() { true }

    @Override
    void afterView() {}
}
