/*
 * ------------------------------------------------------------------------------
 * *******************************************************************************
 * * COPYRIGHT Ericsson 2022
 * *
 * * The copyright to the computer program(s) herein is the property of
 * * Ericsson Inc. The programs may be used and/or copied only with written
 * * permission from Ericsson Inc. or in accordance with the terms and
 * * conditions stipulated in the agreement/contract under which the
 * * program(s) have been supplied.
 * *******************************************************************************
 * *----------------------------------------------------------------------------
 */

package com.ericsson.oss.services.restconf.scenario;

import javax.inject.Inject;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.runner;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.scenario;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.TestContext;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.cifwk.taf.scenario.TestScenarioRunner;
import com.ericsson.cifwk.taf.scenario.api.ScenarioExceptionHandler;
import com.ericsson.cifwk.taf.scenario.impl.LoggingScenarioListener;
import com.ericsson.oss.services.restconf.flows.CleanupFlows;
import com.ericsson.oss.services.restconf.flows.RestConfFlows;
import com.ericsson.oss.testware.security.authentication.flows.LoginLogoutRestFlows;

/**
 * This class is for scenarios that will execute as part of RESTCONF application testing.
 */
public class RestConfManagementScenario extends TafTestBase {

    public static final String REST_NODES_TO_ADD = "nodesToAdd";

    @Inject
    private LoginLogoutRestFlows loginLogoutRestFlows;

    @Inject
    TestContext context;

    @Inject
    private RestScenarios restScenarios;

    @Inject
    private RestConfFlows restConfFlows;

    @Inject
    private RestScenarios restScenario;

    @Inject
    private CleanupFlows cleanupFlows;

    @BeforeSuite(groups = { "RFA" })
    public void setUp() {
       final TestScenario scenario = scenario("Before scenario - admin user login")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(cleanupFlows.checkUsersFlow())
                .addFlow(loginLogoutRestFlows.logout()).alwaysRun()
                .addFlow(restScenario.createUser())
                .addFlow(loginLogoutRestFlows.login()).addFlow(restConfFlows.startNetsimNodes(REST_NODES_TO_ADD))
                .addFlow(restConfFlows.addAndSyncAndNormalizeNodes(REST_NODES_TO_ADD))
                .addFlow(loginLogoutRestFlows.logout()).alwaysRun()
                .withExceptionHandler(ScenarioExceptionHandler.LOGONLY).build();
        executeScenario(scenario);
    }

    @Test(groups = { "RFA", "NSS" }, priority = 1, enabled = true)
    @TestSuite
    @TestId(id = "RESTCONF_73620_TAF_Interfaces_TC_02", title = "Rest scenario")
    public void restTest() {
        executeScenario(restScenarios.restScenario());
    }

    @AfterSuite(groups = { "RFA" })
    public void teardown() {
        final TestScenario scenario = scenario("After scenario - delete user")
                .addFlow(loginLogoutRestFlows.loginDefaultUser()).alwaysRun()
                .addFlow(restConfFlows.deleteLinks())
                .addFlow(restConfFlows.deleteNodes()).alwaysRun()
                .addFlow(restConfFlows.deleteUser()).alwaysRun()
                .addFlow(loginLogoutRestFlows.logout())
                .alwaysRun().withExceptionHandler(ScenarioExceptionHandler.LOGONLY).build();
        executeScenario(scenario);
    }

    protected void executeScenario(final TestScenario scenario) {
        final TestScenarioRunner runner = runner().withListener(new LoggingScenarioListener()).build();
        runner.start(scenario);
    }

    public static String defineSuiteNameFilter(final String suiteName) {
        return "suiteName == '" + suiteName + "'";
    }

}
