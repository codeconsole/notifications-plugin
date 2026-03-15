<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.getProperty('community.layout', String, 'main')}"/>
    <meta name="_csrf" content="${_csrf?.token}"/>
    <meta name="_csrf_header" content="${_csrf?.headerName}"/>
    <title>Notifications</title>
    <asset:stylesheet src="notifications/notifications.css"/>
</head>
<body>
<div class="container py-3">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">Notifications</h4>
        <div>
            <g:if test="${unreadCount > 0}">
                <button class="btn btn-sm btn-outline-secondary" id="mark-all-read-btn">
                    <i class="bi bi-check2-all"></i> Mark all read
                </button>
            </g:if>
        </div>
    </div>

    <div class="notifications-full-list" id="notifications-full-list">
        <g:if test="${notifications}">
            <g:each in="${notifications}" var="n">
                <g:render template="/notifications/notification/notificationItem" model="[n: n]"/>
            </g:each>
        </g:if>
        <g:else>
            <div class="text-center py-5">
                <i class="bi bi-bell-slash" style="font-size: 2.5rem; opacity: 0.3;"></i>
                <p class="mt-2 text-muted">No notifications yet</p>
            </div>
        </g:else>

        <g:if test="${hasMore}">
            <div id="notifications-load-more" class="text-center py-3">
                <button class="btn btn-sm btn-outline-secondary" id="load-more-btn">Load more</button>
            </div>
        </g:if>
    </div>
</div>

<asset:javascript src="notifications/notifications.js"/>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        Notifications.init({
            notificationsBase: '${notificationsBase}',
            currentUserId: '${userId ?: ''}',
            fullPage: true,
            oldestId: ${oldestId ?: 'null'}
        });
    });
</script>
</body>
</html>
