// Copyright 2008 The Closure Library Authors. All Rights Reserved.
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

goog.provide('goog.ui.DatePickerTest');
goog.setTestOnly('goog.ui.DatePickerTest');

goog.require('goog.a11y.aria');
goog.require('goog.a11y.aria.Role');
goog.require('goog.date.Date');
goog.require('goog.date.DateRange');
goog.require('goog.dom');
goog.require('goog.dom.TagName');
goog.require('goog.dom.classlist');
goog.require('goog.events');
goog.require('goog.events.KeyCodes');
goog.require('goog.i18n.DateTimeSymbols');
goog.require('goog.i18n.DateTimeSymbols_en_US');
goog.require('goog.i18n.DateTimeSymbols_zh_HK');
goog.require('goog.style');
goog.require('goog.testing.events');
goog.require('goog.testing.jsunit');
goog.require('goog.testing.recordFunction');
goog.require('goog.ui.DatePicker');

var picker;
var $$ = goog.dom.getElementsByTagNameAndClass;
var sandbox;

function setUpPage() {
  sandbox = goog.dom.getElement('sandbox');

  // Set the current date to a constant.
  goog.now = function() {
    return +new Date(2017, 9, 17);
  };
}

function tearDown() {
  picker.dispose();
  goog.dom.removeChildren(sandbox);
}

function testIsMonthOnLeft() {
  goog.i18n.DateTimeSymbols = goog.i18n.DateTimeSymbols_en_US;
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  var head = $$('tr', 'goog-date-picker-head')[0];
  var month = $$('button', 'goog-date-picker-month', head.firstChild)[0];
  assertSameElements(
      'Button element must have expected class names',
      ['goog-date-picker-btn', 'goog-date-picker-month'],
      goog.dom.classlist.get(month));
}

function testIsYearOnLeft() {
  goog.i18n.DateTimeSymbols = goog.i18n.DateTimeSymbols_zh_HK;
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  var head = $$('tr', 'goog-date-picker-head')[0];
  var year = $$('button', 'goog-date-picker-year', head.firstChild)[0];
  assertSameElements(
      'Button element must have expected class names',
      ['goog-date-picker-btn', 'goog-date-picker-year'],
      goog.dom.classlist.get(year));
}

function testHidingOfTableFoot0() {
  picker = new goog.ui.DatePicker();
  picker.setAllowNone(false);
  picker.setShowToday(false);
  picker.create(sandbox);
  var tFoot = $$('tfoot')[0];
  assertFalse(goog.style.isElementShown(tFoot));
}

function testHidingOfTableFoot1() {
  picker = new goog.ui.DatePicker();
  picker.setAllowNone(false);
  picker.setShowToday(true);
  picker.create(sandbox);
  var tFoot = $$('tfoot')[0];
  assertTrue(goog.style.isElementShown(tFoot));
}

function testHidingOfTableFoot2() {
  picker = new goog.ui.DatePicker();
  picker.setAllowNone(true);
  picker.setShowToday(false);
  picker.create(sandbox);
  var tFoot = $$('tfoot')[0];
  assertTrue(goog.style.isElementShown(tFoot));
}

function testHidingOfTableFoot3() {
  picker = new goog.ui.DatePicker();
  picker.setAllowNone(true);
  picker.setShowToday(true);
  picker.create(sandbox);
  var tFoot = $$('tfoot')[0];
  assertTrue(goog.style.isElementShown(tFoot));
}

function testHidingOfTableFootAfterCreate0() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setAllowNone(false);
  picker.setShowToday(false);
  var tFoot = $$('tfoot')[0];
  assertFalse(goog.style.isElementShown(tFoot));
}

function testHidingOfTableFootAfterCreate1() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setAllowNone(false);
  picker.setShowToday(true);
  var tFoot = $$('tfoot')[0];
  assertTrue(goog.style.isElementShown(tFoot));
}

function testHidingOfTableFootAfterCreate2() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setAllowNone(true);
  picker.setShowToday(false);
  var tFoot = $$('tfoot')[0];
  assertTrue(goog.style.isElementShown(tFoot));
}

function testHidingOfTableFootAfterCreate3() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setAllowNone(true);
  picker.setShowToday(true);
  var tFoot = $$('tfoot')[0];
  assertTrue(goog.style.isElementShown(tFoot));
}

