package com.aboutcoders.atlassian.bamboo;

import com.atlassian.bamboo.notification.AbstractNotificationType;
import com.atlassian.event.Event;
import org.jetbrains.annotations.NotNull;

public class UnknownBuildNotificationType extends AbstractNotificationType
{
    @Override
    public boolean isNotificationRequired(@NotNull Event event)
    {
        return true;
    }
}
