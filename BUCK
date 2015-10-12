java_binary(
  name = 'plovr',
  deps = [':plovr-lib'],
  main_class = 'org.plovr.cli.Main')

java_library(
  name = 'plovr-lib',
  srcs = glob(['src/**/*.java']),
  resources = glob([
    'src/**/*.js',
    'src/**/*.soy',
    'src/**/*.ts',
  ]),
  source = '7',
  target = '7',
  deps = [
    '//closure/closure-compiler:gson',
    ':selenium',
    '//closure/closure-compiler:args4j',
    '//closure/closure-compiler:closure-compiler',
    '//closure/closure-compiler:guava',
    '//closure/closure-compiler:jsr305',
    '//closure/closure-compiler:protobuf',
    '//closure/closure-stylesheets:closure-stylesheets',
    '//closure/closure-templates:closure-templates',
    '//closure/closure-templates:guice',
    '//closure/closure-templates:guice-assistedinject',
    '//closure/closure-templates:guice-multibindings',
  ],
)

java_test(
  name = 'test',
  srcs = glob(['test/**/*.java']),
  resources = glob(['test/**/*.js']) + [
    'library_manifest.txt',
    'third_party_manifest.txt',
    'externs_manifest.txt',
  ],
  deps = [
    '//closure/closure-compiler:gson',
    ':junit',
    ':plovr-lib',
    '//closure/closure-compiler:closure-compiler',
    '//closure/closure-compiler:jsr305',
    '//closure/closure-stylesheets:closure-stylesheets',
    '//closure/closure-templates:closure-templates',
    '//closure/closure-templates:guava',
  ],
)

prebuilt_jar(
  name = 'gson',
  binary_jar = 'lib/gson-2.2.2.jar',
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
