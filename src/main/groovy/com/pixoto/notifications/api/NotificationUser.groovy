package com.pixoto.notifications.api

/**
 * Abstraction for user identity in the notifications plugin.
 * Host apps implement this to wrap their native user model.
 */
interface NotificationUser {

    Serializable getId()

    String getDisplayName()

    String getAvatarUrl()
}
