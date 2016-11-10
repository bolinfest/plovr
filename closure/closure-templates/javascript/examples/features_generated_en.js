// This file was automatically generated from features.soy.
// Please don't edit this file by hand.

if (typeof soy == 'undefined') { var soy = {}; }
if (typeof soy.examples == 'undefined') { soy.examples = {}; }
if (typeof soy.examples.features == 'undefined') { soy.examples.features = {}; }


soy.examples.features.demoComments = function(opt_data, opt_ignored) {
  return 'blah blah<br>http://www.google.com<br>';
};


soy.examples.features.demoLineJoining = function(opt_data, opt_ignored) {
  return 'First second.<br><i>First</i>second.<br>Firstsecond.<br><i>First</i> second.<br>Firstsecond.<br>';
};


soy.examples.features.demoRawTextCommands = function(opt_data, opt_ignored) {
  return '<pre>Space       : AA BB<br>Empty string: AABB<br>New line    : AA\nBB<br>Carriage ret: AA\rBB<br>Tab         : AA\tBB<br>Left brace  : AA{BB<br>Right brace : AA}BB<br>Literal     : AA\tBB { CC\n  DD } EE {sp}{\\n}{rb} FF</pre>';
};


soy.examples.features.demoPrint = function(opt_data, opt_ignored) {
  return 'Boo!<br>Boo!<br>3<br>' + soy.$$escapeHtml(opt_data.boo) + '<br>' + soy.$$escapeHtml(1 + opt_data.two) + '<br>26, true.<br>';
};


soy.examples.features.demoPrintDirectives = function(opt_data, opt_ignored) {
  return 'insertWordBreaks:<br><div style="width:150px; border:1px solid #00CC00">' + soy.$$escapeHtml(opt_data.longVarName) + '<br>' + soy.$$insertWordBreaks(soy.$$escapeHtml(opt_data.longVarName), 5) + '<br></div>id:<br><span id="' + opt_data.elementId + '" class="' + opt_data.cssClass + '" style="border:1px solid #000000">Hello</span>';
};


soy.examples.features.demoAutoescapeTrue = function(opt_data, opt_ignored) {
  return soy.$$escapeHtml(opt_data.italicHtml) + '<br>' + opt_data.italicHtml + '<br>';
};


soy.examples.features.demoAutoescapeFalse = function(opt_data, opt_ignored) {
  return opt_data.italicHtml + '<br>' + soy.$$escapeHtml(opt_data.italicHtml) + '<br>';
};


soy.examples.features.demoMsg = function(opt_data, opt_ignored) {
  return 'Hello ' + soy.$$escapeHtml(opt_data.name) + '!<br>Click <a href="' + soy.$$escapeHtml(opt_data.labsUrl) + '">here</a> to access Labs.<br>Archive<br>Archive<br>';
};


soy.examples.features.demoIf = function(opt_data, opt_ignored) {
  return ((Math.round(opt_data.pi * 100) / 100 == 3.14) ? soy.$$escapeHtml(opt_data.pi) + ' is a good approximation of pi.' : (Math.round(opt_data.pi) == 3) ? soy.$$escapeHtml(opt_data.pi) + ' is a bad approximation of pi.' : soy.$$escapeHtml(opt_data.pi) + ' is nowhere near the value of pi.') + '<br>';
};


soy.examples.features.demoSwitch = function(opt_data, opt_ignored) {
  var output = 'Dear ' + soy.$$escapeHtml(opt_data.name) + ', &nbsp;';
  switch (opt_data.name) {
    case 'Go':
      output += 'You\'ve been bad this year.';
      break;
    case 'Fay':
    case 'Ivy':
      output += 'You\'ve been good this year.';
      break;
    default:
      output += 'You don\'t really believe in me, do you?';
  }
  output += '&nbsp; --Santa<br>';
  return output;
};


