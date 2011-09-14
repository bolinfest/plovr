{
  "id": "root",
  "inputs": "root.js",
  "paths": "fakedir",
  "id-generators": ["goog.events.getUniqueId"],
  "custom-passes": [
    {
      "class-name": "org.plovr.CheckDoubleEquals",
      "when": "BEFORE_CHECKS"
    }
  ],
  "soy-function-plugins": "org.plovr.soy.function.PlovrModule",
  "modules": {
    "app": {
      "inputs": "app_init.js",
      "deps": []
    },
    "api": {
      "inputs": ["api.js", "api_init.js"],
      "deps": "app"
    }
  },
  "define": {
    "goog.userAgent.ASSUME_IE": true
  },
  "checks": {
    "checkTypes": "ERROR"
  },
  "experimental-compiler-options": {
    "instrumentForCoverage": true
  }
}
