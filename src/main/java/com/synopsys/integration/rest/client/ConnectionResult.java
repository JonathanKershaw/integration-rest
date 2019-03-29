/**
 * integration-rest
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import java.util.Optional;

public class ConnectionResult {
    private final int httpStatusCode;
    private final String failureMessage;

    public static final ConnectionResult SUCCESS(int httpStatusCode) {
        return new ConnectionResult(httpStatusCode, null);
    }

    public static final ConnectionResult FAILURE(int httpStatusCode, String failureMessage) {
        return new ConnectionResult(httpStatusCode, failureMessage);
    }

    private ConnectionResult(int httpStatusCode, String failureMessage) {
        this.httpStatusCode = httpStatusCode;
        this.failureMessage = failureMessage;
    }

    public boolean isSuccess() {
        return !isFailure();
    }

    public boolean isFailure() {
        return getFailureMessage().isPresent();
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public Optional<String> getFailureMessage() {
        return Optional.ofNullable(failureMessage);
    }

}
