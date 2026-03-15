package com.pixoto.notifications

import grails.gorm.annotation.AutoTimestamp
import java.time.LocalDateTime

/**
 * Web Push API subscription for a user's browser.
 * Stores the push endpoint and encryption keys needed to send
 * push notifications via the Web Push protocol.
 *
 * Each browser/device gets its own subscription record.
 */
class PushSubscription implements Serializable {

    Long id
    String userId
    String scopeId
    String endpoint                      // Push service endpoint URL
    String p256dhKey                      // Client public key (Base64)
    String authKey                       // Auth secret (Base64)
    String userAgent                     // Browser identifier (for management UI)
    @AutoTimestamp(AutoTimestamp.EventType.CREATED) LocalDateTime created

    static constraints = {
        userId()
        scopeId nullable: true
        endpoint maxSize: 2000
        p256dhKey maxSize: 500
        authKey maxSize: 500
        userAgent nullable: true, maxSize: 500
    }

    static mapping = {
        collection 'notificationPushSubscription'
        compoundIndex([userId: 1, endpoint: 1, indexAttributes: [unique: true]])
    }

    static mapWith = 'mongo'
}
