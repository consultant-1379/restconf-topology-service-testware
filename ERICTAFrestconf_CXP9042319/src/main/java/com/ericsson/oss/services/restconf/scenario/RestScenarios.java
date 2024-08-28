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

import com.ericsson.cifwk.taf.TestContext;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.cifwk.taf.scenario.TestStepFlow;
import com.ericsson.cifwk.taf.scenario.api.TestStepFlowBuilder;
import com.ericsson.oss.services.restconf.flows.RestConfFlows;
import com.ericsson.oss.testware.enmbase.data.CommonDataSources;
import com.ericsson.oss.testware.security.authentication.flows.LoginLogoutRestFlows;
import com.ericsson.oss.testware.security.gim.steps.UserManagementTestSteps;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.annotatedMethod;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataDrivenScenario;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;

import javax.inject.Inject;

/**
 * This class is for scenarios that will execute as part of RESTCONF application testing.
 */
public class RestScenarios {

    public static final String PUT_TESTCASE_TO_CONTEXT = "putTestCaseToContext";
    public static final String SCENARIO_TESTCASE_ID = "SCENARIO_TESTCASE_ID";
    public static final String MATCHING_TESTCASE_ID_FILTER = "fTestCaseId == testCaseId";
    public static final String TESTCASE_ID = "testCaseId";
    public static final String REST_SCENARIO_TEST_CASE_IDS_DS = "restScenarioTestCaseIds";

    @Inject
    private RestConfFlows restFlows;

    @Inject
    private LoginLogoutRestFlows loginLogoutRestFlows;

    @Inject
    private UserManagementTestSteps userManagementTestSteps;

    @Inject
    TestContext context;

    /**
     * This Scenario is for REST calls.
     * 
     * @param testCaseIdsDataSource
     * @return Scenario
     */
    public TestScenario restScenario() {
        return dataDrivenScenario("Rest Scenario")
                .addFlow(flow("Getting Rest Data").addSubFlow(putTestCaseIdToContext()))
                .addFlow(loginLogoutRestFlows.login()).addFlow(restFlows.getData(MATCHING_TESTCASE_ID_FILTER))
                .addFlow(loginLogoutRestFlows.logout())
                .withScenarioDataSources(dataSource(REST_SCENARIO_TEST_CASE_IDS_DS)).build();
    }

    public static String getMatchingTestcaseIdFilter(final String testcaseId) {
        return "fTestCaseId == '" + testcaseId + "'";
    }

    public TestStepFlowBuilder putTestCaseIdToContext() {
        return flow("push test case to context").addTestStep(annotatedMethod(this, PUT_TESTCASE_TO_CONTEXT));
    }

    @TestStep(id = PUT_TESTCASE_TO_CONTEXT)
    public void putTestCaseIdToContext(@Input(TESTCASE_ID) final String testCaseId) {
        context.setAttribute(TESTCASE_ID, testCaseId);
    }

    public TestStepFlow createUser() {
        return flow("Create Users FLow").addSubFlow(loginLogoutRestFlows.loginDefaultUser())
                .addSubFlow(createEnmUsersFlow()).addSubFlow(loginLogoutRestFlows.logout()).alwaysRun()
                .addSubFlow(verifyUserLogin()).build();
    }

    private TestStepFlow verifyUserLogin() {
        return flow("Verify user can login")
                .addTestStep(annotatedMethod(userManagementTestSteps,
                        UserManagementTestSteps.TEST_STEP_VERIFY_USER_CAN_LOGIN))
                .withDataSources(dataSource(UserManagementTestSteps.USERS_TO_VERIFY_DATASOURCE)).build();
    }

    private TestStepFlow createEnmUsersFlow() {
        return flow("Create ENM Users Flow")
                .addTestStep(
                        annotatedMethod(userManagementTestSteps, UserManagementTestSteps.TEST_STEP_CREATE_ENM_USER))
                .withDataSources(dataSource(CommonDataSources.USERS_TO_CREATE)).build();
    }

    public static final class DataSources {
        public static final String REST_DATA = "restData";

        private DataSources() {
        }
    }
}
