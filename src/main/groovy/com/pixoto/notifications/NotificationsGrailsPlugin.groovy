package com.pixoto.notifications

import grails.plugins.*

class NotificationsGrailsPlugin extends Plugin {

    def grailsVersion = "7.0.0 > *"

    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Notifications Plugin"
    def author = "Pixoto"
    def authorEmail = ""
    def description = 'Standalone notifications plugin for Grails applications. Provides in-app and browser notifications with an extensible type system.'
    def profiles = ['web']

    def documentation = "http://grails.org/plugin/notifications-plugin"

    Closure doWithSpring() { {->
    }}

    void doWithDynamicMethods() {
    }

    void doWithApplicationContext() {
    }

    void onChange(Map<String, Object> event) {
    }

    void onConfigChange(Map<String, Object> event) {
    }

    void onShutdown(Map<String, Object> event) {
    }
}
