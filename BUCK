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
    '//tools/imports:revs',
    ':third-party-support-libs',
    '//third-party:COMPILE',
    '//closure/closure-library:closure-library'
  ],
)

java_library(
  name = 'third-party-support-libs',
  srcs = [],
  resources = [
    '//third-party/javascript:soyutils_usegoog.js'
  ],
  resources_root = './third-party')

java_test(
  name = 'test',
  srcs = glob(['test/**/*.java']),
  resources = glob(['test/**/*.js']),
  deps = [
    ':plovr-lib',
    '//third-party:COMPILE',
    '//third-party:TEST',
  ],
)
