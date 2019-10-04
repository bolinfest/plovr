// Copyright 2009 The Closure Library Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

goog.provide('goog.events.BrowserEventTest');
goog.setTestOnly('goog.events.BrowserEventTest');

goog.require('goog.events.BrowserEvent');
goog.require('goog.events.BrowserFeature');
goog.require('goog.math.Coordinate');
goog.require('goog.testing.PropertyReplacer');
goog.require('goog.testing.jsunit');
goog.require('goog.userAgent');

var stubs = new goog.testing.PropertyReplacer();
var Button = goog.events.BrowserEvent.MouseButton;
var PointerType = goog.events.BrowserEvent.PointerType;

function setUp() {
  stubs.reset();
}


/**
 * @see https://bugzilla.mozilla.org/show_bug.cgi?id=497780
 */
function testInvalidNodeBug() {
  if (!goog.userAgent.GECKO) return;

  var event = {};
  event.relatedTarget = {};
  event.relatedTarget.__defineGetter__('nodeName', function() {
    throw new Error('https://bugzilla.mozilla.org/show_bug.cgi?id=497780');
  });
  assertThrows(function() { return event.relatedTarget.nodeName; });

  var bEvent = new goog.events.BrowserEvent(event);
  assertEquals(event, bEvent.event_);
  assertNull(bEvent.relatedTarget);
}

function testPreventDefault() {
  var event = {};
  event.defaultPrevented = false;
  var bEvent = new goog.events.BrowserEvent(event);
  assertFalse(bEvent.defaultPrevented);
  bEvent.preventDefault();
  assertTrue(bEvent.defaultPrevented);
}

function testDefaultPrevented() {
  var event = {};
  event.defaultPrevented = true;
  var bEvent = new goog.events.BrowserEvent(event);
  assertTrue(bEvent.defaultPrevented);
}

function testIsButtonIe() {
  stubs.set(goog.events.BrowserFeature, 'HAS_W3C_BUTTON', false);
  assertIsButton(createMouseEvent('mousedown', 1), Button.LEFT, true);
  assertIsButton(createMouseEvent('click', 0), Button.LEFT, true);
  assertIsButton(createMouseEvent('mousedown', 2), Button.RIGHT, false);
  assertIsButton(createMouseEvent('mousedown', 4), Button.MIDDLE, false);
}

function testIsButtonWebkitMac() {
  stubs.set(goog.events.BrowserFeature, 'HAS_W3C_BUTTON', true);
  stubs.set(goog.userAgent, 'WEBKIT', true);
  stubs.set(goog.userAgent, 'MAC', true);
  assertIsButton(createMouseEvent('mousedown', 0), Button.LEFT, true);
  assertIsButton(createMouseEvent('mousedown', 0, true), Button.LEFT, false);
  assertIsButton(createMouseEvent('mousedown', 2), Button.RIGHT, false);
  assertIsButton(createMouseEvent('mousedown', 2, true), Button.RIGHT, false);
  assertIsButton(createMouseEvent('mousedown', 1), Button.MIDDLE, false);
  assertIsButton(createMouseEvent('mousedown', 1, true), Button.MIDDLE, false);
}

function testIsButtonGecko() {
  stubs.set(goog.events.BrowserFeature, 'HAS_W3C_BUTTON', true);
  stubs.set(goog.userAgent, 'GECKO', true);
  stubs.set(goog.userAgent, 'MAC', true);
  assertIsButton(createMouseEvent('mousedown', 0), Button.LEFT, true);
  assertIsButton(createMouseEvent('mousedown', 2, true), Button.RIGHT, false);
}

function testTouchEventHandling() {
  var clientCoords = new goog.math.Coordinate(5, 5);
  var screenCoords = new goog.math.Coordinate(10, 10);
  var target = document.body;
  var touchStart =
      createTouchEvent('touchstart', target, clientCoords, screenCoords);
  var touchMove =
      createTouchEvent('touchmove', target, clientCoords, screenCoords);
  var touchEnd =
      createTouchEvent('touchend', target, clientCoords, screenCoords);
  var touchCancel =
      createTouchEvent('touchcancel', target, clientCoords, screenCoords);

  assertEquals(clientCoords.x, touchStart.clientX);
  assertEquals(clientCoords.y, touchStart.clientY);
  assertEquals(target, touchStart.target);

  assertEquals(screenCoords.x, touchMove.screenX);
  assertEquals(screenCoords.y, touchMove.screenY);

  assertEquals(clientCoords.x, touchEnd.clientX);
  assertEquals(clientCoords.y, touchEnd.clientY);

  assertEquals(screenCoords.x, touchCancel.screenX);
  assertEquals(screenCoords.y, touchCancel.screenY);
  assertEquals(target, touchCancel.target);
}

