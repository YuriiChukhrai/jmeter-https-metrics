package com.yc.qa.jmeter.visualizers.backend.influxdb.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.yc.qa.jmeter.visualizers.backend.influxdb.HttpsMetricsSender;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

/**
 *
 * @author limit (Yurii Chukhrai)
 */

public class BaseTest {

    protected Function<Boolean, WireMockConfiguration> wireMockConfiguration = (isHttps) -> isHttps ? WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort() : WireMockConfiguration.wireMockConfig().dynamicPort();
    protected static final String API_URL = "/api/write";


    protected MappingBuilder influxRequest(CountDownLatch latch) {
        return WireMock.post(API_URL)
                .willReturn(WireMock.aResponse().withStatus(HttpURLConnection.HTTP_NO_CONTENT))
                .withPostServeAction("countdown", Parameters.one("latch", latch));
    }

    protected void setupSenderAndSendMetric(String influxDbUrl) throws Exception {
        HttpsMetricsSender httpsMetricsSender = new HttpsMetricsSender();
        httpsMetricsSender.setup(influxDbUrl);
        httpsMetricsSender.addMetric("measurement", ",location=west", "size=10");
        httpsMetricsSender.writeAndSendMetrics();
        httpsMetricsSender.destroy();
    }

    protected void assertAuthHeader(WireMockServer server, StringValuePattern authHeader) {
        server.verify(1, RequestPatternBuilder.newRequestPattern(RequestMethod.POST, WireMock.urlEqualTo(API_URL))
                .withRequestBody(WireMock.matching("measurement,location=west size=10 \\d{19}\\s*"))
                .withHeader("Authorization", authHeader));
    }
}