function testLongDateFormat() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setLongDateFormat(true);
  var dates = $$('td', 'goog-date-picker-date');
  for (var i = 0; i < dates.length; i++) {
    assertEquals(2, goog.dom.getTextContent(dates[i]).length);
  }
}

function testGetActiveMonth() {
  picker = new goog.ui.DatePicker(new Date(2000, 5, 5));
  var month = picker.getActiveMonth();
  assertObjectEquals(new goog.date.Date(2000, 5, 1), month);

  month.setMonth(10);
  assertObjectEquals(
      'modifying the returned object is safe', new goog.date.Date(2000, 5, 1),
      picker.getActiveMonth());
}

function testGetActiveMonthBeforeYear100() {
  var date = new Date(23, 5, 5);
  // Above statement will create date with year 1923, need to set full year
  // explicitly.
  date.setFullYear(23);

  var expectedMonth = new goog.date.Date(23, 5, 1);
  expectedMonth.setFullYear(23);

  picker = new goog.ui.DatePicker(date);
  var month = picker.getActiveMonth();
  assertObjectEquals(expectedMonth, month);

  month.setMonth(10);
  assertObjectEquals(
      'modifying the returned object is safe', expectedMonth,
      picker.getActiveMonth());
}

function testGetDate() {
  picker = new goog.ui.DatePicker(new Date(2000, 0, 1));
  var date = picker.getDate();
  assertObjectEquals(new goog.date.Date(2000, 0, 1), date);

  date.setMonth(1);
  assertObjectEquals(
      'modifying the returned date is safe', new goog.date.Date(2000, 0, 1),
      picker.getDate());

  picker.setDate(null);
  assertNull('no date is selected', picker.getDate());
}

function testGetDateBeforeYear100() {
  var inputDate = new Date(23, 5, 5);
  // Above statement will create date with year 1923, need to set full year
  // explicitly.
  inputDate.setFullYear(23);
  picker = new goog.ui.DatePicker(inputDate);
  var date = picker.getDate();

  var expectedDate = new goog.date.Date(23, 5, 5);
  expectedDate.setFullYear(23);
  assertObjectEquals(expectedDate, date);

  picker.setDate(inputDate);
  assertObjectEquals(expectedDate, picker.getDate());
  var expectedMonth = new goog.date.Date(23, 5, 1);
  expectedMonth.setFullYear(23);
  assertObjectEquals(expectedMonth, picker.getActiveMonth());
}

function testGridForDecember23() {
  // Initialize picker to December 23.
  var inputDate = new Date(23, 11, 5);
  // Above statement will create date with year 1923, need to set full year
  // explicitly.
  inputDate.setFullYear(23);
  picker = new goog.ui.DatePicker(inputDate);
  picker.create(sandbox);

  // Grid start with last days of November 23, shows December 23 and first days
  // of January 24.
  for (var i = 0; i < 6; i++) {
    for (var j = 0; j < 7; j++) {
      var date = picker.getDateAt(i, j);
      if (date.getMonth() == 0) {
        assertEquals(24, date.getFullYear());
      } else {
        assertEquals(23, date.getFullYear());
      }
    }
  }
}

function testGridForJanuary22() {
  // Initialize picker to January 22.
  var inputDate = new Date(22, 0, 5);
  // Above statement will create date with year 1922, need to set full year
  // explicitly.
  inputDate.setFullYear(22);
  picker = new goog.ui.DatePicker(inputDate);
  picker.create(sandbox);

  // Grid start with last days of December 21, shows January 22 and first days
  // of February 22.
  for (var i = 0; i < 6; i++) {
    for (var j = 0; j < 7; j++) {
      var date = picker.getDateAt(i, j);
      if (date.getMonth() == 11) {
        assertEquals(21, date.getFullYear());
      } else {
        assertEquals(22, date.getFullYear());
      }
    }
  }
}

function testGetDateAt() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setDate(new Date(2000, 5, 5));
  var date = picker.getDateAt(0, 0);
  assertTrue(date.equals(picker.grid_[0][0]));

  date.setMonth(1);
  assertFalse(date.equals(picker.grid_[0][0]));
}

