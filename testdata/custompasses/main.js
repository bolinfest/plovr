goog.provide('example.test');

example.test.main = function() {
  // This use of == will trigger a warning or error if CheckDoubleEquals is
  // enabled.
  if (Math.random() == 0.5) {
    alert('I won the lottery!');
  }
};

example.test.main();