soy.examples.features.demoForeach = function(opt_data, opt_ignored) {
  var output = '';
  var personList137 = opt_data.persons;
  var personListLen137 = personList137.length;
  if (personListLen137 > 0) {
    for (var personIndex137 = 0; personIndex137 < personListLen137; personIndex137++) {
      var personData137 = personList137[personIndex137];
      output += ((personIndex137 == 0) ? 'First,' : (personIndex137 == personListLen137 - 1) ? 'Finally,' : 'Then') + ' ' + ((personData137.numWaffles == 1) ? soy.$$escapeHtml(personData137.name) + ' ate 1 waffle.' : soy.$$escapeHtml(personData137.name) + ' ate ' + soy.$$escapeHtml(personData137.numWaffles) + ' waffles.') + '<br>';
    }
  } else {
    output += 'Nobody here ate any waffles.<br>';
  }
  return output;
};


soy.examples.features.demoFor = function(opt_data, opt_ignored) {
  var output = '';
  var iLimit160 = opt_data.numLines;
  for (var i160 = 0; i160 < iLimit160; i160++) {
    output += 'Line ' + soy.$$escapeHtml(i160 + 1) + ' of ' + soy.$$escapeHtml(opt_data.numLines) + '.<br>';
  }
  for (var i166 = 2; i166 < 10; i166 += 2) {
    output += soy.$$escapeHtml(i166) + '... ';
  }
  output += 'Who do we appreciate?<br>';
  return output;
};


soy.examples.features.demoCallWithoutParam = function(opt_data, opt_ignored) {
  return 'Hello world!<br>' + soy.examples.features.tripReport_(null) + '<br>' + soy.examples.features.tripReport_(opt_data) + '<br>' + soy.examples.features.tripReport_(opt_data.tripInfo) + '<br>';
};


soy.examples.features.demoCallWithParam = function(opt_data, opt_ignored) {
  var output = '';
  var destinationList180 = opt_data.destinations;
  var destinationListLen180 = destinationList180.length;
  for (var destinationIndex180 = 0; destinationIndex180 < destinationListLen180; destinationIndex180++) {
    var destinationData180 = destinationList180[destinationIndex180];
    output += soy.examples.features.tripReport_(soy.$$augmentData(opt_data, {destination: destinationData180})) + '<br>' + ((destinationIndex180 % 2 == 0) ? soy.examples.features.tripReport_({name: opt_data.companionName, destination: destinationData180}) + '<br>' : '');
  }
  return output;
};


soy.examples.features.demoCallWithParamBlock = function(opt_data, opt_ignored) {
  var param193 = '';
  switch (Math.floor(Math.random() * 3)) {
    case 0:
      param193 += 'Boston';
      break;
    case 1:
      param193 += 'Singapore';
      break;
    case 2:
      param193 += 'Zurich';
      break;
  }
  var output = soy.examples.features.tripReport_({name: opt_data.name, destination: param193});
  output += '<br>';
  return output;
};


soy.examples.features.tripReport_ = function(opt_data, opt_ignored) {
  opt_data = opt_data || {};
  return (! opt_data.name) ? 'A trip was taken.' : (! opt_data.destination) ? soy.$$escapeHtml(opt_data.name) + ' took a trip.' : soy.$$escapeHtml(opt_data.name) + ' took a trip to ' + soy.$$escapeHtml(opt_data.destination) + '.';
};


soy.examples.features.demoExpressions = function(opt_data, opt_ignored) {
  var output = 'First student\'s major: ' + soy.$$escapeHtml(opt_data.students[0].major) + '<br>Last student\'s year: ' + soy.$$escapeHtml(opt_data.students[opt_data.students.length - 1].year) + '<br>Random student\'s major: ' + soy.$$escapeHtml(opt_data.students[Math.floor(Math.random() * opt_data.students.length)].major) + '<br>';
  var studentList229 = opt_data.students;
  var studentListLen229 = studentList229.length;
  for (var studentIndex229 = 0; studentIndex229 < studentListLen229; studentIndex229++) {
    var studentData229 = studentList229[studentIndex229];
    output += soy.$$escapeHtml(studentData229.name) + ':' + ((studentIndex229 == 0) ? ' First.' : (studentIndex229 == studentListLen229 - 1) ? ' Last.' : (studentIndex229 == Math.ceil(opt_data.students.length / 2) - 1) ? ' Middle.' : '') + ((studentIndex229 % 2 == 1) ? ' Even.' : '') + ' ' + soy.$$escapeHtml(studentData229.major) + '.' + ((studentData229.major == 'Physics' || studentData229.major == 'Biology') ? ' Scientist.' : '') + ((opt_data.currentYear - studentData229.year < 10) ? ' Young.' : '') + ' ' + soy.$$escapeHtml(studentData229.year < 2000 ? Math.round((studentData229.year - 1905) / 10) * 10 + 's' : '00s') + '. ' + ((studentData229.year < 2000) ? soy.$$escapeHtml(Math.round((studentData229.year - 1905) / 10) * 10) : '00') + 's.<br>';
  }
  return output;
};


