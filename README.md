# Geonosis #

Geonosis is a [Zookeeper](http://zookeeper.apache.org/) client daemon that synchronizes Zookeeper nodes with a local file system. It uses [Curator](http://curator.apache.org/) to access Zookeeper and is coded in Scala.

Zookeeper is a widely used and well tested system in the Java world but as soon as you leave the JVM, you have to rely on a module of your language binded to the Zookeeper C driver. A combination that might be far from being stable enough in all situations.

Geonosis is here to replace any (non Java) read-only system that could benefit of data kept in Zookeeper.

## Usage ##

```
./sbt run
```
or
```
./sbt run [-Dconfig.file=/path_to/application.conf]
```

Default parameters are located in [src/main/resources/application.conf](https://github.com/fasterize/geonosis/blob/master/src/main/resources/application.conf)

## How it works? ##

Geonosis will connect to Zookeeper servers listed in its configuration file and synchronize configured paths.

A typical Geonosis configuration file would look like this:
```
zookeeper {
  servers = ["localhost:2181"]
  paths = ["/test"]
}
syncdir = "/tmp/zookeeper"
port = 9000
validate_json = false
```

With this configuration file, Geonosis will connect to a Zookeeper server on `localhost:2181` and keep track of all children nodes under node `/test`. Files will be created on the local file system under `/tmp/zookeeper`.

For example, with the following Zookeeper nodes hierarchy:
```
/test
/test/test1
/test/test2
/test/test2/test21
```

The following files hierarchy will be created:
```
/tmp/zookeeper/test/test1/__data__
/tmp/zookeeper/test/test1/__metadata__
/tmp/zookeeper/test/test1/__data__
/tmp/zookeeper/test/test1/__metadata__
```

Data found in Zookeeper nodes are dumped into a `__data__` file and metadata in a `__metadata__` file formated as JSON.

Geonosis keeps track of any changes in Zookeeper watched nodes and will apply any changes to local files as soon as it gets notified by Zookeeper.

## Create a Debian package ##

You can create a Debian package of Geonosis with:
```sh
$ ./sbt clean debian:packageBin
```

A Debian package will be created in `./target`. After installing, the default configuration (in `/etc/geonosis/application.conf`) can be overridden with some configuration in `/etc/geonosis/geonosis.conf`.

## Build & Run Geonosis ##

You can compile and test geonosis with:
```sh
$ ./sbt
> test
> container:start
> ~ ;copy-resources;aux-compile
```

`~ ;copy-resources;aux-compile` automatically recompile source when it changes.
