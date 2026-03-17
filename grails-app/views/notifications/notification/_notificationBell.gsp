<%--
  Notification bell component for inclusion in the host app's header/navbar.

  Usage in your layout GSP:
    <asset:stylesheet src="notifications/notifications.css"/>
    <g:render template="/notifications/notification/notificationBell" model="[notificationsBase: '/notifications']"/>
    <asset:javascript src="notifications/notifications.js"/>
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            Notifications.init({ notificationsBase: '/notifications', currentUserId: '${currentUserId}' });
        });
    </script>
--%>
<div class="notifications-bell-wrapper" id="notifications-bell"
     data-poll-interval="${grailsApplication.config.getProperty('notifications.pollInterval', Integer, 30000)}">
    <button class="btn btn-link notifications-bell-btn position-relative" type="button"
            id="notifications-bell-btn" aria-label="Notifications" title="Notifications">
        <i class="bi bi-bell"></i>
        <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger d-none"
              id="notifications-badge">0</span>
    </button>

    <div class="notifications-dropdown" id="notifications-dropdown">
        <div class="notifications-dropdown-header">
            <span class="fw-semibold">Notifications</span>
            <div>
                <button class="btn btn-sm btn-link p-0 me-2" id="notifications-mark-all-read" title="Mark all as read">
                    <i class="bi bi-check2-all"></i>
                </button>
                <a href="${notificationsBase ?: '/notifications'}" class="btn btn-sm btn-link p-0" title="View all">
                    <i class="bi bi-arrow-up-right-square"></i>
                </a>
            </div>
        </div>

        <div class="notifications-dropdown-list" id="notifications-dropdown-list">
            <div class="notifications-empty text-center py-3">
                <i class="bi bi-bell-slash" style="font-size: 1.5rem; opacity: 0.3;"></i>
                <p class="small text-muted mb-0 mt-1">No notifications</p>
            </div>
        </div>

        <div class="notifications-dropdown-footer">
            <button class="btn btn-sm btn-link text-muted" id="notifications-enable-browser" title="Enable browser notifications">
                <i class="bi bi-window-desktop"></i> <span id="notifications-browser-label">Enable browser notifications</span>
            </button>
        </div>
    </div>
</div>
