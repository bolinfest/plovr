Plovr Build: A Closure build tool
===========================

Plovr Build is a build tool that dynamically recompiles JavaScript and Closure
Template code. It is designed to simplify Closure development, and to make it
more enjoyable.

[Using Plovr](http://plovr.org/docs.html)

Plovr requires Java 7 or higher.

### Downloading Plovr

You can find Plovr JARs for download 
[on the Releases page](https://github.com/bolinfest/plovr/releases)

### Building Plovr from Source

The Plovr build requires the Java Development Kit (JDK) 7 or higher, [Buck](https://buckbuild.com/), and `zip`.

When following the [Buck installation instructions](https://buckbuild.com/setup/getting_started.html),
double-check that you are following the instructions for building Java projects
and for your operating system. If you have trouble installing Buck
(which can be non-trivial on Windows), please contact the Buck team.

To test:

```
buck fetch ...
buck test
```

To build:

```
buck fetch ...
buck build plovr
```

The output of the build will be in `buck-out/gen/plovr.jar`.

### Building Plovr inside a Docker container

If you want to build plovr without installing Buck on your local machine,
we have a Docker container with Buck installed.

The Plovr build is split into two Dockerfiles:

- [Dockerfile](Dockerfile) builds and tests Plovr
- [Dockerfile.base](Dockerfile.base) builds Buck and Plovr's dependencies

To test:

```
docker build .
```

downloads the plovr-deps container from docker, adds Plovr source, and runs all the Plovr tests.

If you want to build `plovr-deps` yourself:

```
docker build -t nicks/plovr-deps -f Dockerfile.base .
```

### To Upgrade Closure Library

To upgrade one of Closure Library, go to the official repo and find the SHA digest
of the commit you want to sync to. Then run.

```
scripts/update-closure-library.sh sha-digest
```

Sometimes this doesn't work because `git subtree` is buggy. If nothing updates, try running:

```
scripts/update-closure-library.sh master
git reset --hard origin/master
scripts/update-closure-library.sh sha-digest
```

This will bully `git subtree` into shape.

### To Upgrade Closure Compiler, Closure Templates, or Closure Spreadsheets

The Closure Compiler, Template, and Spreadsheets depenencies are managed with Maven.

Follow the instructions in [third-party/README.md](third-party/README.md).
