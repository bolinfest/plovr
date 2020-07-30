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
  "paths": [
    "testdata",
    "../third-party/javascript"
  ],
  "inputs": "testdata/example/main.js",
  "externs": "../third-party/externs/",

  "options": {
    "basic": {
      "level": "SIMPLE_OPTIMIZATIONS"
    },
    "optimized": {
      "level": "ADVANCED_OPTIMIZATIONS"
    }
  }

}
