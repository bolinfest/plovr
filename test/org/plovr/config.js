{
  "comment": [
    "Because // was dropped from the JSON specification, this is the best way ",
    "to comment a JSON config file while maintaining 80 column lines.",
    
    "This config file is used to test the main() method of ConfigParser.java.",
    "It assumes that each of the Closure Tools is checked out in a parallel ",
    "directory. (This is not a requirement when using the full binary.)"
  ],

  "version": "1.0",

  "id": "testdata",

  "closure-library": "../closure-library/closure/goog",
  "deps": [
    "testdata",
    "../closure-templates/javascript/soyutils_usegoog.js"
  ],
  "inputs": "testdata/example/main.js",
  "externs": "../closure-compiler/externs/",

  "options": {
    "level": "ADVANCED_OPTIMIZATIONS"
  }

}
