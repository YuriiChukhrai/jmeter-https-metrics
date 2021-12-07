package com.yc.qa.jmeter.visualizers.backend.influxdb;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * @author limit (Yurii Chukhrai)
 */

@AllArgsConstructor
public enum BackendInfluxdbProperties {

    BACKEND_INFLUXDB_CONNECTION_TIMEOUT("backend_influxdb.connection_timeout", 2000, ""),
    BACKEND_INFLUXDB_SOCKET_TIMEOUT("backend_influxdb.socket_timeout", 5000, ""),
    BACKEND_INFLUXDB_CONNECTION_REQUEST_TIMEOUT("backend_influxdb.connection_request_timeout", 100, ""),
    BACKEND_INFLUXDB_SEND_INTERVAL("backend_influxdb.send_interval", 5, "");

    @Getter
    private String propertiesName;

    @Getter
    private int propertiesValue;

    @Getter
    private String propertiesDescription;
}
