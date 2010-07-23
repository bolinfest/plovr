goog.require('example.App');
goog.require('example.Settings');
goog.require('goog.module.ModuleManager');

var app = example.App.getInstance();  
var settings = new example.Settings(app.getDomHelper());
app.addChild(settings, true /* opt_render */);

// This tells the module manager that the 'settings' module has been loaded;
// otherwise, the module manager will assume that loading has timed out and it
// will try again.
goog.module.ModuleManager.getInstance().setLoaded('settings');