function testGetDateAt_NotInGrid() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setDate(new Date(2000, 5, 5));
  var date = picker.getDateAt(-1, 0);
  assertNull(date);

  date = picker.getDateAt(0, -1);
  assertNull(date);
}

function testGetDateElementAt() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setDate(new Date(2000, 5, 5));
  var element = picker.getDateElementAt(0, 0);
  assertEquals('td', element.tagName.toLowerCase());
  assertObjectEquals(element, picker.elTable_[1][1]);
}

function testGetDateElementAt_NotInTable() {
  picker = new goog.ui.DatePicker();
  picker.create(sandbox);
  picker.setDate(new Date(2000, 5, 5));
  var element = picker.getDateElementAt(-1, 0);
  assertNull(element);

  element = picker.getDateElementAt(0, -1);
  assertNull(element);

  element = picker.getDateElementAt(picker.elTable_.length - 1, 0);
  assertNull(element);

  element = picker.getDateElementAt(0, picker.elTable_[0].length - 1);
  assertNull(element);
}

function testSetDate() {
  picker = new goog.ui.DatePicker();
  picker.createDom();
  picker.enterDocument();
  var selectEvents = 0;
  var changeEvents = 0;
  var changeActiveMonthEvents = 0;
  goog.events.listen(
      picker, goog.ui.DatePicker.Events.SELECT, function() { selectEvents++; });
  goog.events.listen(
      picker, goog.ui.DatePicker.Events.CHANGE, function() { changeEvents++; });
  goog.events.listen(
      picker, goog.ui.DatePicker.Events.CHANGE_ACTIVE_MONTH,
      function() { changeActiveMonthEvents++; });

  // Set date.
  picker.setDate(new Date(2010, 1, 26));
  assertEquals('no select event dispatched', 1, selectEvents);
  assertEquals('no change event dispatched', 1, changeEvents);
  assertEquals(
      'no change active month event dispatched', 1, changeActiveMonthEvents);
  assertTrue(
      'date is set', new goog.date.Date(2010, 1, 26).equals(picker.getDate()));

  // Set date to same date.
  picker.setDate(new Date(2010, 1, 26));
  assertEquals('1 select event dispatched', 2, selectEvents);
  assertEquals('no change event dispatched', 1, changeEvents);
  assertEquals(
      'no change active month event dispatched', 1, changeActiveMonthEvents);

  // Set date to different date.
  picker.setDate(new Date(2010, 1, 27));
  assertEquals('another select event dispatched', 3, selectEvents);
  assertEquals('1 change event dispatched', 2, changeEvents);
  assertEquals(
      '2 change active month events dispatched', 1, changeActiveMonthEvents);

  // Set date to a date in a different month.
  picker.setDate(new Date(2010, 2, 27));
  assertEquals('another select event dispatched', 4, selectEvents);
  assertEquals('another change event dispatched', 3, changeEvents);
  assertEquals(
      '3 change active month event dispatched', 2, changeActiveMonthEvents);

  // Set date to none.
  picker.setDate(null);
  assertEquals('another select event dispatched', 5, selectEvents);
  assertEquals('another change event dispatched', 4, changeEvents);
  assertNull('date cleared', picker.getDate());
}

function testChangeActiveMonth() {
  picker = new goog.ui.DatePicker();
  var changeActiveMonthEvents = 0;
  goog.events.listen(
      picker, goog.ui.DatePicker.Events.CHANGE_ACTIVE_MONTH,
      function() { changeActiveMonthEvents++; });

  // Set date.
  picker.setDate(new Date(2010, 1, 26));
  assertEquals(
      'change active month events dispatched', 1, changeActiveMonthEvents);
  assertTrue(
      'date is set', new goog.date.Date(2010, 1, 26).equals(picker.getDate()));

  // Change to next month.
  picker.nextMonth();
  assertEquals(
      '1 change active month event dispatched', 2, changeActiveMonthEvents);
  assertTrue(
      'date should still be the same',
      new goog.date.Date(2010, 1, 26).equals(picker.getDate()));

  // Change to next year.
  picker.nextYear();
  assertEquals(
      '2 change active month events dispatched', 3, changeActiveMonthEvents);

  // Change to previous month.
  picker.previousMonth();
  assertEquals(
      '3 change active month events dispatched', 4, changeActiveMonthEvents);

  // Change to previous year.
  picker.previousYear();
  assertEquals(
      '4 change active month events dispatched', 5, changeActiveMonthEvents);
}

