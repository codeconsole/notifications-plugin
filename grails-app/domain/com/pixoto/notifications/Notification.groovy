package com.pixoto.notifications

import grails.gorm.annotation.AutoTimestamp
import java.time.LocalDateTime

/**
 * A single notification delivered to a user.
 * Type is a free-form string so any plugin can define its own notification types
 * (e.g. 'message', 'mention', 'community_post', 'event_reminder').
 */
class Notification implements Serializable {

    Long id
    String scopeId
    String userId                        // Recipient
    String type                          // Extensible type identifier
    String title                         // Singular title (e.g. "Scott sent a message")
    String titlePlural                   // Plural template with {count} (e.g. "Scott sent {count} messages")
    String body                          // Preview text
    String link                          // URL to navigate to on click
    String iconUrl                       // Custom icon (e.g. sender avatar), nullable
    String sourceUserId                  // Who triggered this notification
    String sourceEntityType              // Entity kind (e.g. 'conversation', 'post')
    String sourceEntityId                // Entity ID
    int coalescedCount = 1               // How many events this notification represents
    boolean read = false
    LocalDateTime readAt
    @AutoTimestamp(AutoTimestamp.EventType.CREATED) LocalDateTime created
    @AutoTimestamp LocalDateTime modified

    static constraints = {
        scopeId nullable: true
        userId()
        type maxSize: 100
        title maxSize: 200
        titlePlural nullable: true, maxSize: 200
        body nullable: true, maxSize: 500
        link nullable: true, maxSize: 1000
        iconUrl nullable: true, maxSize: 1000
        sourceUserId nullable: true
        sourceEntityType nullable: true, maxSize: 100
        sourceEntityId nullable: true, maxSize: 100
        readAt nullable: true
    }

    static mapping = {
        collection 'notification'
        compoundIndex([userId: 1, scopeId: 1, created: -1])
        compoundIndex([userId: 1, scopeId: 1, read: 1])
        compoundIndex([userId: 1, sourceEntityType: 1, sourceEntityId: 1])
    }

    static mapWith = 'mongo'
}
