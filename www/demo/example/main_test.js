
goog.require('goog.testing.jsunit')
goog.require('example.templates')

function testBasic() {
  assertEquals('This is a basic test', 'This is a basic test');
}

function testSoyTemplate() {
  assertEquals('<h1>Title</h1>', example.templates.header({heading: 'Title'}))
}