function testChangeActiveMonth_whenGridGrows_dispatchesGridIncreaseEvent() {
  picker = new goog.ui.DatePicker();
  picker.setShowFixedNumWeeks(false);
  picker.render();
  var gridSizeIncreaseEvents = 0;
  goog.events.listen(
      picker, goog.ui.DatePicker.Events.GRID_SIZE_INCREASE,
      () => void gridSizeIncreaseEvents++);

  // Feb 2015 has only four rows of dates.
  picker.setDate(new Date(2015, 1, 1));
  assertEquals('No grid size changes yet', 0, gridSizeIncreaseEvents);

  // Mar 2015 has 5 rows.
  picker.nextMonth();
  assertEquals(
      '1 grid size increase events dispatched', 1, gridSizeIncreaseEvents);

  // Going back to Feb is a size decrease, so no dispatch.
  picker.previousMonth();
  assertEquals(
      '1 grid size increase events dispatched', 1, gridSizeIncreaseEvents);

  // Going forward to Mar again should dispatch again.
  picker.nextMonth();
  assertEquals(
      '2 grid size increase events dispatched', 2, gridSizeIncreaseEvents);

  // Apr 2015 has 5 rows also.
  picker.nextMonth();
  assertEquals(
      '2 grid size increase events dispatched', 2, gridSizeIncreaseEvents);

  // May 2015 has 6 rows.
  picker.nextMonth();
  assertEquals(
      '3 grid size increase events dispatched', 3, gridSizeIncreaseEvents);
}

function
testChangeActiveMonth_withFixedNumWeeks_dispatchesNoGridIncreaseEvent() {
  picker = new goog.ui.DatePicker();
  picker.setShowFixedNumWeeks(true);
  picker.render();
  var gridSizeIncreaseEvents = 0;
  goog.events.listen(
      picker, goog.ui.DatePicker.Events.GRID_SIZE_INCREASE,
      () => void gridSizeIncreaseEvents++);

  // Feb 2015 has only four rows of dates.
  picker.setDate(new Date(2015, 1, 1));
  assertEquals('No grid size changes yet', 0, gridSizeIncreaseEvents);

  for (var i = 0; i < 100; i++) {
    picker.nextMonth();
  }
  assertEquals('No grid size changes', 0, gridSizeIncreaseEvents);
}

function testUserSelectableDates() {
  var dateRange = new goog.date.DateRange(
      new goog.date.Date(2010, 1, 25), new goog.date.Date(2010, 1, 27));
  picker = new goog.ui.DatePicker();
  picker.setUserSelectableDateRange(dateRange);
  assertFalse(
      'should not be selectable date',
      picker.isUserSelectableDate_(new goog.date.Date(2010, 1, 24)));
  assertTrue(
      'should be a selectable date',
      picker.isUserSelectableDate_(new goog.date.Date(2010, 1, 25)));
  assertTrue(
      'should be a selectable date',
      picker.isUserSelectableDate_(new goog.date.Date(2010, 1, 26)));
  assertTrue(
      'should be a selectable date',
      picker.isUserSelectableDate_(new goog.date.Date(2010, 1, 27)));
  assertFalse(
      'should not be selectable date',
      picker.isUserSelectableDate_(new goog.date.Date(2010, 1, 28)));
}

function testGetUserSelectableDateRange() {
  picker = new goog.ui.DatePicker();
  var dateRange = picker.getUserSelectableDateRange();
  assertTrue(
      'default date range is all time',
      goog.date.DateRange.equals(dateRange, goog.date.DateRange.allTime()));
  var newDateRange = new goog.date.DateRange(
      new goog.date.Date(2010, 1, 25), new goog.date.Date(2010, 1, 27));
  picker.setUserSelectableDateRange(newDateRange);
  dateRange = picker.getUserSelectableDateRange();
  assertTrue(
      'should be equal to updated date range',
      goog.date.DateRange.equals(dateRange, newDateRange));
}

