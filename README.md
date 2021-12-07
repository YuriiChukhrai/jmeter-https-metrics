# jmeter-https-metrics


## Dependencies
Make sure you have installed on your operating system:<br/>
1. [Java](http://www.java.com/) 
2. [Git](https://git-scm.com/)
3. [Maven](https://maven.apache.org/)


## Running Tests
The following steps should get you set up for running tests locally on your machine:

1. Clone this repository to your local machine.<br/>
2. All commands must be run from the `\jmeter-https-metrics` directory cloned during setup process above.<br/>


### Deploy

* `mvn clean install` - Run unit test, generate artifacts and install it to the M2 local repository.
* `mvn clean deploy` - Run unit test, generate artifacts and deploy it.
* `mvn clean -Dmaven.test.skip=true deploy` - Generate artifacts and deploy it without unit tests.


# References
- [SRC. JMeter. HttpMetricsSender](https://github.com/apache/jmeter/blob/master/src/components/src/main/java/org/apache/jmeter/visualizers/backend/influxdb/HttpMetricsSender.java)
- [SRC. JMeter. HttpMetricsSenderTest](https://github.com/apache/jmeter/blob/master/src/components/src/test/java/org/apache/jmeter/visualizers/backend/influxdb/HttpMetricsSenderTest.java)
- [SRC. JMeter. InfluxdbBackendListenerClient](https://github.com/apache/jmeter/blob/master/src/components/src/main/java/org/apache/jmeter/visualizers/backend/influxdb/InfluxdbBackendListenerClient.java)