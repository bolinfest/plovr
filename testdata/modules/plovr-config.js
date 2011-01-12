{
  "id": "module-example",
  "paths": ".",
  "mode": "ADVANCED",
  "level": "VERBOSE",

  "modules": {
    "app": {
      "inputs": "app_init.js",
      "deps": []
    },
    "api": {
      "inputs": "api_init.js",
      "deps": "app"
    },
    "settings": {
      "inputs": "settings_init.js",
      "deps": "app"
    }
  },
  "module-output-path": "../../build/module-example/module_%s.js",

  // For a local HTML page, production_uri happens to have the same value as
  // output_path, but for a production system, they would likely be different.
  "module-production-uri": "../../build/module-example/module_%s.js"
}
