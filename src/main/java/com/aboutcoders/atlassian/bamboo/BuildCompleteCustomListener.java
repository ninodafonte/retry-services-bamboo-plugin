package com.aboutcoders.atlassian.bamboo;

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.event.BuildCompletedEvent;
import com.atlassian.bamboo.event.HibernateEventListener;
import com.atlassian.bamboo.notification.*;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.TopLevelPlan;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.event.Event;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;

import java.util.Set;

public class BuildCompleteCustomListener implements HibernateEventListener {

    private NotificationManager notificationManager;
    private NotificationDispatcher notificationDispatcher;
    private PlanManager planManager;
    private TransactionTemplate transactionTemplate;
    private TemplateRenderer templateRenderer;

    public Class[] getHandledEventClasses()
    {
        Class[] array = {BuildCompletedEvent.class};
        return array;
    }

    @Override
    public void handleEvent(Event event) {
        final BuildCompletedEvent buildEvent = (BuildCompletedEvent) event;
        final Plan currentPlan = planManager.getPlanByKey(buildEvent.getPlanKey());

        transactionTemplate.execute(new TransactionCallback()
        {
            @Override
            public Object doInTransaction()
            {
                if (buildEvent.getBuildState() == BuildState.UNKNOWN)
                {
                    Notification myNotification = new UnknownStatusNotification(templateRenderer);
                    myNotification.setEvent(buildEvent);

                    String parentPlanKey = getCustomPlanKey(currentPlan.getPlanKey().getKey());
                    Set<NotificationRule> rules = notificationManager.getNotificationRules((TopLevelPlan) planManager.getPlanByKey(parentPlanKey));
                    for (NotificationRule rule : rules)
                    {
                        NotificationType notificationType = rule.getNotificationType();
                        if (notificationType instanceof UnknownBuildNotificationType)
                        {
                            if (notificationType.isNotificationRequired(buildEvent))
                            {
                                NotificationRecipient recipient = rule.getNotificationRecipient();
                                myNotification.addRecipient(recipient);
                            }
                        }
                    }
                    notificationDispatcher.dispatchNotifications(myNotification);
                }

                return null;
            }
        });
    }

    public String getCustomPlanKey(String key)
    {
        return key.substring(0, key.lastIndexOf("-"));
    }

    public void setNotificationManager(NotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    public void setNotificationDispatcher(NotificationDispatcher notificationDispatcher)
    {
        this.notificationDispatcher = notificationDispatcher;
    }

    public void setPlanManager(final PlanManager planManager)
    {
        this.planManager = planManager;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setTemplateRenderer(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }
}
