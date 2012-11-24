package com.aboutcoders.atlassian.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.*;
import com.atlassian.util.concurrent.NotNull;

public class RetryTask implements TaskType
{
    @NotNull
    @java.lang.Override
    public TaskResult execute(@NotNull final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final String checkboxEnabled = taskContext.getConfigurationMap().get("checkboxEnabled");
        buildLogger.addBuildLogEntry("Retry Task status (true: enabled / null: disabled): " + checkboxEnabled);

        return TaskResultBuilder.create(taskContext).success().build();
    }
}