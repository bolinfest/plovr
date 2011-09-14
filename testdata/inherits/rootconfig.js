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
  "soy-function-plugins": "org.plovr.soy.function.PlovrModule"
}
