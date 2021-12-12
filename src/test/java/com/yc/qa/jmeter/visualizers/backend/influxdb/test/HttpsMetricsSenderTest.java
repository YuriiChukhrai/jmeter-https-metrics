/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yc.qa.jmeter.visualizers.backend.influxdb.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HttpsMetricsSenderTest extends BaseTest {

    @DataProvider(parallel = true)
    public Iterator<WireMockServer> dp() {
        return Arrays.asList(new WireMockServer(wireMockConfiguration.apply(true)), new WireMockServer(wireMockConfiguration.apply(false))).iterator();
    }

    @Test(dataProvider = "dp", enabled = true)
    public void testSelfSignedCertificate(WireMockServer wireMockServer) throws Exception {

        Assert.assertTrue(Objects.nonNull(wireMockServer), "The instance of [WireMockServer] is NULL.");

        wireMockServer.start();
        CountDownLatch latch = new CountDownLatch(1);
        wireMockServer.stubFor( influxRequest(latch) );

        setupSenderAndSendMetric( wireMockServer.url(API_URL) );

        latch.await(2, TimeUnit.SECONDS);
        assertAuthHeader(wireMockServer, WireMock.absent());

        wireMockServer.shutdown();
    }
}
