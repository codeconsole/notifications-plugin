# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A standalone Grails 7 plugin that adds in-app and browser notifications to any host application. It is fully self-contained — user identity is resolved from Spring Security's `SecurityContextHolder` (no host-app interceptor required), and an optional `NotificationUserProvider` SPI enriches notifications with display names and avatars.

## Build Commands

```bash
# Compile (from project root, not this module)
./gradlew :notifications-plugin:compileGroovy

# Build the plugin JAR
./gradlew :notifications-plugin:build

# Run tests
./gradlew :notifications-plugin:test

# Full clean + rebuild
./gradlew :notifications-plugin:clean :notifications-plugin:build
```

This module cannot run standalone — it is included as a dependency in the host app. Use `./gradlew bootRun` at the project root to test end-to-end.

## Architecture

### Host-App Integration Points

The plugin works out of the box with one optional SPI:

1. **`NotificationsInterceptor`** (`NotificationsInterceptor.groovy`) — A concrete interceptor (registered as a bean in `doWithSpring()`) that reads `principal.id` from Spring Security's `SecurityContextHolder` and sets `notificationsUserId` on the request. Works with any Spring Security setup where the principal has an `id` property. Host apps can override by registering a bean named `notificationsInterceptor`.

2. **`NotificationUserProvider`** (`api/NotificationUserProvider.groovy`) — Optional. A Spring bean the host implements to supply batch user lookup (display names, avatars) for rendering notification source users. If not provided, notifications render without source user info.

### Domain Model (MongoDB, `mapWith = 'mongo'`)

| Domain | Collection | Key Fields |
|---|---|---|
| `Notification` | `notification` | userId (recipient), type (extensible string), title, titlePlural, body, link, sourceUserId, coalescedCount, read flag |
| `NotificationPreference` | `notificationPreference` | userId + scopeId + type (unique), enabled/browserEnabled booleans |
| `PushSubscription` | `notificationPushSubscription` | userId + endpoint (unique), p256dh/auth keys for Web Push |

No GORM associations — linked by string IDs and queried via criteria builders.

### Service Layer

- **`NotificationService`** — Core notification CRUD. Any plugin can inject this to create notifications via `notify(scopeId, userId, type, title, body, link, opts)`. Also handles preferences, coalescing, enrichment with user info, and cursor-based pagination.
- **`PushSubscriptionService`** — Web Push subscription lifecycle (subscribe, unsubscribe, list).

### Creating Notifications from Other Plugins

```groovy
@Autowired(required = false)
NotificationService notificationService

// In your service method:
notificationService?.notify(scopeId, recipientUserId, 'message',
    "${senderName} sent a message", messagePreview, "/messaging/${conversationId}",
    [sourceUserId: senderId, iconUrl: senderAvatarUrl,
     sourceEntityType: 'conversation', sourceEntityId: conversationId.toString(),
     titlePlural: "${senderName} sent {count} messages"])
```

### Notification Coalescing

When `sourceEntityType` and `sourceEntityId` are provided, the service coalesces notifications: if an existing **unread** notification matches the same `userId + type + sourceEntityType + sourceEntityId`, it updates that notification instead of creating a new one. The `coalescedCount` increments, the `body` refreshes to the latest content, and the `modified` timestamp updates.

This prevents notification spam — e.g., 5 messages in the same conversation produce 1 notification with count=5.

### Singular/Plural Titles

Callers pass both `title` (singular) and `titlePlural` (template with `{count}` placeholder) via opts:

- `title: "Scott sent a message"` — used when `coalescedCount == 1`
- `titlePlural: "Scott sent {count} messages"` — used when `coalescedCount > 1`, `{count}` replaced with the actual number

The enrichment method resolves the correct title based on the count before returning to the client.

### Controller & URL Mapping

Single controller (`NotificationController`, namespace `notifications`) handles all endpoints. URL mappings in `NotificationsUrlMappings`:

- `GET /notifications` — full page notification list (index)
- `GET /notifications/list` — JSON list for dropdown/AJAX
- `GET /notifications/unread` — unread count (polled by JS)
- `POST /notifications/:id/read` — mark single as read
- `POST /notifications/read-all` — mark all as read
- `DELETE /notifications/:id` — delete notification
- `GET /notifications/preferences` — get user preferences
- `POST /notifications/preferences` — update a preference
- `POST /notifications/push/subscribe` — register push subscription
- `POST /notifications/push/unsubscribe` — remove push subscription

The bell/dropdown/unread endpoints query by `userId` only (no scope filter) so notifications from all sources appear regardless of which plugin or scope created them.

### Frontend

Vanilla JS module (`Notifications` IIFE in `notifications.js`) with no framework dependencies:
- Bell icon dropdown for header (via `_notificationBell.gsp` template)
- Client-side rendering of notification items in dropdown
- Server-side GSP for full page list
- Browser Notification API integration (permission request for Chrome/Safari)
- Unread badge polling every 30 seconds
- Web Push subscription helpers (client-side)

### Notification Interaction Model

- **Click notification** → deletes it from the server, removes from dropdown, decrements badge, navigates to the link
- **Mark as read (checkmark button)** → marks as read server-side, removes unread styling, decrements badge, keeps notification in list
- **Mark all read** → marks all as read, clears badge, removes unread styling from all items

### Including the Bell in Your Layout

```gsp
<meta name="_csrf" content="${_csrf?.token}"/>
<meta name="_csrf_header" content="${_csrf?.headerName}"/>
<asset:stylesheet src="notifications/notifications.css"/>
<!-- In your navbar -->
<g:render template="/notifications/notification/notificationBell"
          model="[notificationsBase: '/notifications']"/>
<!-- Before </body> -->
<asset:javascript src="notifications/notifications.js"/>
<script>
document.addEventListener('DOMContentLoaded', function() {
    Notifications.init({ notificationsBase: '/notifications' });
});
</script>
```

### Key Patterns

- **Standalone interceptor**: The plugin provides its own `NotificationsInterceptor` that reads the user ID directly from Spring Security. No host-app interceptor needed. Override by registering a bean named `notificationsInterceptor`.
- **Extensible type system**: Notification `type` is a free-form string. Any plugin defines its own types. Preferences and icon mapping use the same type strings.
- **Coalescing**: Multiple events for the same entity (e.g., messages in one conversation) produce a single notification with an incrementing count and singular/plural title support.
- **Opt-out model**: All notification types are enabled by default. Users disable specific types via preferences.
- **Cursor pagination**: `getNotifications(userId, scopeId, beforeId, max)` — older notifications loaded on "Load more" click.
- **CSRF**: All JS fetch calls include the CSRF token from `<meta>` tags via `getCsrfHeader()`/`getCsrfToken()`. CSRF meta tags must be in the layout `<head>`.
- **Browser notifications**: Uses standard Notification API. Permission prompt triggered by user action (clicking enable button). Notifications shown when page is not focused and unread count increases.
- **Icon mapping**: `NotificationIconUtil` maps type strings to Bootstrap Icon classes. Notifications with a `sourceUserId` show the sender's avatar instead.

## L3me Host-App Wiring

In this project, the community-plugin provides the `NotificationUserProvider` implementation:

| SPI | Implementation | Registered in |
|---|---|---|
| `NotificationUserProvider` | `CommunityNotificationUserProvider` — resolves host app user IDs to community `User` records (display names, avatars) via `User.hostUserId` | `CommunityGrailsPlugin.doWithSpring()` |

The messaging-plugin and community-plugin inject `NotificationService` with `@Autowired(required = false)` to create notifications:
- **Messaging**: `MessageService.sendMessage()` notifies conversation participants (coalesced per conversation)
- **Community**: `TopicReplyService.createReply()` notifies the topic author when someone else replies