soy.examples.features.demoDoubleBraces = function(opt_data, opt_ignored) {
  return 'The set of ' + soy.$$escapeHtml(opt_data.setName) + ' is {' + soy.examples.features.buildCommaSeparatedList_({items: opt_data.setMembers}) + ', ...}.';
};


soy.examples.features.buildCommaSeparatedList_ = function(opt_data, opt_ignored) {
  var output = '';
  var itemList272 = opt_data.items;
  var itemListLen272 = itemList272.length;
  for (var itemIndex272 = 0; itemIndex272 < itemListLen272; itemIndex272++) {
    var itemData272 = itemList272[itemIndex272];
    output += ((! (itemIndex272 == 0)) ? ', ' : '') + soy.$$escapeHtml(itemData272);
  }
  return output;
};


soy.examples.features.demoBidiSupport = function(opt_data, opt_ignored) {
  var output = '<div id="title1" style="font-variant:small-caps" ' + soy.$$escapeHtml(soy.$$bidiDirAttr(1, opt_data.title)) + '>' + soy.$$escapeHtml(opt_data.title) + '</div><div id="title2" style="font-variant:small-caps">' + soy.$$bidiSpanWrap(1, soy.$$escapeHtml(opt_data.title)) + '</div>by ' + soy.$$bidiSpanWrap(1, soy.$$escapeHtml(opt_data.author)) + ' (' + soy.$$escapeHtml(opt_data.year) + ')<div id="choose_a_keyword">Your favorite keyword: <select>';
  var keywordList300 = opt_data.keywords;
  var keywordListLen300 = keywordList300.length;
  for (var keywordIndex300 = 0; keywordIndex300 < keywordListLen300; keywordIndex300++) {
    var keywordData300 = keywordList300[keywordIndex300];
    output += '<option value="' + soy.$$escapeHtml(keywordData300) + '">' + soy.$$bidiUnicodeWrap(1, soy.$$escapeHtml(keywordData300)) + '</option>';
  }
  output += '</select></div><a href="#" style="float:right">Help</a><br>';
  return output;
};


soy.examples.features.bidiGlobalDir = function(opt_data, opt_ignored) {
  return soy.$$escapeHtml(1);
};


soy.examples.features.exampleHeader = function(opt_data, opt_ignored) {
  return '<hr><b>' + soy.$$escapeHtml(opt_data.exampleNum) + '. ' + soy.$$escapeHtml(opt_data.exampleName) + '</b><br>';
};


soy.examples.features.demoParamWithKindAttribute = function(opt_data, opt_ignored) {
  var output = '<div>';
  var param328 = '';
  var iList329 = opt_data.list;
  var iListLen329 = iList329.length;
  for (var iIndex329 = 0; iIndex329 < iListLen329; iIndex329++) {
    var iData329 = iList329[iIndex329];
    param328 += '<li>' + soy.$$escapeHtml(iData329) + '</li>';
  }
  output += soy.examples.features.demoParamWithKindAttributeCallee_({message: soydata.VERY_UNSAFE.ordainSanitizedHtml('<b>' + soy.$$escapeHtml(opt_data.message) + '</b>'), listItems: soydata.VERY_UNSAFE.ordainSanitizedHtml(param328)});
  output += '</div>';
  return output;
};


soy.examples.features.demoParamWithKindAttributeCallee_ = function(opt_data, opt_ignored) {
  return '<div>' + soy.$$escapeHtml(opt_data.message) + '</div><ol>' + soy.$$escapeHtml(opt_data.listItems) + '</ol>';
};
