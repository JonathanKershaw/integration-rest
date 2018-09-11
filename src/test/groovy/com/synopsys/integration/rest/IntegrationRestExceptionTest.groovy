/**
 * Hub Common Rest
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
 * under the License.*/
package com.synopsys.integration.rest

import com.synopsys.integration.rest.exception.IntegrationRestException
import org.junit.Test

class IntegrationRestExceptionTest {
    @Test
    public void testConstruction() {
        int errorStatusCode = 404
        String errorStatusMessage = 'Four Oh Four'
        String errorMessage = 'Could not find the site'
        String errorContent = "error content";

        String expectedGetMessage = 'Could not find the site: 404: Four Oh Four'

        Exception error = new Exception(errorMessage)

        IntegrationRestException restException = new IntegrationRestException(errorStatusCode, errorStatusMessage, errorContent, errorMessage)
        assert errorStatusCode == restException.httpStatusCode
        assert errorStatusMessage.equals(restException.httpStatusMessage)
        assert errorContent.equals(restException.httpResponseContent)
        assert expectedGetMessage.equals(restException.message)
        assert error != restException.cause

        restException = new IntegrationRestException(errorStatusCode, errorStatusMessage, errorContent, error)
        assert errorStatusCode == restException.httpStatusCode
        assert errorStatusMessage.equals(restException.httpStatusMessage)
        assert errorContent.equals(restException.httpResponseContent)
        assert !expectedGetMessage.equals(restException.message)
        assert error == restException.cause

        restException = new IntegrationRestException(errorStatusCode, errorStatusMessage, errorContent, errorMessage, error)
        assert errorStatusCode == restException.httpStatusCode
        assert errorStatusMessage.equals(restException.httpStatusMessage)
        assert errorContent.equals(restException.httpResponseContent)
        assert expectedGetMessage.equals(restException.message)
        assert error == restException.cause

        restException = new IntegrationRestException(errorStatusCode, errorStatusMessage, errorContent, errorMessage, error, true, true)
        assert errorStatusCode == restException.httpStatusCode
        assert errorStatusMessage.equals(restException.httpStatusMessage)
        assert errorContent.equals(restException.httpResponseContent)
        assert expectedGetMessage.equals(restException.message)
        assert error == restException.cause
    }
}
