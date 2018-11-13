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

import com.synopsys.integration.exception.IntegrationException
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.LogLevel
import com.synopsys.integration.log.PrintStreamIntLogger
import com.synopsys.integration.rest.connection.BasicRestConnection
import com.synopsys.integration.rest.connection.RestConnection
import com.synopsys.integration.rest.credentials.Credentials
import com.synopsys.integration.rest.exception.IntegrationRestException
import com.synopsys.integration.rest.proxy.ProxyInfo
import com.synopsys.integration.rest.proxy.ProxyInfoBuilder
import com.synopsys.integration.rest.request.Request
import com.synopsys.integration.rest.request.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.apache.commons.codec.Charsets
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.charset.Charset

class RestConnectionTest {
    public static final int CONNECTION_TIMEOUT = 213

    private final MockWebServer server = new MockWebServer()

    @Before
    void setUp() throws Exception {
        server.start()
    }

    @After
    void tearDown() throws Exception {
        server.shutdown()
    }

    private String getValidUri() {
        return server.url("www.synopsys.com").uri()
    }

    private RestConnection getRestConnection() {
        getRestConnection(new MockResponse().setResponseCode(200))
    }

    private RestConnection getRestConnection(MockResponse response) {
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                response
            }
        }
        server.setDispatcher(dispatcher)

        return new BasicRestConnection(new PrintStreamIntLogger(System.out, LogLevel.TRACE), CONNECTION_TIMEOUT, false, ProxyInfo.NO_PROXY_INFO)
    }

    @Test
    void testClientBuilding() {
        IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO)
        int timeoutSeconds = 213
        int timeoutMilliSeconds = timeoutSeconds * 1000

        RestConnection restConnection = new BasicRestConnection(logger, timeoutSeconds, true, ProxyInfo.NO_PROXY_INFO)
        def realClient = restConnection.client
        assert null == realClient
        restConnection.initialize()
        realClient = restConnection.client
        assert timeoutMilliSeconds == realClient.defaultConfig.socketTimeout
        assert timeoutMilliSeconds == realClient.defaultConfig.connectionRequestTimeout
        assert timeoutMilliSeconds == realClient.defaultConfig.connectTimeout
        assert null == realClient.defaultConfig.proxy

        String proxyHost = "ProxyHost"
        int proxyPort = 3128
        ProxyInfoBuilder proxyBuilder = new ProxyInfoBuilder()
        proxyBuilder.host = proxyHost
        proxyBuilder.port = proxyPort
        proxyBuilder.credentials = new Credentials("testUser", "password")
        ProxyInfo proxyInfo = proxyBuilder.build()

        restConnection = new BasicRestConnection(logger, timeoutSeconds, true, proxyInfo)

        restConnection.initialize()
        realClient = restConnection.client
        assert null != realClient.defaultConfig.proxy
    }

    @Test
    void testRestConnectionNoProxy() {
        IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO)
        int timeoutSeconds = 213
        RestConnection restConnection = new BasicRestConnection(logger, timeoutSeconds, true, null)
        try {
            restConnection.initialize()
            fail('Should have thrown exception')
        } catch (IllegalStateException e) {
            assert RestConnection.ERROR_MSG_PROXY_INFO_NULL == e.getMessage()
        }
    }

    @Test
    void testHandleExecuteClientCallSuccessful() {
        RestConnection restConnection = getRestConnection()
        restConnection.commonRequestHeaders.put("Common", "Header")
        RequestBuilder requestBuilder = restConnection.createRequestBuilder(HttpMethod.DELETE)
        requestBuilder.setUri(getValidUri())
        assert null != requestBuilder.getHeaders("Common")

        Response response = restConnection.executeRequest(requestBuilder.build())

        assert 200 == response.getStatusCode()
    }

    @Test
    void testHandleExecuteClientCallFail() {
        RestConnection restConnection = getRestConnection()
        RequestBuilder requestBuilder = restConnection.createRequestBuilder(HttpMethod.GET)
        requestBuilder.setUri(getValidUri())
        HttpUriRequest request = requestBuilder.build()
        restConnection.initialize()

        restConnection = getRestConnection(new MockResponse().setResponseCode(404))
        try {
            restConnection.executeRequest(request)
            fail('Should have thrown exception')
        } catch (IntegrationRestException e) {
            assert 404 == e.httpStatusCode
        }

        restConnection = getRestConnection(new MockResponse().setResponseCode(401))
        try {
            restConnection.executeRequest(request)
            fail('Should have thrown exception')
        } catch (IntegrationRestException e) {
            assert 401 == e.httpStatusCode
        }
    }

    @Test
    void testCreateHttpRequestNoURI() {
        RestConnection restConnection = new BasicRestConnection(new PrintStreamIntLogger(System.out, LogLevel.TRACE), 300, true, ProxyInfo.NO_PROXY_INFO)
        Request request = new Request.Builder().build()
        try {
            request.createHttpUriRequest(restConnection.getCommonRequestHeaders())
            fail('Should have thrown exception')
        } catch (IntegrationException e) {
            assert "Missing the URI" == e.getMessage()
        }
    }

    @Test
    void testCreateHttpRequest() {
        RestConnection restConnection = getRestConnection()

        final String uri = getValidUri()
        Map<String, String> queryParametes = [test: "one", query: "two"]
        String q = 'q'
        String mimeType = 'mime'
        Charset bodyEncoding = Charsets.UTF_8

        Request request = new Request.Builder(uri).build()
        HttpUriRequest uriRequest = request.createHttpUriRequest(restConnection.getCommonRequestHeaders())
        assert HttpMethod.GET.name() == uriRequest.method
        assert ContentType.APPLICATION_JSON.getMimeType() == uriRequest.getFirstHeader(HttpHeaders.ACCEPT).getValue()
        assert null != uriRequest.getURI()
        assert uriRequest.getURI().toString().contains(uri)

        request = new Request.Builder(uri).build()
        uriRequest = request.createHttpUriRequest(restConnection.getCommonRequestHeaders())
        assert HttpMethod.GET.name() == uriRequest.method
        assert ContentType.APPLICATION_JSON.getMimeType() == uriRequest.getFirstHeader(HttpHeaders.ACCEPT).getValue()
        assert null != uriRequest.getURI()
        assert uriRequest.getURI().toString().contains(uri)

        request = new Request.Builder(uri).queryParameters([offset: ['0'] as Set, limit: ['100'] as Set]).build()
        uriRequest = request.createHttpUriRequest(restConnection.getCommonRequestHeaders())
        assert HttpMethod.GET.name() == uriRequest.method
        assert ContentType.APPLICATION_JSON.getMimeType() == uriRequest.getFirstHeader(HttpHeaders.ACCEPT).getValue()
        assert null != uriRequest.getURI()
        assert uriRequest.getURI().toString().contains(uri)
        assert uriRequest.getURI().toString().contains('offset=0')
        assert uriRequest.getURI().toString().contains('limit=100')

        request = new Request.Builder(uri).queryParameters([q: ['q'] as Set, test: ['one'] as Set, query: ['two'] as Set, offset: ['0'] as Set, limit: ['100'] as Set]).mimeType('mime').additionalHeaders([header: 'one', thing: 'two']).
            build()
        uriRequest = request.createHttpUriRequest(restConnection.getCommonRequestHeaders())
        assert HttpMethod.GET.name() == uriRequest.method
        assert 'one' == uriRequest.getFirstHeader('header').getValue()
        assert 'two' == uriRequest.getFirstHeader('thing').getValue()
        assert null != uriRequest.getURI()
        assert uriRequest.getURI().toString().contains(uri)
        assert uriRequest.getURI().toString().contains('offset=0')
        assert uriRequest.getURI().toString().contains('limit=100')

        Map headersMap = [header: 'one', thing: 'two']
        headersMap.put(HttpHeaders.ACCEPT, ContentType.APPLICATION_XML.getMimeType())
        request = new Request.Builder(uri).queryParameters([q: ['q'] as Set, test: ['one'] as Set, query: ['two'] as Set, offset: ['0'] as Set, limit: ['100'] as Set]).mimeType('mime').bodyEncoding(bodyEncoding).
            additionalHeaders(headersMap).build()
        uriRequest = request.createHttpUriRequest(restConnection.getCommonRequestHeaders())
        assert HttpMethod.GET.name() == uriRequest.method
        assert ContentType.APPLICATION_XML.getMimeType() == uriRequest.getFirstHeader(HttpHeaders.ACCEPT).getValue()
        assert null != uriRequest.getURI()
        assert uriRequest.getURI().toString().contains(uri)
        assert uriRequest.getURI().toString().contains('offset=0')
        assert uriRequest.getURI().toString().contains('limit=100')

        Request deleteRequest = new Request.Builder(uri).method(HttpMethod.DELETE).mimeType('mime').bodyEncoding(bodyEncoding).additionalHeaders([header: 'one', thing: 'two']).build()
        uriRequest = deleteRequest.createHttpUriRequest(restConnection.getCommonRequestHeaders())
        assert HttpMethod.DELETE.name() == uriRequest.method
        assert 'one' == uriRequest.getFirstHeader('header').getValue()
        assert 'two' == uriRequest.getFirstHeader('thing').getValue()
        assert 2 == uriRequest.getAllHeaders().size()
        assert null != uriRequest.getURI()
        assert uriRequest.getURI().toString().contains(uri)
    }
}
