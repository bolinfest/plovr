Plovr: A Closure build tool
===========================

plovr is a build tool that dynamically recompiles JavaScript and Closure
Template code. It is designed to simplify Closure development, and to make it
more enjoyable.

[Using Plovr](http://plovr.org/docs.html)

Plovr requires Java 7 or higher.

### Downloading Plovr

You can find Plovr JARs for download 
[on the Releases page](https://github.com/bolinfest/plovr/releases)

### Building Plovr

The Plovr build requires [Buck](https://buckbuild.com/).

To test:

```
buck fetch ...
buck test
```

To build a release jar,

```
buck fetch ...
buck build plovr
```

The output will be in `buck-out/gen/plovr.jar`
