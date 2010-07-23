{
  "id": "module-example",
  "paths": ".",
  "inputs": [
    "app_init.js",
    "api_init.js",
    "settings_init.js"
  ],
  "mode": "SIMPLE",
  "modules": {
    "deps": {
      "app": [],
      "api": ["app"],
      "settings": ["app"]
    },
    "output_path_prefix": "../../build/module-example/"
  }
}
