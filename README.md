TestStreamer
=============

TestStreamer is a distriuted testing tool.
You do *NOT* need to deploy any tests to client machines. 

![architecture](http://farm8.staticflickr.com/7451/13056300083_727cdcb781_o.png)


## Features

* JUnit integration
* The setting of classpaths dynamically (You don't need to deploy test classes to a server in advance.)
* Dynamic class loading from server via WebSocket (You don't neet to deploy test classes to clients inadvance.)
* Auto-configuration of JNLP

## Prerequisites

* Leiningen 2
* Java8 or higher (for server), Java6 or higher (for client)

## Get started

### Server

Build client application and sign a jar file.

```shell
% cd client
% lein uberjar
% keytool -genkey -alias test-streamer
% jarsigner -keystore ~/.keystore -tsa http://timestamp.digicert.com target/client.jar test-streamer
```

Build server application and install to local.

```shell
% cd server
% lein install
```

Run the TestStreamer server.

```shell
% cd server
% cp ../client/target/client.jar .
% lein repl
```

### Client

Access `http://[TestStreamer server IP]:[TestStreamer server port (default: 5000)]/client` in a your browser.

And click the Launch button.


## Build setting for your application

As a typical use case, you'll use Apache Maven (surefire plugin) for running tests.
In that case, all you really do is add to pom.xml as following:

```xml
<profile>
  <id>test-streamer</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <skipTests>true</skipTests>
        </configuration>
      </plugin>
      <plugin>
        <groupId>net.unit8.maven.plugins</groupId>
        <artifactId>test-streamer-maven-plugin</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <configuration>
          <testStreamerUrl>http://test-streamer.local:5000</testStreamerUrl>
        </configuration>
        <executions>
          <execution>
            <goals><goal>submit</goal></goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```

## License

Eclipse Public License - v 1.0

