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

package com.ericsson.oss.services.restconf;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.cifwk.taf.tools.http.HttpTool;
import com.ericsson.cifwk.taf.tools.http.RequestBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is for logic that will execute as part of RESTCONF application testing.
 */
public class RestConfOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfOperator.class);

    public HttpResponse getData(final String url, final String method, final HttpTool httpTool) {
        final HttpResponse response = sendJsonRequest(url, method, httpTool);
        return response;
    }

    public HttpResponse sendJsonRequest(final String url, final String method, final HttpTool httpToolVar) {
        LOGGER.info("REST URL is: {} , method: {}", url, method);
        RequestBuilder builder = httpToolVar.request().contentType("application/yang-data+json")
                .header(Constants.ACCEPT, "application/yang-data+json");
        return finalSendRequest(builder, method, url);
    }

    public HttpResponse finalSendRequest(final RequestBuilder builder, final String method, final String url) {
        HttpResponse response = null;

        switch (method) {
               case Constants.GET:
                    response = builder.get(url);
                    break;
               case Constants.PUT:
                    response = builder.put(url);
                    break;
               case Constants.POST:
                    response = builder.post(url);
                    break;
               case Constants.DELETE:
                    response = builder.delete(url);
                    break;
               default:
                   throw new IllegalArgumentException("Unsupported HTTP method " + method);
        }
        LOGGER.debug(response.getBody());
        return response;
    }

    /**
     * Class of constants used in CommonRestOperator.
     */
    public static final class Constants {
        public static final String PUT = "PUT";
        public static final String POST = "POST";
        public static final String DELETE = "DELETE";
        public static final String GET = "GET";
        public static final String ACCEPT = "Accept";
        public static final String HTTP_RESPONSE = "Http Response is {}";

        private Constants() {
        }
    }

}
