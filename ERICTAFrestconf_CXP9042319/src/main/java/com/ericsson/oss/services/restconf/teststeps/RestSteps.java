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

import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.NODES_TO_ADD;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.ADDED_NODES;
import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.quartz.utils.FindbugsSuppressWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.cifwk.taf.tools.http.HttpTool;
import com.ericsson.cifwk.taf.tools.http.constants.HttpStatus;
import com.ericsson.cifwk.taf.utils.FileFinder;
import com.ericsson.oss.services.restconf.RestConfOperator;
import com.ericsson.oss.testware.enm.cli.EnmCliOperator;
import com.ericsson.oss.testware.enm.cli.EnmCliResponse;
import com.ericsson.oss.testware.enmbase.data.NetworkNode;
import com.ericsson.oss.testware.security.authentication.tool.HostProvider;
import com.ericsson.oss.testware.security.authentication.tool.TafToolProvider;
import com.ericsson.cifwk.taf.data.DataHandler;
import com.ericsson.cifwk.taf.data.Host;

/**
 * This class is for teststeps that will execute as part of RESTCONF application testing.
 */
public class RestSteps {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestSteps.class);
    protected static final String ZIP_ARCHIVE_NAME = "restConfTextFiles.zip";
    protected static final String ABSOLUTE_ZIP_ARCHIVE_LOCATION = FileFinder.findFile(ZIP_ARCHIVE_NAME).get(0);
    private static final String NODE_REST_CALL_RETRIES = "node.rest.call.retries";

    private static final String NODE_REST_CALL_RETRY_INTERVAL = "node.rest.call.retry.interval";
    private static final Integer NODE_REST_CALL_RETRIES_DEFAULT = 5;
    private static final Integer NODE_REST_CALL_RETRY_INTERVAL_DEFAULT = 3000;

    private static final Integer UNHANDLED_SYSTEM_ERROR_CODE = 9999;

    @Inject
    private Provider<EnmCliOperator> enmCliOperatorProvider;

    @Inject
    private TafToolProvider tafToolProvider;

    @Inject
    private Provider<RestConfOperator> restConfOperatorProvider;

    @Inject
    private HostProvider hostProvider;

    @TestStep(id = StepIds.GET_DATA)
    public void getData(@Input(Parameters.REST_URL) final String url, @Input(Parameters.METHOD) final String method,
            @Input(Parameters.OUTPUT_FILE) final String restConf,
            @Input(Parameters.RESPONSE_CODE) final String responseCode) {
        final HttpResponse actualResponse = restConfOperatorProvider.get().getData(url, method, getHttpTool());
        LOGGER.info("Response of get data: {}", actualResponse.getBody());
        LOGGER.info("RESPONSE CODE is {}", actualResponse.getResponseCode());
        verifyExpectedInResponse(actualResponse.getBody(), actualResponse.getResponseCode(), restConf, responseCode);
    }

    @TestStep(id = StepIds.REMOVE_LINKS)
    public NetworkNode addNode(@Input(ADDED_NODES) final NetworkNode node) {
        removeLinks(node, tafToolProvider.getHttpTool());
        return node;
    }

    public EnmCliResponse removeLinks(final NetworkNode node, final HttpTool httpTool) {
        String command = "link delete --node ";
        return executeRestCall(command+node.getNetworkElementId(), httpTool);
    }

    protected EnmCliResponse executeRestCall(final String command, final HttpTool httpTool) {
        final Integer numRetries = DataHandler.getConfiguration().getProperty(NODE_REST_CALL_RETRIES, NODE_REST_CALL_RETRIES_DEFAULT, Integer.class);
        final Integer interval =
                DataHandler.getConfiguration().getProperty(NODE_REST_CALL_RETRY_INTERVAL, NODE_REST_CALL_RETRY_INTERVAL_DEFAULT, Integer.class);

        boolean retry = true;
        int retryCount = 0;
        EnmCliResponse enmCliResponse = null;

        while (retry) {
            retryCount++;
            LOGGER.info("Executing CLI command: {}", command);
            enmCliResponse = enmCliOperatorProvider.get().executeCliCommand(command, httpTool);
            final int responseCode = enmCliResponse.getSummaryDto().getErrorCode();
            LOGGER.info("Received command response: {}", enmCliResponse.toString());

            if (UNHANDLED_SYSTEM_ERROR_CODE == responseCode && retryCount <= numRetries) {
                LOGGER.info("Retry attempt {}", retryCount);
                waitForInterval(interval);
            } else {
                retry = false;
            }
        }
        return enmCliResponse;
    }

    @FindbugsSuppressWarnings("THREAD_SLEEP")
    private void waitForInterval(final Integer interval) {
        LOGGER.info("Waiting for {} milliseconds.", interval);
        try {
            Thread.sleep(interval);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Interruption occured during wait interval - {}", e);
        }
    }

    public void verifyExpectedInResponse(final String actualResponseValue, final HttpStatus actualResponseCode,
            final String expectedInResponse, final String expectedResponseCode) {
        if (expectedResponseCode != null) {
            assertEquals(actualResponseCode.toString(), expectedResponseCode);
        }
        if (expectedInResponse != null) {
            ZipFile importZipArchive = null;
            String importZipArchiveLocation = null;
            String extractFileLocation = null;
            try {
                importZipArchive = new ZipFile(ABSOLUTE_ZIP_ARCHIVE_LOCATION);
                importZipArchiveLocation = importZipArchive.getName();
                extractFileLocation = importZipArchiveLocation.replace(ZIP_ARCHIVE_NAME, "");
                importZipArchive.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            extractFileFromArchiveToDirectory(importZipArchiveLocation, extractFileLocation, expectedInResponse);

            final File importFile = new File(extractFileLocation + expectedInResponse);
            LOGGER.info("File location : {}", importFile.getAbsolutePath());
            String contentnew = null;
            try {
                final FileInputStream fis = new FileInputStream(importFile.getAbsoluteFile());
                try {
                    contentnew = IOUtils.toString(fis, "UTF-8").toString();
                    final Host host = hostProvider.getHost();
                    LOGGER.info("hostname: {}", host.getIp());
                    contentnew = contentnew.replaceAll("hostname", host.getIp());
                    LOGGER.info("FINALLLLLLLL CONTENT {}", contentnew);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            assertEquals(actualResponseValue, contentnew);
        } else {
            LOGGER.info("No Expected Value provided, skipping verification");
        }
    }

    private HttpTool getHttpTool() {
        LOGGER.info("HttpTool is being get from TafToolProvider");
        return tafToolProvider.getHttpTool();
    }

    /**
     * This class contains constants for StepIds.
     */
    public static final class StepIds {
        public static final String GET_DATA = "getData";
        public static final String REMOVE_LINKS = "removeLinks";
        private StepIds() {
        }
    }

    public static final class Parameters {
        public static final String REST_URL = "url";
        public static final String METHOD = "method";
        public static final String OUTPUT_FILE = "outputFile";
        public static final String RESPONSE_CODE = "responseCode";

        private Parameters() {
        }
    }

    public static void extractFileFromArchiveToDirectory(String archiveLocation, String extractDirectory,
            String fileName) {
        ZipFile zipFile;
        ZipEntry zipEntry = null;
        try {
            zipFile = new ZipFile(archiveLocation);
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

            while (zipEntries.hasMoreElements()) {
                zipEntry = zipEntries.nextElement();
                String entryName = zipEntry.getName();
                if (entryName.equals(fileName)) {
                    File file = new File(extractDirectory + entryName);

                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = inputStream.read(bytes)) >= 0) {
                        fileOutputStream.write(bytes, 0, length);
                    }
                    inputStream.close();
                    fileOutputStream.close();
                }
            }
            zipFile.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
