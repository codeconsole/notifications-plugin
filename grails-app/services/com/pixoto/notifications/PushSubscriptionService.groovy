package com.pixoto.notifications

import grails.gorm.transactions.Transactional

/**
 * Manages Web Push API subscriptions.
 *
 * Stores browser push endpoints so the host app (or a future push sender)
 * can deliver notifications even when the user doesn't have the page open.
 *
 * To actually send push messages, the host app needs a Web Push library
 * (e.g. web-push for Node.js, or webpush-java) and VAPID key configuration.
 * This service handles the subscription lifecycle only.
 */
@Transactional
class PushSubscriptionService {

    /**
     * Register or update a push subscription for a user.
     */
    PushSubscription subscribe(String userId, String scopeId, String endpoint, String p256dhKey, String authKey, String userAgent = null) {
        if (!userId || !endpoint || !p256dhKey || !authKey) return null

        // Upsert: find existing subscription by user + endpoint
        PushSubscription sub = PushSubscription.createCriteria().get {
            eq('userId', userId)
            eq('endpoint', endpoint)
        }
        if (sub) {
            sub.p256dhKey = p256dhKey
            sub.authKey = authKey
            sub.scopeId = scopeId
            if (userAgent) sub.userAgent = userAgent
        } else {
            sub = new PushSubscription(
                userId: userId,
                scopeId: scopeId,
                endpoint: endpoint,
                p256dhKey: p256dhKey,
                authKey: authKey,
                userAgent: userAgent
            )
        }
        sub.save(flush: true, failOnError: true)
        return sub
    }

    /**
     * Remove a push subscription (user revoked permission or unsubscribed).
     */
    boolean unsubscribe(String userId, String endpoint) {
        PushSubscription sub = PushSubscription.createCriteria().get {
            eq('userId', userId)
            eq('endpoint', endpoint)
        }
        if (sub) {
            sub.delete(flush: true)
            return true
        }
        return false
    }

    /**
     * Get all push subscriptions for a user (across all browsers/devices).
     */
    List<PushSubscription> getSubscriptions(String userId) {
        PushSubscription.createCriteria().list {
            eq('userId', userId)
        }
    }

    /**
     * Remove all subscriptions for a user.
     */
    int removeAll(String userId) {
        List<PushSubscription> subs = getSubscriptions(userId)
        subs.each { it.delete(flush: true) }
        return subs.size()
    }
}
