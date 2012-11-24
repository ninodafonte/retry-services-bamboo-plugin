package com.aboutcoders.atlassian.bamboo;

import com.atlassian.bamboo.build.BuildExecutionManager;
import com.atlassian.bamboo.build.CustomBuildProcessorServer;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.configuration.AdministrationConfigurationManager;
import com.atlassian.bamboo.labels.LabelManager;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanExecutionManager;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.TopLevelPlan;
import com.atlassian.bamboo.plan.cache.ImmutablePlanCacheService;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.util.concurrent.NotNull;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.List;

public class RetryPostBuild implements CustomBuildProcessorServer {

    private LabelManager labelManager;
    private PlanManager planManager;
    private ResultsSummaryManager resultSummaryManager;
    private TransactionTemplate transactionTemplate;
    private PlanExecutionManager planExecutionManager;
    private BuildExecutionManager buildExecutionManager;
    private ImmutablePlanCacheService immutablePlanCacheService;
    private AdministrationConfigurationManager administrationConfigurationManager;
    private Chain chain;
    private TopLevelPlan topPlan;
    private BuildContext buildContext;

    private boolean retryFound = false;

    final static String pluginKey = "com.aboutcoders.atlassian.bamboo.RetryTask";
    final static String retryLabel = "RetryAutomaticExecution";

    final static String taskUsername = "admin";
    final static String taskPassword = "admin";

    @Override
    public void init(@NotNull final BuildContext buildContext)
    {
        this.buildContext = buildContext;
    }

    @NotNull
    @Override
    public BuildContext call() throws InterruptedException, Exception
    {
        BuildState state = buildContext.getBuildResult().getBuildState();
        final Plan currentPlan = planManager.getPlanByKey(buildContext.getPlanResultKey().getPlanKey());
        if (state == BuildState.FAILED)
        {
            List<TaskDefinition> definitions = buildContext.getBuildDefinition().getTaskDefinitions();
            List<TaskResult> taskResults = buildContext.getBuildResult().getTaskResults();

            for (TaskDefinition definition : definitions)
            {
                String currentPluginKey = definition.getPluginKey();
                if (currentPluginKey.startsWith(pluginKey))
                {
                    String value = definition.getConfiguration().get("checkboxEnabled");
                    if (value.equals("true"))
                    {
                        Object result = transactionTemplate.execute(new TransactionCallback() {
                            @Override
                            public Object doInTransaction() {
                                topPlan = (TopLevelPlan) planManager.getPlanByKey(getCustomPlanKey(currentPlan.getPlanKey().getKey()));
                                chain = planManager.getPlanById(topPlan.getId(), Chain.class);

                                List<ResultsSummary> summaries = resultSummaryManager.getLastNResultsSummaries(currentPlan, 1);
                                ResultsSummary previousJob = null;
                                if (summaries.size() == 1)
                                {
                                    previousJob = summaries.get(0);
                                }

                                if (previousJob != null)
                                {
                                    List<String> labels = previousJob.getLabelNames();
                                    if (labels.size() > 0)
                                    {
                                        for (String label : labels)
                                        {
                                            if (label.equalsIgnoreCase(retryLabel))
                                            {
                                                retryFound = true;
                                                break;
                                            }
                                        }
                                    }
                                }

                                return null;
                            }
                        });

                        if (!retryFound)
                        {
                            try {
                                // Setting this build as "Unknown" and label it
                                buildContext.getBuildResult().setBuildState(BuildState.UNKNOWN);
                                labelManager.addLabel(retryLabel, buildContext.getPlanResultKey(), null);
                                String baseUrl = administrationConfigurationManager.getAdministrationConfiguration().getBaseUrl();

                                // Creating a new build:
                                HttpClient client = new HttpClient();
                                client.getState().setCredentials(
                                        AuthScope.ANY,
                                        new UsernamePasswordCredentials(taskUsername, taskPassword)
                                );
                                client.getParams().setAuthenticationPreemptive(true);

                                String url = baseUrl + "/rest/api/latest/queue/" + getCustomPlanKey(currentPlan.getPlanKey().getKey());

                                PostMethod postMethod = new PostMethod(url);
                                postMethod.setDoAuthentication(true);
                                postMethod.setParameter("stage", "");
                                postMethod.setParameter("ExecuteAllStages", "");
                                client.executeMethod(postMethod);

                                if (postMethod.getStatusCode()!=200 && postMethod.getStatusCode()!=204) {

                                }
                            } catch (Exception e) {
                                // Exception
                            }
                        }

                    }
                }
            }
        }
        return buildContext;
    }

    public void setLabelManager(final LabelManager labelManager)
    {
        this.labelManager = labelManager;
    }

    public void setPlanManager(final PlanManager planManager)
    {
        this.planManager = planManager;
    }

    public void setResultSummaryManager(final ResultsSummaryManager resultSummaryManager)
    {
        this.resultSummaryManager = resultSummaryManager;
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setPlanExecutionManager(PlanExecutionManager planExecutionManager) {
        this.planExecutionManager = planExecutionManager;
    }

    public void setImmutablePlanCacheService(ImmutablePlanCacheService immutablePlanCacheService) {
        this.immutablePlanCacheService = immutablePlanCacheService;
    }

    public void setBuildExecutionManager(BuildExecutionManager buildExecutionManager) {
        this.buildExecutionManager = buildExecutionManager;
    }

    public void setAdministrationConfigurationManager(AdministrationConfigurationManager administrationConfigurationManager) {
        this.administrationConfigurationManager = administrationConfigurationManager;
    }

    public String getCustomPlanKey(String key)
    {
        return key.substring(0, key.lastIndexOf("-"));
    }
}
