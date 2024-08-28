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

package com.ericsson.oss.services.restconf.flows;

import com.ericsson.cifwk.taf.scenario.TestStepFlow;
import com.ericsson.cifwk.taf.scenario.api.TestStepFlowBuilder;
import com.ericsson.oss.services.restconf.teststeps.CleanupTestSteps;
import com.ericsson.oss.testware.enmbase.data.CommonDataSources;
import com.ericsson.oss.testware.security.gim.steps.UserManagementTestSteps;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.annotatedMethod;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;

import javax.inject.Inject;

/**
 * This class is for cleanup need to be performed before test execution.
 */

public class CleanupFlows {

    @Inject
    private CleanupTestSteps cleanupTestSteps;

    @Inject
    private UserManagementTestSteps userManagementTestSteps;

     /**
     * check and delete users.
     * @return TestStepFlowBuilder
     */
    public TestStepFlow checkUsersFlow() {
        return flow("Check and Clean Users Flow")
                .addTestStep(annotatedMethod(userManagementTestSteps, UserManagementTestSteps.TEST_STEP_GET_USERS))
                .addSubFlow(checkAndCleanUsers().withDataSources(dataSource(CommonDataSources.USERS_TO_CREATE))).build();
    }

    private TestStepFlowBuilder checkAndCleanUsers() {
        return flow("Check and Clean Users Flow").addTestStep(annotatedMethod(cleanupTestSteps, "checkAndCleanUsers"));
    }

}
