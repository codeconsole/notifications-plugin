package com.pixoto.notifications

import grails.gorm.annotation.AutoTimestamp
import java.time.LocalDateTime

/**
 * Per-user, per-type notification preferences.
 * Controls whether a given notification type is enabled (in-app)
 * and whether browser push notifications are enabled for that type.
 *
 * A null scopeId means global preference; a specific scopeId overrides for that scope.
 */
class NotificationPreference implements Serializable {

    Long id
    String userId
    String scopeId                       // null = global default
    String type                          // Notification type (e.g. 'message', 'mention')
    boolean enabled = true               // In-app notifications enabled
    boolean browserEnabled = false       // Browser notifications enabled
    @AutoTimestamp(AutoTimestamp.EventType.CREATED) LocalDateTime created
    @AutoTimestamp LocalDateTime modified

    static constraints = {
        userId()
        scopeId nullable: true
        type maxSize: 100
    }

    static mapping = {
        collection 'notificationPreference'
        compoundIndex([userId: 1, scopeId: 1, type: 1, indexAttributes: [unique: true]])
    }

    static mapWith = 'mongo'
}
