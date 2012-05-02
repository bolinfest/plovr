{
  "id": "name-collision-test",
  "paths": [
    "main",
    "custom",
    "../../closure/closure-templates/javascript/soyutils_usegoog.js"
  ],
  "inputs": "main.js",

  // These options must be used because plovr is not being run from a jar where
  // Closure resources are pre-packaged.
  "closure-library": "../../closure/closure-library/closure/goog/",
  "custom-externs-only": true,
  "externs": "../../closure/closure-compiler/externs/"
}
