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

package com.ericsson.oss.services.restconf.teststeps;

import com.ericsson.cifwk.taf.TafTestContext;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.oss.services.security.genericidentitymgmtcore.usermgmt.model.User;
import com.ericsson.cifwk.taf.datasource.DataRecord;

import com.ericsson.oss.testware.enmbase.data.CommonDataSources;
import com.ericsson.oss.testware.enmbase.data.ENMUser;

import com.ericsson.oss.testware.security.gim.operators.UserManagementRestOperator;

import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.inject.Inject;
import javax.inject.Provider;

/**
 * This class is for cleanup teststeps need to be performed before test execution.
 */
public class CleanupTestSteps {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupTestSteps.class);

    @Inject
    private Provider<UserManagementRestOperator> userManagementRestOperatorProvider;


    @TestStep(id = StepIds.CHECK_AND_CLEAN_USERS)
    public void getUsers(@Input(CommonDataSources.USERS_TO_CREATE) final ENMUser user) {
        final Iterator<DataRecord> listAllUsers = TafTestContext.getContext().dataSource(CommonDataSources.LIST_USERS).iterator();
        while (listAllUsers.hasNext()) {
            final User userRecord = listAllUsers.next().getFieldValue("user");
            final String userName = userRecord.getUserName();
            LOGGER.debug("Pointer on [{}] user in [listAllUsers]", userName);
            LOGGER.debug("username from usersToCreate.csv: {}. username from list: {}. Their condition: {}", user.getUsername(), userName,
                    userName.equalsIgnoreCase(user.getUsername()));
            if (!"administrator".equalsIgnoreCase(userName) && userName.equalsIgnoreCase(user.getUsername())) {
                LOGGER.info("Found {} users in list by comparing with [{}]. Going to delete it.", user.getUsername(), userName);
                userManagementRestOperatorProvider.get().deleteUser(user.getUsername());
                break;
            }
        }
    }

    /**
     * This class contains constants for StepIds.
     */
    public static final class StepIds {
        public static final String CHECK_AND_CLEAN_USERS = "checkAndCleanUsers";

        private StepIds() {
        }
    }
}
