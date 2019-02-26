/**
 * integration-rest
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.rest.client;

import java.io.IOException;

import org.apache.http.client.methods.HttpUriRequest;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.rest.RestConstants;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.request.Response;

public abstract class AuthenticatingIntHttpClient extends IntHttpClient {
    public AuthenticatingIntHttpClient(IntLogger logger, int timeoutInSeconds, boolean alwaysTrustServerCertificate, ProxyInfo proxyInfo) {
        super(logger, timeoutInSeconds, alwaysTrustServerCertificate, proxyInfo);
    }

    public abstract boolean isAlreadyAuthenticated(HttpUriRequest request);

    public abstract Response attemptAuthentication() throws IntegrationException;

    public final void authenticateRequest(HttpUriRequest request) throws IntegrationException {
        try (Response response = attemptAuthentication()) {
            completeAuthenticationRequest(request, response);
        } catch (IOException e) {
            throw new IntegrationException("The request could not be authenticated with the provided credentials: " + e.getMessage(), e);
        }
    }

    protected abstract void completeAuthenticationRequest(HttpUriRequest request, Response response) throws IntegrationException;

    @Override
    public Response execute(HttpUriRequest request) throws IntegrationException {
        return retryExecute(request, 0);
    }

    private Response retryExecute(HttpUriRequest request, int retryCount) throws IntegrationException {
        if (!isAlreadyAuthenticated(request)) {
            authenticateRequest(request);
        }
        Response response = super.execute(request);

        boolean notOkay = isUnauthorizedOrForbidden(response);

        if (notOkay && retryCount < 2) {
            return retryExecute(request, retryCount + 1);
        } else if (notOkay) {
            response.throwExceptionForError();
        }

        return response;
    }

    public final boolean isUnauthorizedOrForbidden(Response response) {
        Integer statusCode = response.getStatusCode();
        return null == statusCode || statusCode == RestConstants.UNAUTHORIZED_401 || statusCode == RestConstants.FORBIDDEN_403;
    }

    public boolean canConnect() {
        ConnectionResult connectionResult = attemptConnection();
        return connectionResult.isSuccess();
    }

    public ConnectionResult attemptConnection() {
        String errorMessage = null;
        int httpStatusCode = 0;

        try {
            try (Response response = attemptAuthentication()) {
                // if you get an error response, you know that a connection could not be made
                httpStatusCode = response.getStatusCode();
                if (response.isStatusCodeError()) {
                    errorMessage = response.getContentString();
                }
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
        }

        if (null != errorMessage) {
            logger.error(errorMessage);
            return ConnectionResult.FAILURE(httpStatusCode, errorMessage);
        }

        logger.info("A successful connection was made.");
        return ConnectionResult.SUCCESS(httpStatusCode);
    }

}