function testGuardAgainstUndefinedTouchCoordinates() {
  const noChangedTouches = new goog.events.BrowserEvent({
    type: 'touchstart',
    target: document.body,
    changedTouches: [],
  });

  const emptyTouchObject = new goog.events.BrowserEvent({
    type: 'touchstart',
    target: document.body,
    changedTouches: [{}],
  });

  const onlyPageCoords = new goog.events.BrowserEvent({
    type: 'touchstart',
    target: document.body,
    changedTouches: [{pageX: 6, pageY: 7}],
  });

  assertEquals(undefined, noChangedTouches.clientX);
  assertEquals(undefined, noChangedTouches.clientY);
  assertEquals(0, noChangedTouches.screenX);
  assertEquals(0, noChangedTouches.screenY);

  assertEquals(undefined, emptyTouchObject.clientX);
  assertEquals(undefined, emptyTouchObject.clientY);
  assertEquals(0, emptyTouchObject.screenX);
  assertEquals(0, emptyTouchObject.screenY);

  assertEquals(6, onlyPageCoords.clientX);
  assertEquals(7, onlyPageCoords.clientY);
  assertEquals(0, onlyPageCoords.screenX);
  assertEquals(0, onlyPageCoords.screenY);
}

function testPointerEvent() {
  var event = createPointerEvent('pointerdown', 123, PointerType.MOUSE);
  assertEquals(123, event.pointerId);
  assertEquals(PointerType.MOUSE, event.pointerType);
}

function testMSPointerEvent() {
  var event = createPointerEvent('MSPointerDown', 123, 4 /* mouse */);
  assertEquals(123, event.pointerId);
  assertEquals(PointerType.MOUSE, event.pointerType);
}

function testUnsupportedPointerEvent() {
  var event = createMouseEvent('mousedown', 1);
  assertEquals(0, event.pointerId);
  assertEquals('', event.pointerType);
}

/**
 * @param {string} type
 * @param {number} button
 * @param {boolean=} opt_ctrlKey
 * @return {!goog.events.BrowserEvent}
 */
function createMouseEvent(type, button, opt_ctrlKey) {
  return new goog.events.BrowserEvent(
      {type: type, button: button, ctrlKey: !!opt_ctrlKey});
}

/**
 * @param {string} type
 * @param {!Element} target
 * @param {!goog.math.Coordinate} clientCoords
 * @param {!goog.math.Coordinate} screenCoords
 * @return {!goog.events.BrowserEvent}
 */
function createTouchEvent(type, target, clientCoords, screenCoords) {
  return new goog.events.BrowserEvent({
    type: type,
    target: target,
    changedTouches: [{
      clientX: clientCoords.x,
      clientY: clientCoords.y,
      screenX: screenCoords.x,
      screenY: screenCoords.y
    }]
  });
}

/**
 * @param {string} type
 * @param {number} pointerId
 * @param {string} pointerType
 * @return {!goog.events.BrowserEvent}
 */
function createPointerEvent(type, pointerId, pointerType) {
  return new goog.events.BrowserEvent(
      {type: type, pointerId: pointerId, pointerType: pointerType});
}

/**
 * @param {!goog.events.BrowserEvent} event
 * @param {goog.events.BrowserEvent.MouseButton} button
 * @param {boolean} isActionButton
 * @return {!goog.events.BrowserEvent}
 */
function assertIsButton(event, button, isActionButton) {
  for (var key in Button) {
    assertEquals(
        'Testing isButton(' + key + ') against ' + button + ' and type ' +
            event.type,
        Button[key] == button, event.isButton(Button[key]));
  }

  assertEquals(isActionButton, event.isMouseActionButton());
}
