/*
* Re-factored by Yurii Chukhrai (limit)
* Added custom [SSL I/O Session Strategy] (TrustSelfSignedStrategy)
* for HTTPS self signed Grafana Web server certificate.
*
* */

package com.yc.qa.jmeter.visualizers.backend.influxdb;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.jmeter.report.utils.MetricUtils;
import org.apache.jmeter.util.JMeterUtils;

import javax.net.ssl.SSLContext;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class HttpsMetricsSender extends AbstractInfluxdbMetricsSender {

    private final Object lock = new Object();

    private List<MetricTuple> metrics = new ArrayList<>();

    private HttpPost httpRequest;

    private CloseableHttpAsyncClient httpClient;

    private URL url;

    private Future<HttpResponse> lastRequest;

    public HttpsMetricsSender() {
        super();
    }

    /**
     * The HTTP API is the primary means of writing data into InfluxDB, by
     * sending POST requests to the /write endpoint. Initiate the HttpClient
     * client with a HttpPost request from influxdb url
     *
     * @param influxdbUrl
     *            example : http://localhost:8086/write?db=myd&rp=one_week
     * @see org.apache.jmeter.visualizers.backend.influxdb.InfluxdbMetricsSender#setup(String)
     */
    @Override
    public void setup(String influxdbUrl) throws Exception {

        // Create I/O reactor configuration
        IOReactorConfig ioReactorConfig = IOReactorConfig
                .custom()
                .setIoThreadCount(1)
                .setConnectTimeout(JMeterUtils.getPropDefault(BackendInfluxdbProperties.BACKEND_INFLUXDB_CONNECTION_TIMEOUT.getPropertiesName(), BackendInfluxdbProperties.BACKEND_INFLUXDB_CONNECTION_TIMEOUT.getPropertiesValue()))
                .setSoTimeout(JMeterUtils.getPropDefault(BackendInfluxdbProperties.BACKEND_INFLUXDB_SOCKET_TIMEOUT.getPropertiesName(), BackendInfluxdbProperties.BACKEND_INFLUXDB_SOCKET_TIMEOUT.getPropertiesValue()))
                .build();

        // Create a custom I/O reactor
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);

        final SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        Registry <SchemeIOSessionStrategy> sessionStrategyRegistry = RegistryBuilder. <SchemeIOSessionStrategy> create ()
                        .register ("http", NoopIOSessionStrategy.INSTANCE)
                        .register ("https", new SSLIOSessionStrategy(sslContext, (hostname, session) -> true))
                        .build ();

        PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor, sessionStrategyRegistry);

        httpClient = HttpAsyncClientBuilder.create()
                .setConnectionManager(connManager)
                .setMaxConnPerRoute(2)
                .setMaxConnTotal(2)
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier((hostname, session) -> true)
                .setUserAgent("ApacheJMeter" +  JMeterUtils.getJMeterVersion())
                .disableCookieManagement()
                .disableConnectionState()
                .build();

        url = new URL(influxdbUrl);
        httpRequest = createRequest(url);
        httpClient.start();
    }

    /**
     * @param url {@link URL} Influxdb Url
     * @return {@link HttpPost}
     * @throws URISyntaxException
     */
    private HttpPost createRequest(URL url) throws URISyntaxException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(JMeterUtils.getPropDefault(BackendInfluxdbProperties.BACKEND_INFLUXDB_CONNECTION_TIMEOUT.getPropertiesName(), BackendInfluxdbProperties.BACKEND_INFLUXDB_CONNECTION_TIMEOUT.getPropertiesValue()))
                .setSocketTimeout(JMeterUtils.getPropDefault(BackendInfluxdbProperties.BACKEND_INFLUXDB_SOCKET_TIMEOUT.getPropertiesName(), BackendInfluxdbProperties.BACKEND_INFLUXDB_SOCKET_TIMEOUT.getPropertiesValue()))
                .setConnectionRequestTimeout(JMeterUtils.getPropDefault(BackendInfluxdbProperties.BACKEND_INFLUXDB_CONNECTION_REQUEST_TIMEOUT.getPropertiesName(), BackendInfluxdbProperties.BACKEND_INFLUXDB_CONNECTION_REQUEST_TIMEOUT.getPropertiesValue()))
                .build();

        HttpPost currentHttpRequest = new HttpPost(url.toURI());
        currentHttpRequest.setConfig(defaultRequestConfig);
        if(log.isDebugEnabled()){
            log.debug("Created InfluxDBMetricsSender with url: {}", url);
        }
        return currentHttpRequest;
    }

    @Override
    public void addMetric(String mesurement, String tag, String field) {
        synchronized (lock) {
            metrics.add(new MetricTuple(mesurement, tag, field, System.currentTimeMillis()));
        }
    }

    /**
     * @see org.apache.jmeter.visualizers.backend.graphite.GraphiteMetricsSender#
     *      writeAndSendMetrics()
     */
    @Override
    public void writeAndSendMetrics() {
        List<MetricTuple> copyMetrics;
        synchronized (lock) {
            if(metrics.isEmpty()) {
                return;
            }
            copyMetrics = metrics;
            metrics = new ArrayList<>(copyMetrics.size());
        }
        if (!copyMetrics.isEmpty()) {
            try {
                if(httpRequest == null) {
                    httpRequest = createRequest(url);
                }
                StringBuilder sb = new StringBuilder(copyMetrics.size()*35);
                for (MetricTuple metric : copyMetrics) {
                    // Add TimeStamp in nanosecond from epoch ( default in InfluxDB )
                    sb.append(metric.measurement)
                            .append(metric.tag)
                            .append(" ") //$NON-NLS-1$
                            .append(metric.field)
                            .append(" ")
                            .append(metric.timestamp+"000000")
                            .append("\n"); //$NON-NLS-1$
                }

                StringEntity entity = new StringEntity(sb.toString(), StandardCharsets.UTF_8);

                httpRequest.setEntity(entity);
                lastRequest = httpClient.execute(httpRequest, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(final HttpResponse response) {
                        int code = response.getStatusLine().getStatusCode();
                        /*
                         * HTTP response summary 2xx: If your write request received
                         * HTTP 204 No Content, it was a success! 4xx: InfluxDB
                         * could not understand the request. 5xx: The system is
                         * overloaded or significantly impaired.
                         */
                        if (MetricUtils.isSuccessCode(code)) {
                            if(log.isDebugEnabled()){
                                log.debug("Success, number of metrics written: {}", copyMetrics.size());
                            }
                        } else {
                            log.error("Error writing metrics to influxDB Url: {}, responseCode: {}, responseBody: {}", url, code, getBody(response));
                        }
                    }
                    @Override
                    public void failed(final Exception ex) {
                        log.error("failed to send data to influxDB server : {}", ex.getMessage());
                    }
                    @Override
                    public void cancelled() {
                        log.warn("Request to influxDB server was cancelled");
                    }
                });
            }catch (URISyntaxException ex ) {
                log.error(ex.getMessage());
            } finally {
                // We drop metrics in all cases
                copyMetrics.clear();
            }
        }
    }

    /**
     * @param response HttpResponse
     * @return String entity Body if any
     */
    private static String getBody(final HttpResponse response) {
        String body= "";
        try {
            if(response != null && response.getEntity() != null) {
                body = EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) { // NOSONAR
            // NOOP
        }
        return body;
    }

    /**
     * @see org.apache.jmeter.visualizers.backend.graphite.GraphiteMetricsSender#
     *      destroy()
     */
    @Override
    public void destroy() {
        // Give some time to send last metrics before shutting down
        log.info("Destroying ");
        try {
            lastRequest.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error waiting for last request to be send to InfluxDB", e);
        }

        if(httpRequest != null) {
            httpRequest.abort();
        }
        IOUtils.closeQuietly(httpClient);
    }
}//HttpsMetricsSender
