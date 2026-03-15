<%--
  Notification preferences panel. Can be rendered inline on a settings page.

  Usage:
    <g:render template="/notifications/notification/preferences"
              model="[notificationsBase: '/notifications']"/>
--%>
<div class="notifications-preferences" id="notifications-preferences">
    <h5>Notification Preferences</h5>
    <p class="text-muted small">Choose which notifications you receive.</p>

    <div class="notifications-pref-list" id="notifications-pref-list">
        <%-- Populated by JS from /notifications/preferences endpoint --%>
        <div class="text-center py-3">
            <div class="spinner-border spinner-border-sm text-secondary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>
    </div>
</div>
