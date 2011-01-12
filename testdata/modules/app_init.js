goog.require('example.App');

goog.require('goog.module.ModuleLoader');
goog.require('goog.module.ModuleManager');

example.App.setButtonClickHandler(function(e) {
  var moduleManager = goog.module.ModuleManager.getInstance();
  moduleManager.execOnLoad('settings', this.onSettingsLoaded, this);
});

example.App.install('content');

var moduleManager = goog.module.ModuleManager.getInstance();
var moduleLoader = new goog.module.ModuleLoader();

// Normally, this would be:
// moduleLoader.setDebugMode(!!goog.global['PLOVR_MODULE_USE_DEBUG_MODE']);
// But because this is still used with local files in "production," debug mode
// should always be used.
moduleLoader.setDebugMode(true);

moduleManager.setLoader(moduleLoader);
moduleManager.setAllModuleInfo(goog.global['PLOVR_MODULE_INFO']);
moduleManager.setModuleUris(goog.global['PLOVR_MODULE_URIS']);

// This tells the module manager that the 'app' module has been loaded.
// The module manager will not evaluate the code for any of app's
// dependencies until it knows it has been loaded.
moduleManager.setLoaded('app');

// One problem with this use of exports is that it causes problems in RAW mode.
// These calls to goog.exportSymbol() define an example.api object in the global
// namespace. Therefore, when api.js is loaded in RAW mode, the call to
// goog.provide('example.api') throws an error because the pre-existing
// example.api object results in a "Namespace already declared" error.
//
// This is not a problem in SIMPLE or ADVANCED mode because the Compiler
// sets the value of goog.DEBUG such that goog.provide() no longer checks for
// duplicate namespaces at runtime because it assumes that the Compiler has
// already addressed such issues at compile time.
//
// The easiest way to solve this problem for RAW mode would be to add the
// following two exports in their own namespace, though that would likely be
// awkward for clients of the API. This is effectively what Gmail's Greasemonkey
// API does (http://code.google.com/p/gmail-greasemonkey/wiki/GmailGreasemonkey10API):
// the loading code is in the gmonkey.* namespace while the Gmail API is in the
// gmail.* namespace.

goog.exportSymbol('example.api.load', function(callback) {
  moduleManager.execOnLoad('api', callback);
});

goog.exportSymbol('example.api.isLoaded', function() {
  var moduleInfo = moduleManager.getModuleInfo('api');
  return moduleInfo ? moduleInfo.isLoaded() : false;
});

// TODO(bolinfest): Include deps.js by default to eliminate the need for this.
goog.addDependency('', [
  'goog.debug.ErrorHandler',
  'goog.Uri'
], []);
