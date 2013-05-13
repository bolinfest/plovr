java_library(
  name = 'plovr',
  srcs = glob(['src/**/*.java']),
  resources = glob(['src/**/*.js']),
  deps = [
    ':closure-stylesheets',
    ':gson',
    ':guava',
    ':selenium',
    '//closure/closure-compiler:closure-compiler',
    '//closure/closure-compiler:rhino',
    '//closure/closure-templates:closure-templates',
    '//closure/closure-templates:guice',
    '//closure/closure-templates:guice-multibindings',
  ],
)

# TODO(mbolin): This does not work yet.
java_test(
  name = 'test',
  srcs = glob(['test/**/*.java']),
  resources = glob(['test/**/*.js']),
  deps = [
    ':closure-stylesheets',
    ':gson',
    ':junit',
    ':plovr',
    ':guava',
  ],
)

prebuilt_jar(
  name = 'closure-stylesheets',
  binary_jar = 'lib/closure-stylesheets-20130106.jar',
)

prebuilt_jar(
  name = 'gson',
  binary_jar = 'lib/gson-2.2.2.jar',
)

prebuilt_jar(
  name = 'guava',
  binary_jar = 'lib/guava-13.0.1.jar',
)

prebuilt_jar(
  name = 'junit',
  binary_jar = 'lib/junit-4.8.2.jar',
)

prebuilt_jar(
  name = 'selenium',
  binary_jar = 'lib/selenium-java-2.21.0.jar',
)
