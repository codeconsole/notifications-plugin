# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A standalone Grails 7 plugin that adds in-app and browser notifications to any host application. It is host-app agnostic — all user resolution goes through the `NotificationUserProvider` SPI, and scoping uses a generic `scopeId` string rather than assuming a specific tenant model.

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

This module cannot run standalone — it is included as a dependency in the host app (the root L3me project). Use `./gradlew bootRun` at the project root to test end-to-end.

## Architecture

### Host-App Integration Points

The plugin requires two things from the host app:

1. **`NotificationUserProvider`** (`api/NotificationUserProvider.groovy`) — A Spring bean the host implements to supply batch user lookup (display names, avatars) for rendering notification source users.

2. **`NotificationsInterceptor`** (`NotificationsInterceptor.groovy`) — The host subclasses this abstract interceptor to set two request attributes before any notification controller action runs:
   - `notificationsScopeId` — tenant/community/scope identifier
   - `notificationsUserId` — the authenticated user's ID as a String

The controller reads these via `getScopeId()` / `getUserId()` helper methods.

### Domain Model (MongoDB, `mapWith = 'mongo'`)

| Domain | Collection | Key Fields |
|---|---|---|
| `Notification` | `notification` | scopeId, userId (recipient), type (extensible string), title, body, link, sourceUserId, read flag |
| `NotificationPreference` | `notificationPreference` | userId + scopeId + type (unique), enabled/browserEnabled booleans |
| `PushSubscription` | `notificationPushSubscription` | userId + endpoint (unique), p256dh/auth keys for Web Push |

No GORM associations — linked by string IDs and queried via criteria builders.

### Service Layer

- **`NotificationService`** — Core notification CRUD. Any plugin can inject this to create notifications via `notify(scopeId, userId, type, title, body, link, opts)`. Also handles preferences, enrichment with user info, and cursor-based pagination.
- **`PushSubscriptionService`** — Web Push subscription lifecycle (subscribe, unsubscribe, list).

### Creating Notifications from Other Plugins

```groovy
@Autowired(required = false)
NotificationService notificationService

// In your service method:
notificationService?.notify(scopeId, recipientUserId, 'message',
    "${senderName} sent you a message", messagePreview, "/messaging/${conversationId}",
    [sourceUserId: senderId, iconUrl: senderAvatarUrl,
     sourceEntityType: 'conversation', sourceEntityId: conversationId.toString()])
```

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

### Frontend

Vanilla JS module (`Notifications` IIFE in `notifications.js`) with no framework dependencies:
- Bell icon dropdown for header (via `_notificationBell.gsp` template)
- Client-side rendering of notification items in dropdown
- Server-side GSP for full page list
- Browser Notification API integration (permission request for Chrome/Safari)
- Unread badge polling every 30 seconds
- Web Push subscription helpers (client-side)

### Including the Bell in Your Layout

```gsp
<asset:stylesheet src="notifications/notifications.css"/>
<!-- In your navbar -->
<g:render template="/notifications/notification/notificationBell"
          model="[notificationsBase: '/notifications']"/>
<!-- Before </body> -->
<asset:javascript src="notifications/notifications.js"/>
<script>
document.addEventListener('DOMContentLoaded', function() {
    Notifications.init({
        notificationsBase: '/notifications',
        currentUserId: '${currentUserId}'
    });
});
</script>
```

### Key Patterns

- **Extensible type system**: Notification `type` is a free-form string. Any plugin defines its own types. Preferences and icon mapping use the same type strings.
- **Opt-out model**: All notification types are enabled by default. Users disable specific types via preferences.
- **Cursor pagination**: `getNotifications(userId, scopeId, beforeId, max)` — older notifications loaded on "Load more" click.
- **CSRF**: All JS fetch calls include the CSRF token from `<meta>` tags via `getCsrfHeader()`/`getCsrfToken()`.
- **Browser notifications**: Uses standard Notification API. Permission prompt triggered by user action (clicking enable button). Notifications shown when page is not focused and unread count increases.
- **Icon mapping**: `NotificationIconUtil` maps type strings to Bootstrap Icon classes. Notifications with a `sourceUserId` show the sender's avatar instead.
