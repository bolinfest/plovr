{
  "comment": [
    "Because // was dropped from the JSON specification, this is the best way ",
    "to comment a JSON config file."
  ],

  "version": "1.0",

  "id": "testdata",

  "closure-library": "../workspace/closure-library/closure/goog",
  "deps": [
    "testdata",
    "../workspace/closure-templates/javascript/soyutils_usegoog.js"
  ],
  "inputs": "testdata/example/main.js",
  "externs": "../workspace/closure-compiler/externs/",

  "options": {
    "level": "ADVANCED_OPTIMIZATIONS"
  }

}
