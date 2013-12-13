java_library(
  name = 'plovr',
  srcs = glob(['src/**/*.java']),
  resources = glob([
    'src/**/*.js',
    'src/**/*.soy',
    'src/**/*.ts',
  ]),
  source = '7',
  target = '7',
  deps = [
    ':gson',
    ':guava',
    ':selenium',
    '//closure/closure-compiler:args4j',
    '//closure/closure-compiler:closure-compiler',
    '//closure/closure-compiler:json',
    '//closure/closure-compiler:jsr305',
    '//closure/closure-compiler:protobuf',
    '//closure/closure-compiler:rhino',
    '//closure/closure-stylesheets:closure-stylesheets',
    '//closure/closure-templates:closure-templates',
    '//closure/closure-templates:guice',
    '//closure/closure-templates:guice-multibindings',
  ],
)

java_test(
  name = 'test',
  srcs = glob(['test/**/*.java']),
  resources = glob(['test/**/*.js']),
  deps = [
    ':gson',
    ':junit',
    ':plovr',
    ':guava',
    '//closure/closure-compiler:closure-compiler',
    '//closure/closure-compiler:jsr305',
    '//closure/closure-stylesheets:closure-stylesheets',
    '//closure/closure-templates:closure-templates',
  ],
)

prebuilt_jar(
  name = 'gson',
  binary_jar = 'lib/gson-2.2.2.jar',
)

prebuilt_jar(
  name = 'guava',
  binary_jar = 'lib/guava-14.0.1.jar',
)

prebuilt_jar(
  name = 'junit',
  binary_jar = 'lib/junit-4.11.jar',
  deps = [
    ':hamcrest-core',
    ':hamcrest-library',
  ],
)

prebuilt_jar(
  name = 'hamcrest-core',
  binary_jar = 'lib/hamcrest-core-1.3.jar',
)

prebuilt_jar(
  name = 'hamcrest-library',
  binary_jar = 'lib/hamcrest-library-1.3.jar',
  deps = [
    ':hamcrest-core',
  ],
)

prebuilt_jar(
  name = 'selenium',
  binary_jar = 'lib/selenium-java-2.21.0.jar',
)
