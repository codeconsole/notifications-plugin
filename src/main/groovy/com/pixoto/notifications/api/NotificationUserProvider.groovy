package com.pixoto.notifications.api

/**
 * SPI that host apps implement to supply user information
 * for rendering notification details (sender names, avatars).
 *
 * Register the implementation as a Spring bean named 'notificationUserProvider'.
 */
interface NotificationUserProvider {

    /**
     * Batch-load users by their IDs.
     * @return Map of userId -> NotificationUser (missing IDs may be omitted)
     */
    Map<Serializable, NotificationUser> getUsers(Collection<Serializable> userIds)
}
