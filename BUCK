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
  ]) + [
    'library_manifest.txt',
    'third_party_manifest.txt'
  ],
  source = '7',
  target = '7',
  deps = [
    '//closure/closure-compiler:args4j',
    '//closure/closure-compiler:closure-compiler',
    '//closure/closure-compiler:gson',
    '//closure/closure-compiler:guava',
    '//closure/closure-compiler:jsr305',
    '//closure/closure-compiler:protobuf',
    '//closure/closure-library:closure-library',
    '//closure/closure-stylesheets:closure-stylesheets',
    '//closure/closure-templates:closure-templates',
    '//closure/closure-templates:guice',
    '//closure/closure-templates:guice-assistedinject',
    '//closure/closure-templates:guice-multibindings',
    ':selenium',
    ':webdriver',
  ],
)

java_test(
  name = 'test',
  srcs = glob(['test/**/*.java']),
  resources = glob(['test/**/*.js']),
  deps = [
    '//closure/closure-compiler:closure-compiler',
    '//closure/closure-compiler:jsr305',
    '//closure/closure-compiler:junit',
    '//closure/closure-stylesheets:closure-stylesheets',
    '//closure/closure-templates:closure-templates',
    '//closure/closure-templates:guava',
    ':mockito',
    ':plovr-lib',
    '//closure/closure-compiler:gson',
  ],
)

prebuilt_jar(
  name = 'mockito',
  binary_jar = 'lib/mockito-core-1.10.19.jar',
)

prebuilt_jar(
  name = 'selenium',
  binary_jar = 'lib/selenium-java-2.53.0.jar',
)

prebuilt_jar(
  name = 'webdriver',
  binary_jar = 'lib/webdriver-2.6.jar',
)