function testUniqueCellIds() {
  picker = new goog.ui.DatePicker();
  picker.render();
  var cells = goog.dom.getElementsByTagNameAndClass(
      goog.dom.TagName.TD, undefined, picker.getElement());
  var existingIds = {};
  var numCells = cells.length;
  for (var i = 0; i < numCells; i++) {
    assertNotNull(cells[i]);
    if (goog.a11y.aria.getRole(cells[i]) == goog.a11y.aria.Role.GRIDCELL) {
      assertNonEmptyString('cell id is non empty', cells[i].id);
      assertUndefined('cell id is not unique', existingIds[cells[i].id]);
      existingIds[cells[i].id] = 1;
    }
  }
}

function testDecoratePreservesClasses() {
  picker = new goog.ui.DatePicker();
  var div = goog.dom.createDom(goog.dom.TagName.DIV, 'existing-class');
  picker.decorate(div);
  assertTrue(goog.dom.classlist.contains(div, picker.getBaseCssClass()));
  assertTrue(goog.dom.classlist.contains(div, 'existing-class'));
}


function testKeyboardNavigation_arrowKey() {
  // This is a Sunday, so it's the first cell in the grid.
  picker = new goog.ui.DatePicker(new Date(2017, 9, 1));
  // Make the first column be Sunday, not week numbers
  picker.setShowWeekNum(false);
  picker.render(goog.dom.getElement('sandbox'));
  var selectEvents = goog.testing.recordFunction();
  var changeEvents = goog.testing.recordFunction();
  goog.events.listen(picker, goog.ui.DatePicker.Events.SELECT, selectEvents);
  goog.events.listen(picker, goog.ui.DatePicker.Events.CHANGE, changeEvents);

  goog.testing.events.fireNonAsciiKeySequence(
      picker.getElement(), goog.events.KeyCodes.DOWN,
      goog.events.KeyCodes.DOWN);
  changeEvents.assertCallCount(1);
  selectEvents.assertCallCount(0);

  // Make sure the new selection is focused, for a11y.  elTable_[0] has the week
  // day headers, so +1 starts at the first row of days.
  assertEquals(picker.elTable_[1 + 1][1], document.activeElement);

  goog.testing.events.fireNonAsciiKeySequence(
      picker.getElement(), goog.events.KeyCodes.ENTER,
      goog.events.KeyCodes.ENTER);
  changeEvents.assertCallCount(1);
  selectEvents.assertCallCount(1);
}

function testKeyboardNavigation_homeKey() {
  // This is a Sunday, so it's the first cell in the grid.
  picker = new goog.ui.DatePicker(new Date(2017, 9, 1));
  // Make the first column be Sunday, not week numbers
  picker.setShowWeekNum(false);
  picker.render(goog.dom.getElement('sandbox'));
  var selectEvents = goog.testing.recordFunction();
  var changeEvents = goog.testing.recordFunction();
  goog.events.listen(picker, goog.ui.DatePicker.Events.SELECT, selectEvents);
  goog.events.listen(picker, goog.ui.DatePicker.Events.CHANGE, changeEvents);

  goog.testing.events.fireNonAsciiKeySequence(
      picker.getElement(), goog.events.KeyCodes.HOME,
      goog.events.KeyCodes.HOME);
  changeEvents.assertCallCount(1);
  selectEvents.assertCallCount(1);

  // Make sure the new selection is focused, for a11y.  elTable_[0] has the week
  // day headers, so +1 starts at the first row of days.
  assertEquals(picker.elTable_[2 + 1][3], document.activeElement);
}

function testDayGridHasNonEmptyAriaLabels() {
  picker = new goog.ui.DatePicker(new Date(2017, 8, 9));
  picker.render(goog.dom.getElement('sandbox'));

  var cells = goog.dom.getElementsByTagNameAndClass(
      goog.dom.TagName.TD, undefined, picker.getElement());
  var numCells = cells.length;
  for (var i = 0; i < numCells; i++) {
    assertNotNull(cells[i]);
    if (goog.a11y.aria.getRole(cells[i]) == goog.a11y.aria.Role.GRIDCELL) {
      assertNonEmptyString(
          'Aria label in date cell should not be empty',
          goog.a11y.aria.getLabel(cells[i]));
    }
  }
}
