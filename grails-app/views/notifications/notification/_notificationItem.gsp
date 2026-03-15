<div class="notifications-item ${n.read ? '' : 'unread'}" data-notification-id="${n.id}" data-link="${n.link ?: ''}">
    <div class="notifications-item-icon">
        <g:if test="${n.iconUrl}">
            <img src="${n.iconUrl}" alt="" class="rounded-circle" width="36" height="36"/>
        </g:if>
        <g:else>
            <span class="notifications-type-icon"><i class="bi ${com.pixoto.notifications.NotificationIconUtil.iconForType(n.type)}"></i></span>
        </g:else>
    </div>
    <div class="notifications-item-content">
        <div class="notifications-item-title">${n.title?.encodeAsHTML()}</div>
        <g:if test="${n.body}">
            <div class="notifications-item-body">${n.body?.encodeAsHTML()}</div>
        </g:if>
        <div class="notifications-item-time">${n.timeAgo}</div>
    </div>
    <div class="notifications-item-actions">
        <g:if test="${!n.read}">
            <button class="btn btn-sm btn-link notifications-mark-read-btn" title="Mark as read" data-id="${n.id}">
                <i class="bi bi-check2"></i>
            </button>
        </g:if>
    </div>
</div>
