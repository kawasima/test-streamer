Test Streamer
=============

Test Streamer is a distriuted testing tool.
You do *NOT* need to deploy any tests to client machines. 

![architecture](http://farm8.staticflickr.com/7451/13056300083_727cdcb781_o.png)


## Get started

Build client application and sign a jar file.

```shell
% cd client
% lein uberjar
% keytool -genkey -alias test-streamer
% jarsign target/client.jar test-streamer
```

Build server application and install to local.

```shell
% cd server
% lein install
```

Run an example server.

```shell
% cd examples/simple-server
% cp ../../client/target/client.jar .
% lein trampoline run
```

## License

Eclipse Public License - v 1.0

