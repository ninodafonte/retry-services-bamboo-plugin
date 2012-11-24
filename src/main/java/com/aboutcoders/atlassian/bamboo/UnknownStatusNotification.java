package com.aboutcoders.atlassian.bamboo;

import com.atlassian.bamboo.event.BuildCompletedEvent;
import com.atlassian.bamboo.template.TemplateRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class UnknownStatusNotification extends com.atlassian.bamboo.notification.AbstractNotification
{
    private TemplateRenderer templateRenderer;

    public UnknownStatusNotification(TemplateRenderer templateRenderer)
    {
        super();
        this.templateRenderer = templateRenderer;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Send notification when build is in UNKNOWN status.";
    }

    @Override
    public String getTextEmailContent() throws Exception {
        return "TXT EMAIL";
    }

    @Override
    public String getHtmlEmailContent() throws Exception {
        BuildCompletedEvent event = (BuildCompletedEvent) getEvent();
        if (event != null)
        {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("BuildNumber", event.getBuildResultKey());

            try
            {
                return templateRenderer.render("notification-templates/BuildUnknownHtmlEmail.ftl", context);
            }
            catch (Exception e)
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getEmailSubject() throws Exception {
        BuildCompletedEvent event = (BuildCompletedEvent) getEvent();

        return "Unstable build - 1st Attempt - " + event.getBuildResultKey();
    }

    @Override
    public String getIMContent() {
        return null;
    }
}
