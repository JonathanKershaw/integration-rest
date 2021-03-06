/**
 * integration-rest
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.rest.credentials;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.builder.Buildable;
import com.synopsys.integration.util.Stringable;

public class Credentials extends Stringable implements Buildable {
    public static final Credentials NO_CREDENTIALS = new Credentials(null, null);

    public static CredentialsBuilder newBuilder() {
        return new CredentialsBuilder();
    }

    private static final String MASKED_PASSWORD = "************************";

    private final String username;
    private final String password;

    Credentials(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public String getMaskedPassword() {
        return MASKED_PASSWORD;
    }

    public boolean isBlank() {
        return StringUtils.isAllBlank(username, password);
    }

}
