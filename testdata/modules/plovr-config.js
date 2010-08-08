{
  "id": "module-example",
  "paths": ".",
  "inputs": [
    "app_init.js",
    "api_init.js",
    "settings_init.js"
  ],
  "mode": "ADVANCED",
  "modules": {
    "deps": {
      "app": [],
      "api": ["app"],
      "settings": ["app"]
    },
    "output_path": "../../build/module-example/module_%s.js",

    // For a local HTML page, production_uri happens to have the same value as
    // output_path, but for a production system, they would likely be different.
    "production_uri": "../../build/module-example/module_%s.js"
  }
}
