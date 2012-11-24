retry-services-bamboo-plugin
============================

Experimental plugin for bamboo.
The main functionality is to retry a plan when fails for the first time.
The whole functionality includes a new Task Type, new Notification Type and some hooks to fit the complete action.

Detailed explanation:

- RetryTask.java: creates a new Task Type. Adding this task as a final task in a plan, and enabling the checkbox it's
enough to turn on the whole process. When the build fails for the first time, the plugin will set this build as "UNKNOWN"
and will create a new job in the queue for another attempt. At the same time, you can define a new notification (UNKNOWN BUILDS)
to receive information about this UNKNOWN build.
If the build fails another time, it will trigger a failed notification (standard notification) and its job will be finished.

- RetryTaskConfigurator.java: Configuration of the task type (checkbox to enable/disable the task)

- RetryPostBuild.java: Manage the notification for this UNKNOWN build.

- UnknownBuildNotificationType.java: The new type of notification you will find in "Notification" inside the plan configuration.

- UnknownStatusNotification.java: The notification object itself. Here you can define formats, data, etc for the notification.

It's really difficult to find examples of the bamboo api, hope this "experimental" plugin can help anyone who need some example.

Tested only with Atlassian sdk, and working fine :).