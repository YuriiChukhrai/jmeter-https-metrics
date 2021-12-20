# jmeter-https-metrics

## Article
[Implementation of custom HTTPS sender for JMeter and InfluxDB](./doc/implementation_of_custom_https_metric_sender.md)

## Dependencies
Make sure you have installed on your operating system:<br/>
1. [Java](http://www.java.com/) 
2. [Git](https://git-scm.com/)
3. [Maven](https://maven.apache.org/)


## Running Tests
The following steps should get you set up for running tests locally on your machine:

1. Clone this repository to your local machine.<br/>
2. All commands must be run from the `\jmeter-https-metrics` directory cloned during setup process above.<br/>

## InfluxdbBackendListenerClient properties

* `backend_influxdb.connection_timeout` - default value 2000 ms.
* `backend_influxdb.socket_timeout` - default value 5000 ms.
* `backend_influxdb.connection_request_timeout` - default value 100 ms.
* `backend_influxdb.send_interval` - default value 5 seconds.



### Deploy

* `mvn clean install` - Run unit test, generate artifacts and install it to the M2 local repository.
* `mvn clean deploy` - Run unit test, generate artifacts and deploy it (the artifact server configurations should be provided in the [~/.m2/settings.xml]).
* `mvn clean -Dmaven.test.skip=true deploy` - Generate artifacts and deploy it without unit tests.


# References
- [JMeter. Real-time results](https://jmeter.apache.org/usermanual/realtime-results.html)
- [SRC. JMeter. HttpMetricsSender](https://github.com/apache/jmeter/blob/master/src/components/src/main/java/org/apache/jmeter/visualizers/backend/influxdb/HttpMetricsSender.java)
- [SRC. JMeter. HttpMetricsSenderTest](https://github.com/apache/jmeter/blob/master/src/components/src/test/java/org/apache/jmeter/visualizers/backend/influxdb/HttpMetricsSenderTest.java)
- [SRC. JMeter. InfluxdbBackendListenerClient](https://github.com/apache/jmeter/blob/master/src/components/src/main/java/org/apache/jmeter/visualizers/backend/influxdb/InfluxdbBackendListenerClient.java)