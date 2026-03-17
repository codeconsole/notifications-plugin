/**
 * Notifications Module (standalone)
 * Handles bell icon dropdown, unread badge, notification list,
 * browser Notification API, and push subscription management.
 *
 * API base: /notifications
 */
var Notifications = (function() {
    'use strict';

    var config = {};
    var state = {
        dropdownOpen: false,
        unreadCount: 0,
        pollTimer: null,
        lastSeenId: null,
        browserPermission: null,
        notifications: []
    };

    // Type → Bootstrap Icon mapping (extensible via config.typeIcons)
    var DEFAULT_TYPE_ICONS = {
        'message': 'bi-chat-dots-fill',
        'mention': 'bi-at',
        'community_post': 'bi-megaphone-fill',
        'community_reply': 'bi-reply-fill',
        'event_reminder': 'bi-calendar-event-fill',
        'event_invite': 'bi-calendar-plus-fill',
        'share': 'bi-share-fill',
        'system': 'bi-info-circle-fill'
    };

    // ─── CSRF ───────────────────────────────────────────

    function getCsrfToken() {
        return document.querySelector('meta[name="_csrf"]')?.content;
    }

    function getCsrfHeader() {
        return document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    }

    function fetchJson(url, options) {
        options = options || {};
        options.headers = options.headers || {};
        options.headers[getCsrfHeader()] = getCsrfToken();
        if (options.body && typeof options.body === 'string') {
            options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
        }
        if (options.json) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(options.json);
            delete options.json;
        }
        return fetch(url, options).then(function(r) {
            if (!r.ok) {
                console.warn('Notifications: fetch error', r.status, url);
                return {};
            }
            return r.json();
        }).catch(function(err) {
            console.warn('Notifications: fetch failed', url, err);
            return {};
        });
    }

    // ─── Init ───────────────────────────────────────────

    function init(cfg) {
        config = cfg || {};
        config.base = cfg.notificationsBase || '/notifications';
        config.pollInterval = cfg.pollInterval || 30000;
        config.typeIcons = Object.assign({}, DEFAULT_TYPE_ICONS, cfg.typeIcons || {});

        state.browserPermission = ('Notification' in window) ? Notification.permission : 'unsupported';

        // Bell button toggle
        var bellBtn = document.getElementById('notifications-bell-btn');
        if (bellBtn) {
            bellBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                toggleDropdown();
            });
        }

        // Close dropdown on outside click
        document.addEventListener('click', function(e) {
            if (state.dropdownOpen && !e.target.closest('#notifications-bell')) {
                closeDropdown();
            }
        });

        // Mark all read buttons (dropdown + full page)
        var markAllBtn = document.getElementById('notifications-mark-all-read');
        if (markAllBtn) {
            markAllBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                markAllRead();
            });
        }
        var markAllBtnFull = document.getElementById('mark-all-read-btn');
        if (markAllBtnFull) {
            markAllBtnFull.addEventListener('click', function() {
                markAllRead();
            });
        }

        // Browser notification enable button
        var enableBtn = document.getElementById('notifications-enable-browser');
        if (enableBtn) {
            updateBrowserButton();
            enableBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                requestBrowserPermission();
            });
        }

        // Load more button (full page)
        var loadMoreBtn = document.getElementById('load-more-btn');
        if (loadMoreBtn) {
            loadMoreBtn.addEventListener('click', function() {
                loadMore();
            });
        }

        // Full page: wire up existing items
        if (cfg.fullPage) {
            wireNotificationItems(document.getElementById('notifications-full-list'));
            if (cfg.oldestId) state.lastSeenId = cfg.oldestId;
        }

        // Initial fetch and start polling
        fetchUnreadCount();
        startPolling();
    }

    // ─── Dropdown ───────────────────────────────────────

    function toggleDropdown() {
        if (state.dropdownOpen) {
            closeDropdown();
        } else {
            openDropdown();
        }
    }

    function openDropdown() {
        var dropdown = document.getElementById('notifications-dropdown');
        if (!dropdown) return;
        dropdown.classList.add('show');
        state.dropdownOpen = true;
        loadDropdownNotifications();
    }

    function closeDropdown() {
        var dropdown = document.getElementById('notifications-dropdown');
        if (!dropdown) return;
        dropdown.classList.remove('show');
        state.dropdownOpen = false;
    }

    function loadDropdownNotifications() {
        fetchJson(config.base + '/list?max=15').then(function(data) {
            var list = document.getElementById('notifications-dropdown-list');
            if (!list) return;

            if (!data.notifications || data.notifications.length === 0) {
                list.innerHTML =
                    '<div class="notifications-empty text-center py-3">' +
                    '<i class="bi bi-bell-slash" style="font-size: 1.5rem; opacity: 0.3;"></i>' +
                    '<p class="small text-muted mb-0 mt-1">No notifications</p></div>';
                return;
            }

            state.notifications = data.notifications;
            list.innerHTML = data.notifications.map(renderNotificationItem).join('');
            wireNotificationItems(list);
        });
    }

    // ─── Rendering ──────────────────────────────────────

    function renderNotificationItem(n) {
        var iconHtml;
        if (n.iconUrl) {
            iconHtml = '<img src="' + escapeHtml(n.iconUrl) + '" alt="" class="rounded-circle" width="36" height="36"/>';
        } else {
            var iconClass = config.typeIcons[n.type] || 'bi-bell-fill';
            iconHtml = '<span class="notifications-type-icon"><i class="bi ' + iconClass + '"></i></span>';
        }

        var bodyHtml = n.body
            ? '<div class="notifications-item-body">' + escapeHtml(n.body) + '</div>'
            : '';

        var readBtnHtml = !n.read
            ? '<button class="btn btn-sm btn-link notifications-mark-read-btn" title="Mark as read" data-id="' + n.id + '"><i class="bi bi-check2"></i></button>'
            : '';

        return '<div class="notifications-item ' + (n.read ? '' : 'unread') + '" data-notification-id="' + n.id + '" data-link="' + escapeHtml(n.link || '') + '">' +
            '<div class="notifications-item-icon">' + iconHtml + '</div>' +
            '<div class="notifications-item-content">' +
            '<div class="notifications-item-title">' + escapeHtml(n.title) + '</div>' +
            bodyHtml +
            '<div class="notifications-item-time">' + escapeHtml(n.timeAgo || '') + '</div>' +
            '</div>' +
            '<div class="notifications-item-actions">' + readBtnHtml + '</div>' +
            '</div>';
    }

    function wireNotificationItems(container) {
        if (!container) return;
        // Click on item → delete + navigate
        container.querySelectorAll('.notifications-item').forEach(function(item) {
            item.addEventListener('click', function(e) {
                if (e.target.closest('.notifications-mark-read-btn')) return;
                var id = item.dataset.notificationId;
                var link = item.dataset.link;
                if (item.classList.contains('unread')) {
                    state.unreadCount = Math.max(0, state.unreadCount - 1);
                    updateBadge(state.unreadCount);
                }
                item.remove();
                deleteNotification(id);
                if (link) window.location.href = link;
            });
        });
        // Mark read button (checkmark) — just clear unread, keep in list
        container.querySelectorAll('.notifications-mark-read-btn').forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                var id = btn.dataset.id;
                var item = btn.closest('.notifications-item');
                if (item) item.classList.remove('unread');
                btn.remove();
                state.unreadCount = Math.max(0, state.unreadCount - 1);
                updateBadge(state.unreadCount);
                markRead(id);
            });
        });
    }

    // ─── API Actions ────────────────────────────────────

    function fetchUnreadCount() {
        fetchJson(config.base + '/unread').then(function(data) {
            updateBadge(data.count || 0);
        });
    }

    function markRead(notificationId) {
        fetchJson(config.base + '/' + notificationId + '/read', { method: 'POST' });
    }

    function deleteNotification(notificationId) {
        fetchJson(config.base + '/' + notificationId, { method: 'DELETE' });
    }

    function markAllRead() {
        fetchJson(config.base + '/read-all', { method: 'POST' })
            .then(function(data) {
                if (data.success) {
                    state.unreadCount = 0;
                    updateBadge(0);
                    document.querySelectorAll('.notifications-item.unread').forEach(function(item) {
                        item.classList.remove('unread');
                    });
                    document.querySelectorAll('.notifications-mark-read-btn').forEach(function(btn) {
                        btn.remove();
                    });
                }
            });
    }

    function loadMore() {
        var lastItem = document.querySelector('#notifications-full-list .notifications-item:last-of-type');
        var beforeId = lastItem ? lastItem.dataset.notificationId : null;
        if (!beforeId) return;

        var loadMoreDiv = document.getElementById('notifications-load-more');
        var btn = document.getElementById('load-more-btn');
        if (btn) btn.disabled = true;

        fetchJson(config.base + '/list?max=20&beforeId=' + beforeId).then(function(data) {
            if (!data.notifications || data.notifications.length === 0) {
                if (loadMoreDiv) loadMoreDiv.remove();
                return;
            }

            var list = document.getElementById('notifications-full-list');
            if (loadMoreDiv) loadMoreDiv.remove();

            var html = data.notifications.map(renderNotificationItem).join('');
            if (data.hasMore) {
                html += '<div id="notifications-load-more" class="text-center py-3">' +
                    '<button class="btn btn-sm btn-outline-secondary" id="load-more-btn">Load more</button></div>';
            }

            list.insertAdjacentHTML('beforeend', html);
            wireNotificationItems(list);

            if (data.hasMore) {
                var newBtn = document.getElementById('load-more-btn');
                if (newBtn) newBtn.addEventListener('click', function() { loadMore(); });
            }
        });
    }

    // ─── Badge ──────────────────────────────────────────

    function updateBadge(count) {
        state.unreadCount = count;
        var badge = document.getElementById('notifications-badge');
        if (!badge) return;
        if (count > 0) {
            badge.textContent = count > 99 ? '99+' : count;
            badge.classList.remove('d-none');
        } else {
            badge.classList.add('d-none');
        }
    }

    // ─── Polling ────────────────────────────────────────

    function startPolling() {
        if (state.pollTimer) clearInterval(state.pollTimer);
        state.pollTimer = setInterval(function() {
            var previousCount = state.unreadCount;
            fetchJson(config.base + '/unread').then(function(data) {
                var newCount = data.count || 0;
                updateBadge(newCount);

                // If count increased and browser notifications are granted, show one
                if (newCount > previousCount && state.browserPermission === 'granted') {
                    showBrowserNotification(newCount - previousCount);
                }
            });
        }, config.pollInterval);
    }

    // ─── Browser Notifications ──────────────────────────

    function requestBrowserPermission() {
        if (!('Notification' in window)) {
            alert('Browser notifications are not supported in this browser.');
            return;
        }

        if (Notification.permission === 'granted') {
            state.browserPermission = 'granted';
            updateBrowserButton();
            return;
        }

        if (Notification.permission === 'denied') {
            alert('Notifications are blocked. Please enable them in your browser settings.');
            return;
        }

        Notification.requestPermission().then(function(permission) {
            state.browserPermission = permission;
            updateBrowserButton();
            if (permission === 'granted') {
                new Notification('Notifications enabled', {
                    body: 'You will now receive browser notifications.',
                    icon: '/assets/notifications/bell-icon.png'
                });
            }
        });
    }

    function showBrowserNotification(newCount) {
        if (state.browserPermission !== 'granted') return;
        if (document.hasFocus && document.hasFocus()) return; // Don't show if page is focused

        var title = newCount === 1 ? 'New notification' : newCount + ' new notifications';
        var notification = new Notification(title, {
            body: 'Click to view your notifications.',
            icon: '/assets/notifications/bell-icon.png',
            tag: 'notifications-update'
        });

        notification.onclick = function() {
            window.focus();
            notification.close();
            openDropdown();
        };

        // Auto-close after 5 seconds
        setTimeout(function() { notification.close(); }, 5000);
    }

    function updateBrowserButton() {
        var label = document.getElementById('notifications-browser-label');
        var btn = document.getElementById('notifications-enable-browser');
        if (!label || !btn) return;

        if (state.browserPermission === 'granted') {
            label.textContent = 'Browser notifications enabled';
            btn.classList.add('text-success');
            btn.classList.remove('text-muted');
            btn.querySelector('i').className = 'bi bi-check-circle-fill';
        } else if (state.browserPermission === 'denied') {
            label.textContent = 'Notifications blocked';
            btn.classList.add('text-danger');
            btn.classList.remove('text-muted');
        } else if (state.browserPermission === 'unsupported') {
            btn.style.display = 'none';
        }
    }

    // ─── Push Subscription (Web Push API) ───────────────

    function subscribeToPush(vapidPublicKey) {
        if (!('serviceWorker' in navigator) || !('PushManager' in window)) return;

        navigator.serviceWorker.ready.then(function(registration) {
            var options = {
                userVisibleOnly: true,
                applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
            };
            return registration.pushManager.subscribe(options);
        }).then(function(subscription) {
            var key = subscription.getKey('p256dh');
            var auth = subscription.getKey('auth');
            return fetchJson(config.base + '/push/subscribe', {
                method: 'POST',
                json: {
                    endpoint: subscription.endpoint,
                    p256dhKey: btoa(String.fromCharCode.apply(null, new Uint8Array(key))),
                    authKey: btoa(String.fromCharCode.apply(null, new Uint8Array(auth)))
                }
            });
        });
    }

    function unsubscribeFromPush() {
        if (!('serviceWorker' in navigator)) return;

        navigator.serviceWorker.ready.then(function(registration) {
            return registration.pushManager.getSubscription();
        }).then(function(subscription) {
            if (!subscription) return;
            var endpoint = subscription.endpoint;
            return subscription.unsubscribe().then(function() {
                return fetchJson(config.base + '/push/unsubscribe', {
                    method: 'POST',
                    json: { endpoint: endpoint }
                });
            });
        });
    }

    // ─── Utilities ──────────────────────────────────────

    function escapeHtml(str) {
        if (!str) return '';
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function urlBase64ToUint8Array(base64String) {
        var padding = '='.repeat((4 - base64String.length % 4) % 4);
        var base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
        var rawData = atob(base64);
        var outputArray = new Uint8Array(rawData.length);
        for (var i = 0; i < rawData.length; i++) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }

    // ─── Public API ─────────────────────────────────────

    return {
        init: init,
        openDropdown: openDropdown,
        closeDropdown: closeDropdown,
        fetchUnreadCount: fetchUnreadCount,
        requestBrowserPermission: requestBrowserPermission,
        subscribeToPush: subscribeToPush,
        unsubscribeFromPush: unsubscribeFromPush,
        // For other plugins to trigger a UI refresh
        refresh: function() {
            fetchUnreadCount();
            if (state.dropdownOpen) loadDropdownNotifications();
        }
    };
})();
