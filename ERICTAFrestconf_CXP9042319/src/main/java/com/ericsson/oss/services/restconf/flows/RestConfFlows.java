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

import com.ericsson.cifwk.taf.TestContext;

import com.ericsson.cifwk.taf.scenario.TestStepFlow;

import com.ericsson.cifwk.taf.scenario.api.TestStepFlowBuilder;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.annotatedMethod;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.ADDED_NODES;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.NODES_TO_ADD;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.AVAILABLE_USERS;

import javax.inject.Inject;

import com.ericsson.oss.services.restconf.scenario.RestScenarios;
import com.ericsson.oss.services.restconf.teststeps.RestSteps;
import com.ericsson.oss.testware.network.teststeps.NetworkElementTestSteps;
import com.ericsson.oss.testware.nodeintegration.flows.NodeIntegrationFlows;
import com.ericsson.oss.testware.nodesecurity.steps.SnmpV3TestSteps;
import com.ericsson.oss.testware.security.gim.flows.UserManagementTestFlows;
import com.ericsson.oss.testware.enmbase.data.CommonDataSources;

/**
 * This class is for flows including add/delete node, getting data through rest.
 */
public class RestConfFlows {

    private static final String MINI_LINK_INDOOR_PLATFORM_TYPE_FILTER = "platformType == 'MINI-LINK-Indoor'";

    @Inject
    private TestContext context;

    @Inject
    private UserManagementTestFlows userManagementTestFlows;

    @Inject
    private NetworkElementTestSteps networkElementTestSteps;

    @Inject
    private NodeIntegrationFlows nodeIntegrationFlows;

    @Inject
    private SnmpV3TestSteps snmpV3TestSteps;

    @Inject
    private RestSteps restSteps;

    /**
     * Flow to start the Netsim nodes.
     *
     * @param dataSource
     *            test step data source
     * @return test step flow
     */
    public TestStepFlow startNetsimNodes(final String dataSource) {
        return flow("Start Netsim nodes flow")
                .addTestStep(annotatedMethod(networkElementTestSteps, NetworkElementTestSteps.StepIds.START_NODE))
                .withDataSources(dataSource(dataSource).allowEmpty()).build();
    }

    /**
     * Flow to add and put in sync the nodes and verify the correct creation on NCM
     * database.
     *
     * @return test step flow
     */
    public TestStepFlow addAndSyncAndNormalizeNodes(final String dataSource) {
        return flow("Node agnostic add and sync node").addSubFlow(nodeIntegrationFlows.addNode())
                .addSubFlow(addMiniLinkIndoorNodes()).addSubFlow(nodeIntegrationFlows.syncNode())
                .addSubFlow(nodeIntegrationFlows.normalizeNode())
                .withDataSources(dataSource(dataSource).bindTo(NODES_TO_ADD)).build();
    }

    /**
     * Flow to add MINI-LINK-INDOOR nodes.
     *
     * @return test step flow
     */
    public TestStepFlow addMiniLinkIndoorNodes() {
        return flow("Add MINI-LINK-INDOOR nodes flow").addSubFlow(snmpV3CreateFlow()).build();
    }

    /**
     * Flow to delete nodes and verify the correct deletion also on NCM database.
     *
     * @return test step flow
     */
    public TestStepFlow deleteNodes() {
        return flow("Node agnostic delete node").addSubFlow(nodeIntegrationFlows.deleteNode())
                .withDataSources(dataSource(ADDED_NODES)).build();
    }
    
    /**
     * Flow to delete links for the nodes.
     *
     * @return test step flow
     */
    public TestStepFlow deleteLinks() {
        return flow("Node agnostic delete links").addSubFlow(deleteAllLinks())
                .withDataSources(dataSource(ADDED_NODES)).build();
    }

    /**
     * Flow to add a node using SNMPv3 settings.
     *
     * @return test step flow
     */
    private TestStepFlowBuilder snmpV3CreateFlow() {
        return flow("SNMP V3 Create flow").addTestStep(annotatedMethod(snmpV3TestSteps, SnmpV3TestSteps.SNMPV3_CREATE))
                .withDataSources(
                        dataSource(ADDED_NODES).withFilter(MINI_LINK_INDOOR_PLATFORM_TYPE_FILTER).allowEmpty());
    }

    public TestStepFlow getData(final String filter) {
        return flow("Get Data for REST call").addSubFlow(restFlow()
                .withDataSources(dataSource(RestScenarios.DataSources.REST_DATA).withFilter(filter).allowEmpty()))
                .build();
    }

    public TestStepFlowBuilder restFlow() {
        return flow("REST calls flow").addTestStep(annotatedMethod(restSteps, RestSteps.StepIds.GET_DATA));
    }
    
    public TestStepFlowBuilder deleteAllLinks() {
        return flow("Delete Links flow").addTestStep(annotatedMethod(restSteps, RestSteps.StepIds.REMOVE_LINKS));
    }

    public TestStepFlow deleteUser() {
        context.addDataSource(CommonDataSources.USERS_TO_DELETE, context.dataSource(AVAILABLE_USERS));
        return flow("Delete Users").addSubFlow(userManagementTestFlows.deleteUser()).build();
    }

}
