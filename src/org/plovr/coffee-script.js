/**
 * CoffeeScript Compiler v1.1.1
 * http://coffeescript.org
 *
 * Copyright 2011, Jeremy Ashkenas
 * Released under the MIT License
 */
this.CoffeeScript = function() {
  function require(path){ return require[path]; }
  require['./helpers'] = new function() {
  var exports = this;
  (function() {
  var extend, flatten;
  exports.starts = function(string, literal, start) {
    return literal === string.substr(start, literal.length);
  };
  exports.ends = function(string, literal, back) {
    var len;
    len = literal.length;
    return literal === string.substr(string.length - len - (back || 0), len);
  };
  exports.compact = function(array) {
    var item, _i, _len, _results;
    _results = [];
    for (_i = 0, _len = array.length; _i < _len; _i++) {
      item = array[_i];
      if (item) {
        _results.push(item);
      }
    }
    return _results;
  };
  exports.count = function(string, substr) {
    var num, pos;
    num = pos = 0;
    if (!substr.length) {
      return 1 / 0;
    }
    while (pos = 1 + string.indexOf(substr, pos)) {
      num++;
    }
    return num;
  };
  exports.merge = function(options, overrides) {
    return extend(extend({}, options), overrides);
  };
  extend = exports.extend = function(object, properties) {
    var key, val;
    for (key in properties) {
      val = properties[key];
      object[key] = val;
    }
    return object;
  };
  exports.flatten = flatten = function(array) {
    var element, flattened, _i, _len;
    flattened = [];
    for (_i = 0, _len = array.length; _i < _len; _i++) {
      element = array[_i];
      if (element instanceof Array) {
        flattened = flattened.concat(flatten(element));
      } else {
        flattened.push(element);
      }
    }
    return flattened;
  };
  exports.del = function(obj, key) {
    var val;
    val = obj[key];
    delete obj[key];
    return val;
  };
  exports.last = function(array, back) {
    return array[array.length - (back || 0) - 1];
  };
}).call(this);

};require['./rewriter'] = new function() {
  var exports = this;
  (function() {
  var BALANCED_PAIRS, EXPRESSION_CLOSE, EXPRESSION_END, EXPRESSION_START, IMPLICIT_BLOCK, IMPLICIT_CALL, IMPLICIT_END, IMPLICIT_FUNC, IMPLICIT_UNSPACED_CALL, INVERSES, LINEBREAKS, SINGLE_CLOSERS, SINGLE_LINERS, left, rite, _i, _len, _ref;
  var __indexOf = Array.prototype.indexOf || function(item) {
    for (var i = 0, l = this.length; i < l; i++) {
      if (this[i] === item) return i;
    }
    return -1;
  }, __slice = Array.prototype.slice;
  exports.Rewriter = (function() {
    function Rewriter() {}
    Rewriter.prototype.rewrite = function(tokens) {
      this.tokens = tokens;
      this.removeLeadingNewlines();
      this.removeMidExpressionNewlines();
      this.closeOpenCalls();
      this.closeOpenIndexes();
      this.addImplicitIndentation();
      this.tagPostfixConditionals();
      this.addImplicitBraces();
      this.addImplicitParentheses();
      this.ensureBalance(BALANCED_PAIRS);
      this.rewriteClosingParens();
      return this.tokens;
    };
    Rewriter.prototype.scanTokens = function(block) {
      var i, token, tokens;
      tokens = this.tokens;
      i = 0;
      while (token = tokens[i]) {
        i += block.call(this, token, i, tokens);
      }
      return true;
    };
    Rewriter.prototype.detectEnd = function(i, condition, action) {
      var levels, token, tokens, _ref, _ref2;
      tokens = this.tokens;
      levels = 0;
      while (token = tokens[i]) {
        if (levels === 0 && condition.call(this, token, i)) {
          return action.call(this, token, i);
        }
        if (!token || levels < 0) {
          return action.call(this, token, i - 1);
        }
        if (_ref = token[0], __indexOf.call(EXPRESSION_START, _ref) >= 0) {
          levels += 1;
        } else if (_ref2 = token[0], __indexOf.call(EXPRESSION_END, _ref2) >= 0) {
          levels -= 1;
        }
        i += 1;
      }
      return i - 1;
    };
    Rewriter.prototype.removeLeadingNewlines = function() {
      var i, tag, _len, _ref;
      _ref = this.tokens;
      for (i = 0, _len = _ref.length; i < _len; i++) {
        tag = _ref[i][0];
        if (tag !== 'TERMINATOR') {
          break;
        }
      }
      if (i) {
        return this.tokens.splice(0, i);
      }
    };
    Rewriter.prototype.removeMidExpressionNewlines = function() {
      return this.scanTokens(function(token, i, tokens) {
        var _ref;
        if (!(token[0] === 'TERMINATOR' && (_ref = this.tag(i + 1), __indexOf.call(EXPRESSION_CLOSE, _ref) >= 0))) {
          return 1;
        }
        tokens.splice(i, 1);
        return 0;
      });
    };
    Rewriter.prototype.closeOpenCalls = function() {
      var action, condition;
      condition = function(token, i) {
        var _ref;
        return ((_ref = token[0]) === ')' || _ref === 'CALL_END') || token[0] === 'OUTDENT' && this.tag(i - 1) === ')';
      };
      action = function(token, i) {
        return this.tokens[token[0] === 'OUTDENT' ? i - 1 : i][0] = 'CALL_END';
      };
      return this.scanTokens(function(token, i) {
        if (token[0] === 'CALL_START') {
          this.detectEnd(i + 1, condition, action);
        }
        return 1;
      });
    };
    Rewriter.prototype.closeOpenIndexes = function() {
      var action, condition;
      condition = function(token, i) {
        var _ref;
        return (_ref = token[0]) === ']' || _ref === 'INDEX_END';
      };
      action = function(token, i) {
        return token[0] = 'INDEX_END';
      };
      return this.scanTokens(function(token, i) {
        if (token[0] === 'INDEX_START') {
          this.detectEnd(i + 1, condition, action);
        }
        return 1;
      });
    };
    Rewriter.prototype.addImplicitBraces = function() {
      var action, condition, stack, start, startIndent;
      stack = [];
      start = null;
      startIndent = 0;
      condition = function(token, i) {
        var one, tag, three, two, _ref, _ref2;
        _ref = this.tokens.slice(i + 1, (i + 3 + 1) || 9e9), one = _ref[0], two = _ref[1], three = _ref[2];
        if ('HERECOMMENT' === (one != null ? one[0] : void 0)) {
          return false;
        }
        tag = token[0];
        return ((tag === 'TERMINATOR' || tag === 'OUTDENT') && !((two != null ? two[0] : void 0) === ':' || (one != null ? one[0] : void 0) === '@' && (three != null ? three[0] : void 0) === ':')) || (tag === ',' && one && ((_ref2 = one[0]) !== 'IDENTIFIER' && _ref2 !== 'NUMBER' && _ref2 !== 'STRING' && _ref2 !== '@' && _ref2 !== 'TERMINATOR' && _ref2 !== 'OUTDENT'));
      };
      action = function(token, i) {
        var tok;
        tok = ['}', '}', token[2]];
        tok.generated = true;
        return this.tokens.splice(i, 0, tok);
      };
      return this.scanTokens(function(token, i, tokens) {
        var ago, idx, tag, tok, value, _ref, _ref2;
        if (_ref = (tag = token[0]), __indexOf.call(EXPRESSION_START, _ref) >= 0) {
          stack.push([(tag === 'INDENT' && this.tag(i - 1) === '{' ? '{' : tag), i]);
          return 1;
        }
        if (__indexOf.call(EXPRESSION_END, tag) >= 0) {
          start = stack.pop();
          return 1;
        }
        if (!(tag === ':' && ((ago = this.tag(i - 2)) === ':' || ((_ref2 = stack[stack.length - 1]) != null ? _ref2[0] : void 0) !== '{'))) {
          return 1;
        }
        stack.push(['{']);
        idx = ago === '@' ? i - 2 : i - 1;
        while (this.tag(idx - 2) === 'HERECOMMENT') {
          idx -= 2;
        }
        value = new String('{');
        value.generated = true;
        tok = ['{', value, token[2]];
        tok.generated = true;
        tokens.splice(idx, 0, tok);
        this.detectEnd(i + 2, condition, action);
        return 2;
      });
    };
    Rewriter.prototype.addImplicitParentheses = function() {
      var action, noCall;
      noCall = false;
      action = function(token, i) {
        var idx;
        idx = token[0] === 'OUTDENT' ? i + 1 : i;
        return this.tokens.splice(idx, 0, ['CALL_END', ')', token[2]]);
      };
      return this.scanTokens(function(token, i, tokens) {
        var callObject, current, next, prev, seenControl, seenSingle, tag, _ref, _ref2, _ref3;
        tag = token[0];
        if (tag === 'CLASS' || tag === 'IF') {
          noCall = true;
        }
        _ref = tokens.slice(i - 1, (i + 1 + 1) || 9e9), prev = _ref[0], current = _ref[1], next = _ref[2];
        callObject = !noCall && tag === 'INDENT' && next && next.generated && next[0] === '{' && prev && (_ref2 = prev[0], __indexOf.call(IMPLICIT_FUNC, _ref2) >= 0);
        seenSingle = false;
        seenControl = false;
        if (__indexOf.call(LINEBREAKS, tag) >= 0) {
          noCall = false;
        }
        if (prev && !prev.spaced && tag === '?') {
          token.call = true;
        }
        if (token.fromThen) {
          return 1;
        }
        if (!(callObject || (prev != null ? prev.spaced : void 0) && (prev.call || (_ref3 = prev[0], __indexOf.call(IMPLICIT_FUNC, _ref3) >= 0)) && (__indexOf.call(IMPLICIT_CALL, tag) >= 0 || !(token.spaced || token.newLine) && __indexOf.call(IMPLICIT_UNSPACED_CALL, tag) >= 0))) {
          return 1;
        }
        tokens.splice(i, 0, ['CALL_START', '(', token[2]]);
        this.detectEnd(i + 1, function(token, i) {
          var post, _ref4;
          tag = token[0];
          if (!seenSingle && token.fromThen) {
            return true;
          }
          if (tag === 'IF' || tag === 'ELSE' || tag === 'CATCH' || tag === '->' || tag === '=>') {
            seenSingle = true;
          }
          if (tag === 'IF' || tag === 'ELSE' || tag === 'SWITCH' || tag === 'TRY') {
            seenControl = true;
          }
          if ((tag === '.' || tag === '?.' || tag === '::') && this.tag(i - 1) === 'OUTDENT') {
            return true;
          }
          return !token.generated && this.tag(i - 1) !== ',' && (__indexOf.call(IMPLICIT_END, tag) >= 0 || (tag === 'INDENT' && !seenControl)) && (tag !== 'INDENT' || (this.tag(i - 2) !== 'CLASS' && (_ref4 = this.tag(i - 1), __indexOf.call(IMPLICIT_BLOCK, _ref4) < 0) && !((post = this.tokens[i + 1]) && post.generated && post[0] === '{')));
        }, action);
        if (prev[0] === '?') {
          prev[0] = 'FUNC_EXIST';
        }
        return 2;
      });
    };
    Rewriter.prototype.addImplicitIndentation = function() {
      return this.scanTokens(function(token, i, tokens) {
        var action, condition, indent, outdent, starter, tag, _ref, _ref2;
        tag = token[0];
        if (tag === 'TERMINATOR' && this.tag(i + 1) === 'THEN') {
          tokens.splice(i, 1);
          return 0;
        }
        if (tag === 'ELSE' && this.tag(i - 1) !== 'OUTDENT') {
          tokens.splice.apply(tokens, [i, 0].concat(__slice.call(this.indentation(token))));
          return 2;
        }
        if (tag === 'CATCH' && ((_ref = this.tag(i + 2)) === 'OUTDENT' || _ref === 'TERMINATOR' || _ref === 'FINALLY')) {
          tokens.splice.apply(tokens, [i + 2, 0].concat(__slice.call(this.indentation(token))));
          return 4;
        }
        if (__indexOf.call(SINGLE_LINERS, tag) >= 0 && this.tag(i + 1) !== 'INDENT' && !(tag === 'ELSE' && this.tag(i + 1) === 'IF')) {
          starter = tag;
          _ref2 = this.indentation(token), indent = _ref2[0], outdent = _ref2[1];
          if (starter === 'THEN') {
            indent.fromThen = true;
          }
          indent.generated = outdent.generated = true;
          tokens.splice(i + 1, 0, indent);
          condition = function(token, i) {
            var _ref3;
            return token[1] !== ';' && (_ref3 = token[0], __indexOf.call(SINGLE_CLOSERS, _ref3) >= 0) && !(token[0] === 'ELSE' && (starter !== 'IF' && starter !== 'THEN'));
          };
          action = function(token, i) {
            return this.tokens.splice((this.tag(i - 1) === ',' ? i - 1 : i), 0, outdent);
          };
          this.detectEnd(i + 2, condition, action);
          if (tag === 'THEN') {
            tokens.splice(i, 1);
          }
          return 1;
        }
        return 1;
      });
    };
    Rewriter.prototype.tagPostfixConditionals = function() {
      var condition;
      condition = function(token, i) {
        var _ref;
        return (_ref = token[0]) === 'TERMINATOR' || _ref === 'INDENT';
      };
      return this.scanTokens(function(token, i) {
        var original;
        if (token[0] !== 'IF') {
          return 1;
        }
        original = token;
        this.detectEnd(i + 1, condition, function(token, i) {
          if (token[0] !== 'INDENT') {
            return original[0] = 'POST_' + original[0];
          }
        });
        return 1;
      });
    };
    Rewriter.prototype.ensureBalance = function(pairs) {
      var close, level, levels, open, openLine, tag, token, _i, _j, _len, _len2, _ref, _ref2;
      levels = {};
      openLine = {};
      _ref = this.tokens;
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        token = _ref[_i];
        tag = token[0];
        for (_j = 0, _len2 = pairs.length; _j < _len2; _j++) {
          _ref2 = pairs[_j], open = _ref2[0], close = _ref2[1];
          levels[open] |= 0;
          if (tag === open) {
            if (levels[open]++ === 0) {
              openLine[open] = token[2];
            }
          } else if (tag === close && --levels[open] < 0) {
            throw Error("too many " + token[1] + " on line " + (token[2] + 1));
          }
        }
      }
      for (open in levels) {
        level = levels[open];
        if (level > 0) {
          throw Error("unclosed " + open + " on line " + (openLine[open] + 1));
        }
      }
      return this;
    };
    Rewriter.prototype.rewriteClosingParens = function() {
      var debt, key, stack;
      stack = [];
      debt = {};
      for (key in INVERSES) {
        debt[key] = 0;
      }
      return this.scanTokens(function(token, i, tokens) {
        var inv, match, mtag, oppos, tag, val, _ref;
        if (_ref = (tag = token[0]), __indexOf.call(EXPRESSION_START, _ref) >= 0) {
          stack.push(token);
          return 1;
        }
        if (__indexOf.call(EXPRESSION_END, tag) < 0) {
          return 1;
        }
        if (debt[inv = INVERSES[tag]] > 0) {
          debt[inv] -= 1;
          tokens.splice(i, 1);
          return 0;
        }
        match = stack.pop();
        mtag = match[0];
        oppos = INVERSES[mtag];
        if (tag === oppos) {
          return 1;
        }
        debt[mtag] += 1;
        val = [oppos, mtag === 'INDENT' ? match[1] : oppos];
        if (this.tag(i + 2) === mtag) {
          tokens.splice(i + 3, 0, val);
          stack.push(match);
        } else {
          tokens.splice(i, 0, val);
        }
        return 1;
      });
    };
    Rewriter.prototype.indentation = function(token) {
      return [['INDENT', 2, token[2]], ['OUTDENT', 2, token[2]]];
    };
    Rewriter.prototype.tag = function(i) {
      var _ref;
      return (_ref = this.tokens[i]) != null ? _ref[0] : void 0;
    };
    return Rewriter;
  })();
  BALANCED_PAIRS = [['(', ')'], ['[', ']'], ['{', '}'], ['INDENT', 'OUTDENT'], ['CALL_START', 'CALL_END'], ['PARAM_START', 'PARAM_END'], ['INDEX_START', 'INDEX_END']];
  INVERSES = {};
  EXPRESSION_START = [];
  EXPRESSION_END = [];
  for (_i = 0, _len = BALANCED_PAIRS.length; _i < _len; _i++) {
    _ref = BALANCED_PAIRS[_i], left = _ref[0], rite = _ref[1];
    EXPRESSION_START.push(INVERSES[rite] = left);
    EXPRESSION_END.push(INVERSES[left] = rite);
  }
  EXPRESSION_CLOSE = ['CATCH', 'WHEN', 'ELSE', 'FINALLY'].concat(EXPRESSION_END);
  IMPLICIT_FUNC = ['IDENTIFIER', 'SUPER', ')', 'CALL_END', ']', 'INDEX_END', '@', 'THIS'];
  IMPLICIT_CALL = ['IDENTIFIER', 'NUMBER', 'STRING', 'JS', 'REGEX', 'NEW', 'PARAM_START', 'CLASS', 'IF', 'TRY', 'SWITCH', 'THIS', 'BOOL', 'UNARY', 'SUPER', '@', '->', '=>', '[', '(', '{', '--', '++'];
  IMPLICIT_UNSPACED_CALL = ['+', '-'];
  IMPLICIT_BLOCK = ['->', '=>', '{', '[', ','];
  IMPLICIT_END = ['POST_IF', 'FOR', 'WHILE', 'UNTIL', 'WHEN', 'BY', 'LOOP', 'TERMINATOR'];
  SINGLE_LINERS = ['ELSE', '->', '=>', 'TRY', 'FINALLY', 'THEN'];
  SINGLE_CLOSERS = ['TERMINATOR', 'CATCH', 'FINALLY', 'ELSE', 'OUTDENT', 'LEADING_WHEN'];
  LINEBREAKS = ['TERMINATOR', 'INDENT', 'OUTDENT'];
}).call(this);

};require['./lexer'] = new function() {
  var exports = this;
  (function() {
  var ASSIGNED, BOOL, CALLABLE, CODE, COFFEE_ALIASES, COFFEE_ALIAS_MAP, COFFEE_KEYWORDS, COMMENT, COMPARE, COMPOUND_ASSIGN, HEREDOC, HEREDOC_ILLEGAL, HEREDOC_INDENT, HEREGEX, HEREGEX_OMIT, IDENTIFIER, INDEXABLE, JSTOKEN, JS_FORBIDDEN, JS_KEYWORDS, LINE_BREAK, LINE_CONTINUER, LOGIC, Lexer, MATH, MULTILINER, MULTI_DENT, NOT_REGEX, NOT_SPACED_REGEX, NO_NEWLINE, NUMBER, OPERATOR, REGEX, RELATION, RESERVED, Rewriter, SHIFT, SIMPLESTR, TRAILING_SPACES, UNARY, WHITESPACE, compact, count, key, last, starts, _ref;
  var __indexOf = Array.prototype.indexOf || function(item) {
    for (var i = 0, l = this.length; i < l; i++) {
      if (this[i] === item) return i;
    }
    return -1;
  };
  Rewriter = require('./rewriter').Rewriter;
  _ref = require('./helpers'), count = _ref.count, starts = _ref.starts, compact = _ref.compact, last = _ref.last;
  exports.Lexer = Lexer = (function() {
    function Lexer() {}
    Lexer.prototype.tokenize = function(code, opts) {
      var i;
      if (opts == null) {
        opts = {};
      }
      if (WHITESPACE.test(code)) {
        code = "\n" + code;
      }
      code = code.replace(/\r/g, '').replace(TRAILING_SPACES, '');
      this.code = code;
      this.line = opts.line || 0;
      this.indent = 0;
      this.indebt = 0;
      this.outdebt = 0;
      this.indents = [];
      this.tokens = [];
      i = 0;
      while (this.chunk = code.slice(i)) {
        i += this.identifierToken() || this.commentToken() || this.whitespaceToken() || this.lineToken() || this.heredocToken() || this.stringToken() || this.numberToken() || this.regexToken() || this.jsToken() || this.literalToken();
      }
      this.closeIndentation();
      if (opts.rewrite === false) {
        return this.tokens;
      }
      return (new Rewriter).rewrite(this.tokens);
    };
    Lexer.prototype.identifierToken = function() {
      var colon, forcedIdentifier, id, input, match, prev, tag, _ref2, _ref3;
      if (!(match = IDENTIFIER.exec(this.chunk))) {
        return 0;
      }
      input = match[0], id = match[1], colon = match[2];
      if (id === 'own' && this.tag() === 'FOR') {
        this.token('OWN', id);
        return id.length;
      }
      forcedIdentifier = colon || (prev = last(this.tokens)) && (((_ref2 = prev[0]) === '.' || _ref2 === '?.' || _ref2 === '::') || !prev.spaced && prev[0] === '@');
      tag = 'IDENTIFIER';
      if (!forcedIdentifier && (__indexOf.call(JS_KEYWORDS, id) >= 0 || __indexOf.call(COFFEE_KEYWORDS, id) >= 0)) {
        tag = id.toUpperCase();
        if (tag === 'WHEN' && (_ref3 = this.tag(), __indexOf.call(LINE_BREAK, _ref3) >= 0)) {
          tag = 'LEADING_WHEN';
        } else if (tag === 'FOR') {
          this.seenFor = true;
        } else if (tag === 'UNLESS') {
          tag = 'IF';
        } else if (__indexOf.call(UNARY, tag) >= 0) {
          tag = 'UNARY';
        } else if (__indexOf.call(RELATION, tag) >= 0) {
          if (tag !== 'INSTANCEOF' && this.seenFor) {
            tag = 'FOR' + tag;
            this.seenFor = false;
          } else {
            tag = 'RELATION';
            if (this.value() === '!') {
              this.tokens.pop();
              id = '!' + id;
            }
          }
        }
      }
      if (__indexOf.call(JS_FORBIDDEN, id) >= 0) {
        if (forcedIdentifier) {
          tag = 'IDENTIFIER';
          id = new String(id);
          id.reserved = true;
        } else if (__indexOf.call(RESERVED, id) >= 0) {
          this.identifierError(id);
        }
      }
      if (!forcedIdentifier) {
        if (__indexOf.call(COFFEE_ALIASES, id) >= 0) {
          id = COFFEE_ALIAS_MAP[id];
        }
        tag = (function() {
          switch (id) {
            case '!':
              return 'UNARY';
            case '==':
            case '!=':
              return 'COMPARE';
            case '&&':
            case '||':
              return 'LOGIC';
            case 'true':
            case 'false':
            case 'null':
            case 'undefined':
              return 'BOOL';
            case 'break':
            case 'continue':
            case 'debugger':
              return 'STATEMENT';
            default:
              return tag;
          }
        })();
      }
      this.token(tag, id);
      if (colon) {
        this.token(':', ':');
      }
      return input.length;
    };
    Lexer.prototype.numberToken = function() {
      var match, number;
      if (!(match = NUMBER.exec(this.chunk))) {
        return 0;
      }
      number = match[0];
      this.token('NUMBER', number);
      return number.length;
    };
    Lexer.prototype.stringToken = function() {
      var match, string;
      switch (this.chunk.charAt(0)) {
        case "'":
          if (!(match = SIMPLESTR.exec(this.chunk))) {
            return 0;
          }
          this.token('STRING', (string = match[0]).replace(MULTILINER, '\\\n'));
          break;
        case '"':
          if (!(string = this.balancedString(this.chunk, '"'))) {
            return 0;
          }
          if (0 < string.indexOf('#{', 1)) {
            this.interpolateString(string.slice(1, -1));
          } else {
            this.token('STRING', this.escapeLines(string));
          }
          break;
        default:
          return 0;
      }
      this.line += count(string, '\n');
      return string.length;
    };
    Lexer.prototype.heredocToken = function() {
      var doc, heredoc, match, quote;
      if (!(match = HEREDOC.exec(this.chunk))) {
        return 0;
      }
      heredoc = match[0];
      quote = heredoc.charAt(0);
      doc = this.sanitizeHeredoc(match[2], {
        quote: quote,
        indent: null
      });
      if (quote === '"' && 0 <= doc.indexOf('#{')) {
        this.interpolateString(doc, {
          heredoc: true
        });
      } else {
        this.token('STRING', this.makeString(doc, quote, true));
      }
      this.line += count(heredoc, '\n');
      return heredoc.length;
    };
    Lexer.prototype.commentToken = function() {
      var comment, here, match;
      if (!(match = this.chunk.match(COMMENT))) {
        return 0;
      }
      comment = match[0], here = match[1];
      if (here) {
        this.token('HERECOMMENT', this.sanitizeHeredoc(here, {
          herecomment: true,
          indent: Array(this.indent + 1).join(' ')
        }));
        this.token('TERMINATOR', '\n');
      }
      this.line += count(comment, '\n');
      return comment.length;
    };
    Lexer.prototype.jsToken = function() {
      var match, script;
      if (!(this.chunk.charAt(0) === '`' && (match = JSTOKEN.exec(this.chunk)))) {
        return 0;
      }
      this.token('JS', (script = match[0]).slice(1, -1));
      return script.length;
    };
    Lexer.prototype.regexToken = function() {
      var length, match, prev, regex, _ref2;
      if (this.chunk.charAt(0) !== '/') {
        return 0;
      }
      if (match = HEREGEX.exec(this.chunk)) {
        length = this.heregexToken(match);
        this.line += count(match[0], '\n');
        return length;
      }
      prev = last(this.tokens);
      if (prev && (_ref2 = prev[0], __indexOf.call((prev.spaced ? NOT_REGEX : NOT_SPACED_REGEX), _ref2) >= 0)) {
        return 0;
      }
      if (!(match = REGEX.exec(this.chunk))) {
        return 0;
      }
      regex = match[0];
      this.token('REGEX', regex === '//' ? '/(?:)/' : regex);
      return regex.length;
    };
    Lexer.prototype.heregexToken = function(match) {
      var body, flags, heregex, re, tag, tokens, value, _i, _len, _ref2, _ref3, _ref4, _ref5;
      heregex = match[0], body = match[1], flags = match[2];
      if (0 > body.indexOf('#{')) {
        re = body.replace(HEREGEX_OMIT, '').replace(/\//g, '\\/');
        this.token('REGEX', "/" + (re || '(?:)') + "/" + flags);
        return heregex.length;
      }
      this.token('IDENTIFIER', 'RegExp');
      this.tokens.push(['CALL_START', '(']);
      tokens = [];
      _ref2 = this.interpolateString(body, {
        regex: true
      });
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        _ref3 = _ref2[_i], tag = _ref3[0], value = _ref3[1];
        if (tag === 'TOKENS') {
          tokens.push.apply(tokens, value);
        } else {
          if (!(value = value.replace(HEREGEX_OMIT, ''))) {
            continue;
          }
          value = value.replace(/\\/g, '\\\\');
          tokens.push(['STRING', this.makeString(value, '"', true)]);
        }
        tokens.push(['+', '+']);
      }
      tokens.pop();
      if (((_ref4 = tokens[0]) != null ? _ref4[0] : void 0) !== 'STRING') {
        this.tokens.push(['STRING', '""'], ['+', '+']);
      }
      (_ref5 = this.tokens).push.apply(_ref5, tokens);
      if (flags) {
        this.tokens.push([',', ','], ['STRING', '"' + flags + '"']);
      }
      this.token(')', ')');
      return heregex.length;
    };
    Lexer.prototype.lineToken = function() {
      var diff, indent, match, noNewlines, prev, size;
      if (!(match = MULTI_DENT.exec(this.chunk))) {
        return 0;
      }
      indent = match[0];
      this.line += count(indent, '\n');
      prev = last(this.tokens, 1);
      size = indent.length - 1 - indent.lastIndexOf('\n');
      noNewlines = this.unfinished();
      if (size - this.indebt === this.indent) {
        if (noNewlines) {
          this.suppressNewlines();
        } else {
          this.newlineToken();
        }
        return indent.length;
      }
      if (size > this.indent) {
        if (noNewlines) {
          this.indebt = size - this.indent;
          this.suppressNewlines();
          return indent.length;
        }
        diff = size - this.indent + this.outdebt;
        this.token('INDENT', diff);
        this.indents.push(diff);
        this.outdebt = this.indebt = 0;
      } else {
        this.indebt = 0;
        this.outdentToken(this.indent - size, noNewlines);
      }
      this.indent = size;
      return indent.length;
    };
    Lexer.prototype.outdentToken = function(moveOut, noNewlines, close) {
      var dent, len;
      while (moveOut > 0) {
        len = this.indents.length - 1;
        if (this.indents[len] === void 0) {
          moveOut = 0;
        } else if (this.indents[len] === this.outdebt) {
          moveOut -= this.outdebt;
          this.outdebt = 0;
        } else if (this.indents[len] < this.outdebt) {
          this.outdebt -= this.indents[len];
          moveOut -= this.indents[len];
        } else {
          dent = this.indents.pop() - this.outdebt;
          moveOut -= dent;
          this.outdebt = 0;
          this.token('OUTDENT', dent);
        }
      }
      if (dent) {
        this.outdebt -= moveOut;
      }
      if (!(this.tag() === 'TERMINATOR' || noNewlines)) {
        this.token('TERMINATOR', '\n');
      }
      return this;
    };
    Lexer.prototype.whitespaceToken = function() {
      var match, nline, prev;
      if (!((match = WHITESPACE.exec(this.chunk)) || (nline = this.chunk.charAt(0) === '\n'))) {
        return 0;
      }
      prev = last(this.tokens);
      if (prev) {
        prev[match ? 'spaced' : 'newLine'] = true;
      }
      if (match) {
        return match[0].length;
      } else {
        return 0;
      }
    };
    Lexer.prototype.newlineToken = function() {
      if (this.tag() !== 'TERMINATOR') {
        this.token('TERMINATOR', '\n');
      }
      return this;
    };
    Lexer.prototype.suppressNewlines = function() {
      if (this.value() === '\\') {
        this.tokens.pop();
      }
      return this;
    };
    Lexer.prototype.literalToken = function() {
      var match, prev, tag, value, _ref2, _ref3, _ref4, _ref5;
      if (match = OPERATOR.exec(this.chunk)) {
        value = match[0];
        if (CODE.test(value)) {
          this.tagParameters();
        }
      } else {
        value = this.chunk.charAt(0);
      }
      tag = value;
      prev = last(this.tokens);
      if (value === '=' && prev) {
        if (!prev[1].reserved && (_ref2 = prev[1], __indexOf.call(JS_FORBIDDEN, _ref2) >= 0)) {
          this.assignmentError();
        }
        if ((_ref3 = prev[1]) === '||' || _ref3 === '&&') {
          prev[0] = 'COMPOUND_ASSIGN';
          prev[1] += '=';
          return value.length;
        }
      }
      if (value === ';') {
        tag = 'TERMINATOR';
      } else if (__indexOf.call(MATH, value) >= 0) {
        tag = 'MATH';
      } else if (__indexOf.call(COMPARE, value) >= 0) {
        tag = 'COMPARE';
      } else if (__indexOf.call(COMPOUND_ASSIGN, value) >= 0) {
        tag = 'COMPOUND_ASSIGN';
      } else if (__indexOf.call(UNARY, value) >= 0) {
        tag = 'UNARY';
      } else if (__indexOf.call(SHIFT, value) >= 0) {
        tag = 'SHIFT';
      } else if (__indexOf.call(LOGIC, value) >= 0 || value === '?' && (prev != null ? prev.spaced : void 0)) {
        tag = 'LOGIC';
      } else if (prev && !prev.spaced) {
        if (value === '(' && (_ref4 = prev[0], __indexOf.call(CALLABLE, _ref4) >= 0)) {
          if (prev[0] === '?') {
            prev[0] = 'FUNC_EXIST';
          }
          tag = 'CALL_START';
        } else if (value === '[' && (_ref5 = prev[0], __indexOf.call(INDEXABLE, _ref5) >= 0)) {
          tag = 'INDEX_START';
          switch (prev[0]) {
            case '?':
              prev[0] = 'INDEX_SOAK';
              break;
            case '::':
              prev[0] = 'INDEX_PROTO';
          }
        }
      }
      this.token(tag, value);
      return value.length;
    };
    Lexer.prototype.sanitizeHeredoc = function(doc, options) {
      var attempt, herecomment, indent, match, _ref2;
      indent = options.indent, herecomment = options.herecomment;
      if (herecomment) {
        if (HEREDOC_ILLEGAL.test(doc)) {
          throw new Error("block comment cannot contain \"*/\", starting on line " + (this.line + 1));
        }
        if (doc.indexOf('\n') <= 0) {
          return doc;
        }
      } else {
        while (match = HEREDOC_INDENT.exec(doc)) {
          attempt = match[1];
          if (indent === null || (0 < (_ref2 = attempt.length) && _ref2 < indent.length)) {
            indent = attempt;
          }
        }
      }
      if (indent) {
        doc = doc.replace(RegExp("\\n" + indent, "g"), '\n');
      }
      if (!herecomment) {
        doc = doc.replace(/^\n/, '');
      }
      return doc;
    };
    Lexer.prototype.tagParameters = function() {
      var i, stack, tok, tokens;
      if (this.tag() !== ')') {
        return this;
      }
      stack = [];
      tokens = this.tokens;
      i = tokens.length;
      tokens[--i][0] = 'PARAM_END';
      while (tok = tokens[--i]) {
        switch (tok[0]) {
          case ')':
            stack.push(tok);
            break;
          case '(':
          case 'CALL_START':
            if (stack.length) {
              stack.pop();
            } else if (tok[0] === '(') {
              tok[0] = 'PARAM_START';
              return this;
            } else {
              return this;
            }
        }
      }
      return this;
    };
    Lexer.prototype.closeIndentation = function() {
      return this.outdentToken(this.indent);
    };
    Lexer.prototype.identifierError = function(word) {
      throw SyntaxError("Reserved word \"" + word + "\" on line " + (this.line + 1));
    };
    Lexer.prototype.assignmentError = function() {
      throw SyntaxError("Reserved word \"" + (this.value()) + "\" on line " + (this.line + 1) + " can't be assigned");
    };
    Lexer.prototype.balancedString = function(str, end) {
      var i, letter, match, prev, stack, _ref2;
      stack = [end];
      for (i = 1, _ref2 = str.length; 1 <= _ref2 ? i < _ref2 : i > _ref2; 1 <= _ref2 ? i++ : i--) {
        switch (letter = str.charAt(i)) {
          case '\\':
            i++;
            continue;
          case end:
            stack.pop();
            if (!stack.length) {
              return str.slice(0, i + 1);
            }
            end = stack[stack.length - 1];
            continue;
        }
        if (end === '}' && (letter === '"' || letter === "'")) {
          stack.push(end = letter);
        } else if (end === '}' && letter === '/' && (match = HEREGEX.exec(str.slice(i)) || REGEX.exec(str.slice(i)))) {
          i += match[0].length - 1;
        } else if (end === '}' && letter === '{') {
          stack.push(end = '}');
        } else if (end === '"' && prev === '#' && letter === '{') {
          stack.push(end = '}');
        }
        prev = letter;
      }
      throw new Error("missing " + (stack.pop()) + ", starting on line " + (this.line + 1));
    };
    Lexer.prototype.interpolateString = function(str, options) {
      var expr, heredoc, i, inner, interpolated, len, letter, nested, pi, regex, tag, tokens, value, _len, _ref2, _ref3, _ref4;
      if (options == null) {
        options = {};
      }
      heredoc = options.heredoc, regex = options.regex;
      tokens = [];
      pi = 0;
      i = -1;
      while (letter = str.charAt(i += 1)) {
        if (letter === '\\') {
          i += 1;
          continue;
        }
        if (!(letter === '#' && str.charAt(i + 1) === '{' && (expr = this.balancedString(str.slice(i + 1), '}')))) {
          continue;
        }
        if (pi < i) {
          tokens.push(['NEOSTRING', str.slice(pi, i)]);
        }
        inner = expr.slice(1, -1);
        if (inner.length) {
          nested = new Lexer().tokenize(inner, {
            line: this.line,
            rewrite: false
          });
          nested.pop();
          if (((_ref2 = nested[0]) != null ? _ref2[0] : void 0) === 'TERMINATOR') {
            nested.shift();
          }
          if (len = nested.length) {
            if (len > 1) {
              nested.unshift(['(', '(']);
              nested.push([')', ')']);
            }
            tokens.push(['TOKENS', nested]);
          }
        }
        i += expr.length;
        pi = i + 1;
      }
      if ((i > pi && pi < str.length)) {
        tokens.push(['NEOSTRING', str.slice(pi)]);
      }
      if (regex) {
        return tokens;
      }
      if (!tokens.length) {
        return this.token('STRING', '""');
      }
      if (tokens[0][0] !== 'NEOSTRING') {
        tokens.unshift(['', '']);
      }
      if (interpolated = tokens.length > 1) {
        this.token('(', '(');
      }
      for (i = 0, _len = tokens.length; i < _len; i++) {
        _ref3 = tokens[i], tag = _ref3[0], value = _ref3[1];
        if (i) {
          this.token('+', '+');
        }
        if (tag === 'TOKENS') {
          (_ref4 = this.tokens).push.apply(_ref4, value);
        } else {
          this.token('STRING', this.makeString(value, '"', heredoc));
        }
      }
      if (interpolated) {
        this.token(')', ')');
      }
      return tokens;
    };
    Lexer.prototype.token = function(tag, value) {
      return this.tokens.push([tag, value, this.line]);
    };
    Lexer.prototype.tag = function(index, tag) {
      var tok;
      return (tok = last(this.tokens, index)) && (tag ? tok[0] = tag : tok[0]);
    };
    Lexer.prototype.value = function(index, val) {
      var tok;
      return (tok = last(this.tokens, index)) && (val ? tok[1] = val : tok[1]);
    };
    Lexer.prototype.unfinished = function() {
      var prev, value;
      return LINE_CONTINUER.test(this.chunk) || (prev = last(this.tokens, 1)) && prev[0] !== '.' && (value = this.value()) && !value.reserved && NO_NEWLINE.test(value) && !CODE.test(value) && !ASSIGNED.test(this.chunk);
    };
    Lexer.prototype.escapeLines = function(str, heredoc) {
      return str.replace(MULTILINER, heredoc ? '\\n' : '');
    };
    Lexer.prototype.makeString = function(body, quote, heredoc) {
      if (!body) {
        return quote + quote;
      }
      body = body.replace(/\\([\s\S])/g, function(match, contents) {
        if (contents === '\n' || contents === quote) {
          return contents;
        } else {
          return match;
        }
      });
      body = body.replace(RegExp("" + quote, "g"), '\\$&');
      return quote + this.escapeLines(body, heredoc) + quote;
    };
    return Lexer;
  })();
  JS_KEYWORDS = ['true', 'false', 'null', 'this', 'new', 'delete', 'typeof', 'in', 'instanceof', 'return', 'throw', 'break', 'continue', 'debugger', 'if', 'else', 'switch', 'for', 'while', 'do', 'try', 'catch', 'finally', 'class', 'extends', 'super'];
  COFFEE_KEYWORDS = ['undefined', 'then', 'unless', 'until', 'loop', 'of', 'by', 'when', 'include', 'as'];
  COFFEE_ALIAS_MAP = {
    and: '&&',
    or: '||',
    is: '==',
    isnt: '!=',
    not: '!',
    yes: 'true',
    no: 'false',
    on: 'true',
    off: 'false'
  };
  COFFEE_ALIASES = (function() {
    var _results;
    _results = [];
    for (key in COFFEE_ALIAS_MAP) {
      _results.push(key);
    }
    return _results;
  })();
  COFFEE_KEYWORDS = COFFEE_KEYWORDS.concat(COFFEE_ALIASES);
  RESERVED = ['case', 'default', 'function', 'var', 'void', 'with', 'const', 'let', 'enum', 'export', 'import', 'native', '__hasProp', '__extends', '__slice', '__bind', '__indexOf'];
  JS_FORBIDDEN = JS_KEYWORDS.concat(RESERVED);
  exports.RESERVED = RESERVED.concat(JS_KEYWORDS).concat(COFFEE_KEYWORDS);
  IDENTIFIER = /^([$A-Za-z_\x7f-\uffff][$\w\x7f-\uffff]*)([^\n\S]*:(?!:))?/;
  NUMBER = /^0x[\da-f]+|^\d*\.?\d+(?:e[+-]?\d+)?/i;
  HEREDOC = /^("""|''')([\s\S]*?)(?:\n[^\n\S]*)?\1/;
  OPERATOR = /^(?:[-=]>|[-+*\/%<>&|^!?=]=|>>>=?|([-+:])\1|([&|<>])\2=?|\?\.|\.{2,3})/;
  WHITESPACE = /^[^\n\S]+/;
  COMMENT = /^###([^#][\s\S]*?)(?:###[^\n\S]*|(?:###)?$)|^(?:\s*#(?!##[^#]).*)+/;
  CODE = /^[-=]>/;
  MULTI_DENT = /^(?:\n[^\n\S]*)+/;
  SIMPLESTR = /^'[^\\']*(?:\\.[^\\']*)*'/;
  JSTOKEN = /^`[^\\`]*(?:\\.[^\\`]*)*`/;
  REGEX = /^\/(?![\s=])[^[\/\n\\]*(?:(?:\\[\s\S]|\[[^\]\n\\]*(?:\\[\s\S][^\]\n\\]*)*])[^[\/\n\\]*)*\/[imgy]{0,4}(?!\w)/;
  HEREGEX = /^\/{3}([\s\S]+?)\/{3}([imgy]{0,4})(?!\w)/;
  HEREGEX_OMIT = /\s+(?:#.*)?/g;
  MULTILINER = /\n/g;
  HEREDOC_INDENT = /\n+([^\n\S]*)/g;
  HEREDOC_ILLEGAL = /\*\//;
  ASSIGNED = /^\s*@?([$A-Za-z_][$\w\x7f-\uffff]*|['"].*['"])[^\n\S]*?[:=][^:=>]/;
  LINE_CONTINUER = /^\s*(?:,|\??\.(?![.\d])|::)/;
  TRAILING_SPACES = /\s+$/;
  NO_NEWLINE = /^(?:[-+*&|\/%=<>!.\\][<>=&|]*|and|or|is(?:nt)?|n(?:ot|ew)|delete|typeof|instanceof)$/;
  COMPOUND_ASSIGN = ['-=', '+=', '/=', '*=', '%=', '||=', '&&=', '?=', '<<=', '>>=', '>>>=', '&=', '^=', '|='];
  UNARY = ['!', '~', 'NEW', 'TYPEOF', 'DELETE', 'DO'];
  LOGIC = ['&&', '||', '&', '|', '^'];
  SHIFT = ['<<', '>>', '>>>'];
  COMPARE = ['==', '!=', '<', '>', '<=', '>='];
  MATH = ['*', '/', '%'];
  RELATION = ['IN', 'OF', 'INSTANCEOF'];
  BOOL = ['TRUE', 'FALSE', 'NULL', 'UNDEFINED'];
  NOT_REGEX = ['NUMBER', 'REGEX', 'BOOL', '++', '--', ']'];
  NOT_SPACED_REGEX = NOT_REGEX.concat(')', '}', 'THIS', 'IDENTIFIER', 'STRING');
  CALLABLE = ['IDENTIFIER', 'STRING', 'REGEX', ')', ']', '}', '?', '::', '@', 'THIS', 'SUPER'];
  INDEXABLE = CALLABLE.concat('NUMBER', 'BOOL');
  LINE_BREAK = ['INDENT', 'OUTDENT', 'TERMINATOR'];
}).call(this);

};require['./parser'] = new function() {
  var exports = this;
  /* Jison generated parser */
var parser = (function(){
var parser = {trace: function trace() { },
yy: {},
symbols_: {"error":2,"Root":3,"Body":4,"Block":5,"TERMINATOR":6,"Line":7,"Expression":8,"Statement":9,"Return":10,"Throw":11,"Comment":12,"STATEMENT":13,"Value":14,"Invocation":15,"Code":16,"Operation":17,"Assign":18,"If":19,"Try":20,"Include":21,"While":22,"For":23,"Switch":24,"Class":25,"INDENT":26,"OUTDENT":27,"Identifier":28,"IDENTIFIER":29,"AlphaNumeric":30,"NUMBER":31,"STRING":32,"Literal":33,"JS":34,"REGEX":35,"BOOL":36,"Assignable":37,"=":38,"AssignObj":39,"ObjAssignable":40,":":41,"ThisProperty":42,"RETURN":43,"HERECOMMENT":44,"PARAM_START":45,"ParamList":46,"PARAM_END":47,"FuncGlyph":48,"->":49,"=>":50,"OptComma":51,",":52,"Param":53,"ParamVar":54,"...":55,"Array":56,"Object":57,"Splat":58,"SimpleAssignable":59,"Accessor":60,"Parenthetical":61,"Range":62,"This":63,".":64,"?.":65,"::":66,"Index":67,"INDEX_START":68,"IndexValue":69,"INDEX_END":70,"INDEX_SOAK":71,"INDEX_PROTO":72,"Slice":73,"{":74,"AssignList":75,"}":76,"CLASS":77,"EXTENDS":78,"OptFuncExist":79,"Arguments":80,"SUPER":81,"FUNC_EXIST":82,"CALL_START":83,"CALL_END":84,"ArgList":85,"THIS":86,"@":87,"[":88,"]":89,"RangeDots":90,"..":91,"Arg":92,"SimpleArgs":93,"TRY":94,"Catch":95,"FINALLY":96,"CATCH":97,"THROW":98,"INCLUDE":99,"Namespace":100,"AS":101,"(":102,")":103,"WhileSource":104,"WHILE":105,"WHEN":106,"UNTIL":107,"Loop":108,"LOOP":109,"ForBody":110,"FOR":111,"ForStart":112,"ForSource":113,"ForVariables":114,"OWN":115,"ForValue":116,"FORIN":117,"FOROF":118,"BY":119,"SWITCH":120,"Whens":121,"ELSE":122,"When":123,"LEADING_WHEN":124,"IfBlock":125,"IF":126,"POST_IF":127,"UNARY":128,"-":129,"+":130,"--":131,"++":132,"?":133,"MATH":134,"SHIFT":135,"COMPARE":136,"LOGIC":137,"RELATION":138,"COMPOUND_ASSIGN":139,"$accept":0,"$end":1},
terminals_: {2:"error",6:"TERMINATOR",13:"STATEMENT",26:"INDENT",27:"OUTDENT",29:"IDENTIFIER",31:"NUMBER",32:"STRING",34:"JS",35:"REGEX",36:"BOOL",38:"=",41:":",43:"RETURN",44:"HERECOMMENT",45:"PARAM_START",47:"PARAM_END",49:"->",50:"=>",52:",",55:"...",64:".",65:"?.",66:"::",68:"INDEX_START",70:"INDEX_END",71:"INDEX_SOAK",72:"INDEX_PROTO",74:"{",76:"}",77:"CLASS",78:"EXTENDS",81:"SUPER",82:"FUNC_EXIST",83:"CALL_START",84:"CALL_END",86:"THIS",87:"@",88:"[",89:"]",91:"..",94:"TRY",96:"FINALLY",97:"CATCH",98:"THROW",99:"INCLUDE",101:"AS",102:"(",103:")",105:"WHILE",106:"WHEN",107:"UNTIL",109:"LOOP",111:"FOR",115:"OWN",117:"FORIN",118:"FOROF",119:"BY",120:"SWITCH",122:"ELSE",124:"LEADING_WHEN",126:"IF",127:"POST_IF",128:"UNARY",129:"-",130:"+",131:"--",132:"++",133:"?",134:"MATH",135:"SHIFT",136:"COMPARE",137:"LOGIC",138:"RELATION",139:"COMPOUND_ASSIGN"},
productions_: [0,[3,0],[3,1],[3,2],[4,1],[4,3],[4,2],[7,1],[7,1],[9,1],[9,1],[9,1],[9,1],[8,1],[8,1],[8,1],[8,1],[8,1],[8,1],[8,1],[8,1],[8,1],[8,1],[8,1],[8,1],[5,2],[5,3],[28,1],[30,1],[30,1],[33,1],[33,1],[33,1],[33,1],[18,3],[18,5],[39,1],[39,3],[39,5],[39,1],[40,1],[40,1],[40,1],[10,2],[10,1],[12,1],[16,5],[16,2],[48,1],[48,1],[51,0],[51,1],[46,0],[46,1],[46,3],[53,1],[53,2],[53,3],[54,1],[54,1],[54,1],[54,1],[58,2],[59,1],[59,2],[59,2],[59,1],[37,1],[37,1],[37,1],[14,1],[14,1],[14,1],[14,1],[14,1],[60,2],[60,2],[60,2],[60,1],[60,1],[67,3],[67,2],[67,2],[69,1],[69,1],[57,4],[75,0],[75,1],[75,3],[75,4],[75,6],[25,1],[25,2],[25,3],[25,4],[25,2],[25,3],[25,4],[25,5],[15,3],[15,3],[15,1],[15,2],[79,0],[79,1],[80,2],[80,4],[63,1],[63,1],[42,2],[56,2],[56,4],[90,1],[90,1],[62,5],[73,3],[73,2],[73,2],[85,1],[85,3],[85,4],[85,4],[85,6],[92,1],[92,1],[93,1],[93,3],[20,2],[20,3],[20,4],[20,5],[95,3],[11,2],[21,2],[21,4],[100,1],[100,3],[61,3],[61,5],[104,2],[104,4],[104,2],[104,4],[22,2],[22,2],[22,2],[22,1],[108,2],[108,2],[23,2],[23,2],[23,2],[110,2],[110,2],[112,2],[112,3],[116,1],[116,1],[116,1],[114,1],[114,3],[113,2],[113,2],[113,4],[113,4],[113,4],[113,6],[113,6],[24,5],[24,7],[24,4],[24,6],[121,1],[121,2],[123,3],[123,4],[125,3],[125,5],[19,1],[19,3],[19,3],[19,3],[17,2],[17,2],[17,2],[17,2],[17,2],[17,2],[17,2],[17,2],[17,3],[17,3],[17,3],[17,3],[17,3],[17,3],[17,3],[17,3],[17,5],[17,3]],
performAction: function anonymous(yytext,yyleng,yylineno,yy,yystate,$$,_$) {

var $0 = $$.length - 1;
switch (yystate) {
case 1:return this.$ = new yy.Block;
break;
case 2:return this.$ = $$[$0];
break;
case 3:return this.$ = $$[$0-1];
break;
case 4:this.$ = yy.Block.wrap([$$[$0]]);
break;
case 5:this.$ = $$[$0-2].push($$[$0]);
break;
case 6:this.$ = $$[$0-1];
break;
case 7:this.$ = $$[$0];
break;
case 8:this.$ = $$[$0];
break;
case 9:this.$ = $$[$0];
break;
case 10:this.$ = $$[$0];
break;
case 11:this.$ = $$[$0];
break;
case 12:this.$ = new yy.Literal($$[$0]);
break;
case 13:this.$ = $$[$0];
break;
case 14:this.$ = $$[$0];
break;
case 15:this.$ = $$[$0];
break;
case 16:this.$ = $$[$0];
break;
case 17:this.$ = $$[$0];
break;
case 18:this.$ = $$[$0];
break;
case 19:this.$ = $$[$0];
break;
case 20:this.$ = $$[$0];
break;
case 21:this.$ = $$[$0];
break;
case 22:this.$ = $$[$0];
break;
case 23:this.$ = $$[$0];
break;
case 24:this.$ = $$[$0];
break;
case 25:this.$ = new yy.Block;
break;
case 26:this.$ = $$[$0-1];
break;
case 27:this.$ = new yy.Literal($$[$0]);
break;
case 28:this.$ = new yy.Literal($$[$0]);
break;
case 29:this.$ = new yy.Literal($$[$0]);
break;
case 30:this.$ = $$[$0];
break;
case 31:this.$ = new yy.Literal($$[$0]);
break;
case 32:this.$ = new yy.Literal($$[$0]);
break;
case 33:this.$ = (function () {
        var val;
        val = new yy.Literal($$[$0]);
        if ($$[$0] === 'undefined') {
          val.isUndefined = true;
        }
        return val;
      }());
break;
case 34:this.$ = new yy.Assign($$[$0-2], $$[$0]);
break;
case 35:this.$ = new yy.Assign($$[$0-4], $$[$0-1]);
break;
case 36:this.$ = new yy.Value($$[$0]);
break;
case 37:this.$ = new yy.Assign(new yy.Value($$[$0-2]), $$[$0], 'object');
break;
case 38:this.$ = new yy.Assign(new yy.Value($$[$0-4]), $$[$0-1], 'object');
break;
case 39:this.$ = $$[$0];
break;
case 40:this.$ = $$[$0];
break;
case 41:this.$ = $$[$0];
break;
case 42:this.$ = $$[$0];
break;
case 43:this.$ = new yy.Return($$[$0]);
break;
case 44:this.$ = new yy.Return;
break;
case 45:this.$ = new yy.Comment($$[$0]);
break;
case 46:this.$ = new yy.Code($$[$0-3], $$[$0], $$[$0-1]);
break;
case 47:this.$ = new yy.Code([], $$[$0], $$[$0-1]);
break;
case 48:this.$ = 'func';
break;
case 49:this.$ = 'boundfunc';
break;
case 50:this.$ = $$[$0];
break;
case 51:this.$ = $$[$0];
break;
case 52:this.$ = [];
break;
case 53:this.$ = [$$[$0]];
break;
case 54:this.$ = $$[$0-2].concat($$[$0]);
break;
case 55:this.$ = new yy.Param($$[$0]);
break;
case 56:this.$ = new yy.Param($$[$0-1], null, true);
break;
case 57:this.$ = new yy.Param($$[$0-2], $$[$0]);
break;
case 58:this.$ = $$[$0];
break;
case 59:this.$ = $$[$0];
break;
case 60:this.$ = $$[$0];
break;
case 61:this.$ = $$[$0];
break;
case 62:this.$ = new yy.Splat($$[$0-1]);
break;
case 63:this.$ = new yy.Value($$[$0]);
break;
case 64:this.$ = $$[$0-1].push($$[$0]);
break;
case 65:this.$ = new yy.Value($$[$0-1], [$$[$0]]);
break;
case 66:this.$ = $$[$0];
break;
case 67:this.$ = $$[$0];
break;
case 68:this.$ = new yy.Value($$[$0]);
break;
case 69:this.$ = new yy.Value($$[$0]);
break;
case 70:this.$ = $$[$0];
break;
case 71:this.$ = new yy.Value($$[$0]);
break;
case 72:this.$ = new yy.Value($$[$0]);
break;
case 73:this.$ = new yy.Value($$[$0]);
break;
case 74:this.$ = $$[$0];
break;
case 75:this.$ = new yy.Access($$[$0]);
break;
case 76:this.$ = new yy.Access($$[$0], 'soak');
break;
case 77:this.$ = new yy.Access($$[$0], 'proto');
break;
case 78:this.$ = new yy.Access(new yy.Literal('prototype'));
break;
case 79:this.$ = $$[$0];
break;
case 80:this.$ = $$[$0-1];
break;
case 81:this.$ = yy.extend($$[$0], {
          soak: true
        });
break;
case 82:this.$ = yy.extend($$[$0], {
          proto: true
        });
break;
case 83:this.$ = new yy.Index($$[$0]);
break;
case 84:this.$ = new yy.Slice($$[$0]);
break;
case 85:this.$ = new yy.Obj($$[$0-2], $$[$0-3].generated);
break;
case 86:this.$ = [];
break;
case 87:this.$ = [$$[$0]];
break;
case 88:this.$ = $$[$0-2].concat($$[$0]);
break;
case 89:this.$ = $$[$0-3].concat($$[$0]);
break;
case 90:this.$ = $$[$0-5].concat($$[$0-2]);
break;
case 91:this.$ = new yy.Class;
break;
case 92:this.$ = new yy.Class(null, null, $$[$0]);
break;
case 93:this.$ = new yy.Class(null, $$[$0]);
break;
case 94:this.$ = new yy.Class(null, $$[$0-1], $$[$0]);
break;
case 95:this.$ = new yy.Class($$[$0]);
break;
case 96:this.$ = new yy.Class($$[$0-1], null, $$[$0]);
break;
case 97:this.$ = new yy.Class($$[$0-2], $$[$0]);
break;
case 98:this.$ = new yy.Class($$[$0-3], $$[$0-1], $$[$0]);
break;
case 99:this.$ = new yy.Call($$[$0-2], $$[$0], $$[$0-1]);
break;
case 100:this.$ = new yy.Call($$[$0-2], $$[$0], $$[$0-1]);
break;
case 101:this.$ = new yy.Call('super', [new yy.Splat(new yy.Literal('arguments'))]);
break;
case 102:this.$ = new yy.Call('super', $$[$0]);
break;
case 103:this.$ = false;
break;
case 104:this.$ = true;
break;
case 105:this.$ = [];
break;
case 106:this.$ = $$[$0-2];
break;
case 107:this.$ = new yy.Value(new yy.Literal('this'));
break;
case 108:this.$ = new yy.Value(new yy.Literal('this'));
break;
case 109:this.$ = new yy.Value(new yy.Literal('this'), [new yy.Access($$[$0])], 'this');
break;
case 110:this.$ = new yy.Arr([]);
break;
case 111:this.$ = new yy.Arr($$[$0-2]);
break;
case 112:this.$ = 'inclusive';
break;
case 113:this.$ = 'exclusive';
break;
case 114:this.$ = new yy.Range($$[$0-3], $$[$0-1], $$[$0-2]);
break;
case 115:this.$ = new yy.Range($$[$0-2], $$[$0], $$[$0-1]);
break;
case 116:this.$ = new yy.Range($$[$0-1], null, $$[$0]);
break;
case 117:this.$ = new yy.Range(null, $$[$0], $$[$0-1]);
break;
case 118:this.$ = [$$[$0]];
break;
case 119:this.$ = $$[$0-2].concat($$[$0]);
break;
case 120:this.$ = $$[$0-3].concat($$[$0]);
break;
case 121:this.$ = $$[$0-2];
break;
case 122:this.$ = $$[$0-5].concat($$[$0-2]);
break;
case 123:this.$ = $$[$0];
break;
case 124:this.$ = $$[$0];
break;
case 125:this.$ = $$[$0];
break;
case 126:this.$ = [].concat($$[$0-2], $$[$0]);
break;
case 127:this.$ = new yy.Try($$[$0]);
break;
case 128:this.$ = new yy.Try($$[$0-1], $$[$0][0], $$[$0][1]);
break;
case 129:this.$ = new yy.Try($$[$0-2], null, null, $$[$0]);
break;
case 130:this.$ = new yy.Try($$[$0-3], $$[$0-2][0], $$[$0-2][1], $$[$0]);
break;
case 131:this.$ = [$$[$0-1], $$[$0]];
break;
case 132:this.$ = new yy.Throw($$[$0]);
break;
case 133:this.$ = new yy.Include($$[$0]);
break;
case 134:this.$ = new yy.Include($$[$0-2], $$[$0]);
break;
case 135:this.$ = new yy.Namespace($$[$0]);
break;
case 136:this.$ = new yy.Namespace($$[$0], $$[$0-2]);
break;
case 137:this.$ = new yy.Parens($$[$0-1]);
break;
case 138:this.$ = new yy.Parens($$[$0-2]);
break;
case 139:this.$ = new yy.While($$[$0]);
break;
case 140:this.$ = new yy.While($$[$0-2], {
          guard: $$[$0]
        });
break;
case 141:this.$ = new yy.While($$[$0], {
          invert: true
        });
break;
case 142:this.$ = new yy.While($$[$0-2], {
          invert: true,
          guard: $$[$0]
        });
break;
case 143:this.$ = $$[$0-1].addBody($$[$0]);
break;
case 144:this.$ = $$[$0].addBody(yy.Block.wrap([$$[$0-1]]));
break;
case 145:this.$ = $$[$0].addBody(yy.Block.wrap([$$[$0-1]]));
break;
case 146:this.$ = $$[$0];
break;
case 147:this.$ = new yy.While(new yy.Literal('true')).addBody($$[$0]);
break;
case 148:this.$ = new yy.While(new yy.Literal('true')).addBody(yy.Block.wrap([$$[$0]]));
break;
case 149:this.$ = new yy.For($$[$0-1], $$[$0]);
break;
case 150:this.$ = new yy.For($$[$0-1], $$[$0]);
break;
case 151:this.$ = new yy.For($$[$0], $$[$0-1]);
break;
case 152:this.$ = {
          source: new yy.Value($$[$0])
        };
break;
case 153:this.$ = (function () {
        $$[$0].own = $$[$0-1].own;
        $$[$0].name = $$[$0-1][0];
        $$[$0].index = $$[$0-1][1];
        return $$[$0];
      }());
break;
case 154:this.$ = $$[$0];
break;
case 155:this.$ = (function () {
        $$[$0].own = true;
        return $$[$0];
      }());
break;
case 156:this.$ = $$[$0];
break;
case 157:this.$ = new yy.Value($$[$0]);
break;
case 158:this.$ = new yy.Value($$[$0]);
break;
case 159:this.$ = [$$[$0]];
break;
case 160:this.$ = [$$[$0-2], $$[$0]];
break;
case 161:this.$ = {
          source: $$[$0]
        };
break;
case 162:this.$ = {
          source: $$[$0],
          object: true
        };
break;
case 163:this.$ = {
          source: $$[$0-2],
          guard: $$[$0]
        };
break;
case 164:this.$ = {
          source: $$[$0-2],
          guard: $$[$0],
          object: true
        };
break;
case 165:this.$ = {
          source: $$[$0-2],
          step: $$[$0]
        };
break;
case 166:this.$ = {
          source: $$[$0-4],
          guard: $$[$0-2],
          step: $$[$0]
        };
break;
case 167:this.$ = {
          source: $$[$0-4],
          step: $$[$0-2],
          guard: $$[$0]
        };
break;
case 168:this.$ = new yy.Switch($$[$0-3], $$[$0-1]);
break;
case 169:this.$ = new yy.Switch($$[$0-5], $$[$0-3], $$[$0-1]);
break;
case 170:this.$ = new yy.Switch(null, $$[$0-1]);
break;
case 171:this.$ = new yy.Switch(null, $$[$0-3], $$[$0-1]);
break;
case 172:this.$ = $$[$0];
break;
case 173:this.$ = $$[$0-1].concat($$[$0]);
break;
case 174:this.$ = [[$$[$0-1], $$[$0]]];
break;
case 175:this.$ = [[$$[$0-2], $$[$0-1]]];
break;
case 176:this.$ = new yy.If($$[$0-1], $$[$0], {
          type: $$[$0-2]
        });
break;
case 177:this.$ = $$[$0-4].addElse(new yy.If($$[$0-1], $$[$0], {
          type: $$[$0-2]
        }));
break;
case 178:this.$ = $$[$0];
break;
case 179:this.$ = $$[$0-2].addElse($$[$0]);
break;
case 180:this.$ = new yy.If($$[$0], yy.Block.wrap([$$[$0-2]]), {
          type: $$[$0-1],
          statement: true
        });
break;
case 181:this.$ = new yy.If($$[$0], yy.Block.wrap([$$[$0-2]]), {
          type: $$[$0-1],
          statement: true
        });
break;
case 182:this.$ = new yy.Op($$[$0-1], $$[$0]);
break;
case 183:this.$ = new yy.Op('-', $$[$0]);
break;
case 184:this.$ = new yy.Op('+', $$[$0]);
break;
case 185:this.$ = new yy.Op('--', $$[$0]);
break;
case 186:this.$ = new yy.Op('++', $$[$0]);
break;
case 187:this.$ = new yy.Op('--', $$[$0-1], null, true);
break;
case 188:this.$ = new yy.Op('++', $$[$0-1], null, true);
break;
case 189:this.$ = new yy.Existence($$[$0-1]);
break;
case 190:this.$ = new yy.Op('+', $$[$0-2], $$[$0]);
break;
case 191:this.$ = new yy.Op('-', $$[$0-2], $$[$0]);
break;
case 192:this.$ = new yy.Op($$[$0-1], $$[$0-2], $$[$0]);
break;
case 193:this.$ = new yy.Op($$[$0-1], $$[$0-2], $$[$0]);
break;
case 194:this.$ = new yy.Op($$[$0-1], $$[$0-2], $$[$0]);
break;
case 195:this.$ = new yy.Op($$[$0-1], $$[$0-2], $$[$0]);
break;
case 196:this.$ = (function () {
        if ($$[$0-1].charAt(0) === '!') {
          return new yy.Op($$[$0-1].slice(1), $$[$0-2], $$[$0]).invert();
        } else {
          return new yy.Op($$[$0-1], $$[$0-2], $$[$0]);
        }
      }());
break;
case 197:this.$ = new yy.Assign($$[$0-2], $$[$0], $$[$0-1]);
break;
case 198:this.$ = new yy.Assign($$[$0-4], $$[$0-1], $$[$0-3]);
break;
case 199:this.$ = new yy.Extends($$[$0-2], $$[$0]);
break;
}
},
table: [{1:[2,1],3:1,4:2,5:3,7:4,8:6,9:7,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,5],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[3]},{1:[2,2],6:[1,73]},{6:[1,74]},{1:[2,4],6:[2,4],27:[2,4],103:[2,4]},{4:76,7:4,8:6,9:7,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,27:[1,75],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,7],6:[2,7],27:[2,7],103:[2,7],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,8],6:[2,8],27:[2,8],103:[2,8],104:89,105:[1,64],107:[1,65],110:90,111:[1,67],112:68,127:[1,88]},{1:[2,13],6:[2,13],26:[2,13],27:[2,13],47:[2,13],52:[2,13],55:[2,13],60:92,64:[1,94],65:[1,95],66:[1,96],67:97,68:[1,98],70:[2,13],71:[1,99],72:[1,100],76:[2,13],79:91,82:[1,93],83:[2,103],84:[2,13],89:[2,13],91:[2,13],103:[2,13],105:[2,13],106:[2,13],107:[2,13],111:[2,13],119:[2,13],127:[2,13],129:[2,13],130:[2,13],133:[2,13],134:[2,13],135:[2,13],136:[2,13],137:[2,13],138:[2,13]},{1:[2,14],6:[2,14],26:[2,14],27:[2,14],47:[2,14],52:[2,14],55:[2,14],60:102,64:[1,94],65:[1,95],66:[1,96],67:97,68:[1,98],70:[2,14],71:[1,99],72:[1,100],76:[2,14],79:101,82:[1,93],83:[2,103],84:[2,14],89:[2,14],91:[2,14],103:[2,14],105:[2,14],106:[2,14],107:[2,14],111:[2,14],119:[2,14],127:[2,14],129:[2,14],130:[2,14],133:[2,14],134:[2,14],135:[2,14],136:[2,14],137:[2,14],138:[2,14]},{1:[2,15],6:[2,15],26:[2,15],27:[2,15],47:[2,15],52:[2,15],55:[2,15],70:[2,15],76:[2,15],84:[2,15],89:[2,15],91:[2,15],103:[2,15],105:[2,15],106:[2,15],107:[2,15],111:[2,15],119:[2,15],127:[2,15],129:[2,15],130:[2,15],133:[2,15],134:[2,15],135:[2,15],136:[2,15],137:[2,15],138:[2,15]},{1:[2,16],6:[2,16],26:[2,16],27:[2,16],47:[2,16],52:[2,16],55:[2,16],70:[2,16],76:[2,16],84:[2,16],89:[2,16],91:[2,16],103:[2,16],105:[2,16],106:[2,16],107:[2,16],111:[2,16],119:[2,16],127:[2,16],129:[2,16],130:[2,16],133:[2,16],134:[2,16],135:[2,16],136:[2,16],137:[2,16],138:[2,16]},{1:[2,17],6:[2,17],26:[2,17],27:[2,17],47:[2,17],52:[2,17],55:[2,17],70:[2,17],76:[2,17],84:[2,17],89:[2,17],91:[2,17],103:[2,17],105:[2,17],106:[2,17],107:[2,17],111:[2,17],119:[2,17],127:[2,17],129:[2,17],130:[2,17],133:[2,17],134:[2,17],135:[2,17],136:[2,17],137:[2,17],138:[2,17]},{1:[2,18],6:[2,18],26:[2,18],27:[2,18],47:[2,18],52:[2,18],55:[2,18],70:[2,18],76:[2,18],84:[2,18],89:[2,18],91:[2,18],103:[2,18],105:[2,18],106:[2,18],107:[2,18],111:[2,18],119:[2,18],127:[2,18],129:[2,18],130:[2,18],133:[2,18],134:[2,18],135:[2,18],136:[2,18],137:[2,18],138:[2,18]},{1:[2,19],6:[2,19],26:[2,19],27:[2,19],47:[2,19],52:[2,19],55:[2,19],70:[2,19],76:[2,19],84:[2,19],89:[2,19],91:[2,19],103:[2,19],105:[2,19],106:[2,19],107:[2,19],111:[2,19],119:[2,19],127:[2,19],129:[2,19],130:[2,19],133:[2,19],134:[2,19],135:[2,19],136:[2,19],137:[2,19],138:[2,19]},{1:[2,20],6:[2,20],26:[2,20],27:[2,20],47:[2,20],52:[2,20],55:[2,20],70:[2,20],76:[2,20],84:[2,20],89:[2,20],91:[2,20],103:[2,20],105:[2,20],106:[2,20],107:[2,20],111:[2,20],119:[2,20],127:[2,20],129:[2,20],130:[2,20],133:[2,20],134:[2,20],135:[2,20],136:[2,20],137:[2,20],138:[2,20]},{1:[2,21],6:[2,21],26:[2,21],27:[2,21],47:[2,21],52:[2,21],55:[2,21],70:[2,21],76:[2,21],84:[2,21],89:[2,21],91:[2,21],103:[2,21],105:[2,21],106:[2,21],107:[2,21],111:[2,21],119:[2,21],127:[2,21],129:[2,21],130:[2,21],133:[2,21],134:[2,21],135:[2,21],136:[2,21],137:[2,21],138:[2,21]},{1:[2,22],6:[2,22],26:[2,22],27:[2,22],47:[2,22],52:[2,22],55:[2,22],70:[2,22],76:[2,22],84:[2,22],89:[2,22],91:[2,22],103:[2,22],105:[2,22],106:[2,22],107:[2,22],111:[2,22],119:[2,22],127:[2,22],129:[2,22],130:[2,22],133:[2,22],134:[2,22],135:[2,22],136:[2,22],137:[2,22],138:[2,22]},{1:[2,23],6:[2,23],26:[2,23],27:[2,23],47:[2,23],52:[2,23],55:[2,23],70:[2,23],76:[2,23],84:[2,23],89:[2,23],91:[2,23],103:[2,23],105:[2,23],106:[2,23],107:[2,23],111:[2,23],119:[2,23],127:[2,23],129:[2,23],130:[2,23],133:[2,23],134:[2,23],135:[2,23],136:[2,23],137:[2,23],138:[2,23]},{1:[2,24],6:[2,24],26:[2,24],27:[2,24],47:[2,24],52:[2,24],55:[2,24],70:[2,24],76:[2,24],84:[2,24],89:[2,24],91:[2,24],103:[2,24],105:[2,24],106:[2,24],107:[2,24],111:[2,24],119:[2,24],127:[2,24],129:[2,24],130:[2,24],133:[2,24],134:[2,24],135:[2,24],136:[2,24],137:[2,24],138:[2,24]},{1:[2,9],6:[2,9],27:[2,9],103:[2,9],105:[2,9],107:[2,9],111:[2,9],127:[2,9]},{1:[2,10],6:[2,10],27:[2,10],103:[2,10],105:[2,10],107:[2,10],111:[2,10],127:[2,10]},{1:[2,11],6:[2,11],27:[2,11],103:[2,11],105:[2,11],107:[2,11],111:[2,11],127:[2,11]},{1:[2,12],6:[2,12],27:[2,12],103:[2,12],105:[2,12],107:[2,12],111:[2,12],127:[2,12]},{1:[2,70],6:[2,70],26:[2,70],27:[2,70],38:[1,103],47:[2,70],52:[2,70],55:[2,70],64:[2,70],65:[2,70],66:[2,70],68:[2,70],70:[2,70],71:[2,70],72:[2,70],76:[2,70],82:[2,70],83:[2,70],84:[2,70],89:[2,70],91:[2,70],103:[2,70],105:[2,70],106:[2,70],107:[2,70],111:[2,70],119:[2,70],127:[2,70],129:[2,70],130:[2,70],133:[2,70],134:[2,70],135:[2,70],136:[2,70],137:[2,70],138:[2,70]},{1:[2,71],6:[2,71],26:[2,71],27:[2,71],47:[2,71],52:[2,71],55:[2,71],64:[2,71],65:[2,71],66:[2,71],68:[2,71],70:[2,71],71:[2,71],72:[2,71],76:[2,71],82:[2,71],83:[2,71],84:[2,71],89:[2,71],91:[2,71],103:[2,71],105:[2,71],106:[2,71],107:[2,71],111:[2,71],119:[2,71],127:[2,71],129:[2,71],130:[2,71],133:[2,71],134:[2,71],135:[2,71],136:[2,71],137:[2,71],138:[2,71]},{1:[2,72],6:[2,72],26:[2,72],27:[2,72],47:[2,72],52:[2,72],55:[2,72],64:[2,72],65:[2,72],66:[2,72],68:[2,72],70:[2,72],71:[2,72],72:[2,72],76:[2,72],82:[2,72],83:[2,72],84:[2,72],89:[2,72],91:[2,72],103:[2,72],105:[2,72],106:[2,72],107:[2,72],111:[2,72],119:[2,72],127:[2,72],129:[2,72],130:[2,72],133:[2,72],134:[2,72],135:[2,72],136:[2,72],137:[2,72],138:[2,72]},{1:[2,73],6:[2,73],26:[2,73],27:[2,73],47:[2,73],52:[2,73],55:[2,73],64:[2,73],65:[2,73],66:[2,73],68:[2,73],70:[2,73],71:[2,73],72:[2,73],76:[2,73],82:[2,73],83:[2,73],84:[2,73],89:[2,73],91:[2,73],103:[2,73],105:[2,73],106:[2,73],107:[2,73],111:[2,73],119:[2,73],127:[2,73],129:[2,73],130:[2,73],133:[2,73],134:[2,73],135:[2,73],136:[2,73],137:[2,73],138:[2,73]},{1:[2,74],6:[2,74],26:[2,74],27:[2,74],47:[2,74],52:[2,74],55:[2,74],64:[2,74],65:[2,74],66:[2,74],68:[2,74],70:[2,74],71:[2,74],72:[2,74],76:[2,74],82:[2,74],83:[2,74],84:[2,74],89:[2,74],91:[2,74],103:[2,74],105:[2,74],106:[2,74],107:[2,74],111:[2,74],119:[2,74],127:[2,74],129:[2,74],130:[2,74],133:[2,74],134:[2,74],135:[2,74],136:[2,74],137:[2,74],138:[2,74]},{1:[2,101],6:[2,101],26:[2,101],27:[2,101],47:[2,101],52:[2,101],55:[2,101],64:[2,101],65:[2,101],66:[2,101],68:[2,101],70:[2,101],71:[2,101],72:[2,101],76:[2,101],80:104,82:[2,101],83:[1,105],84:[2,101],89:[2,101],91:[2,101],103:[2,101],105:[2,101],106:[2,101],107:[2,101],111:[2,101],119:[2,101],127:[2,101],129:[2,101],130:[2,101],133:[2,101],134:[2,101],135:[2,101],136:[2,101],137:[2,101],138:[2,101]},{28:109,29:[1,72],42:110,46:106,47:[2,52],52:[2,52],53:107,54:108,56:111,57:112,74:[1,69],87:[1,113],88:[1,114]},{5:115,26:[1,5]},{8:116,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:118,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:119,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{14:121,15:122,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:123,42:62,56:49,57:50,59:120,61:26,62:27,63:28,74:[1,69],81:[1,29],86:[1,57],87:[1,58],88:[1,56],102:[1,55]},{14:121,15:122,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:123,42:62,56:49,57:50,59:124,61:26,62:27,63:28,74:[1,69],81:[1,29],86:[1,57],87:[1,58],88:[1,56],102:[1,55]},{1:[2,67],6:[2,67],26:[2,67],27:[2,67],38:[2,67],47:[2,67],52:[2,67],55:[2,67],64:[2,67],65:[2,67],66:[2,67],68:[2,67],70:[2,67],71:[2,67],72:[2,67],76:[2,67],78:[1,128],82:[2,67],83:[2,67],84:[2,67],89:[2,67],91:[2,67],103:[2,67],105:[2,67],106:[2,67],107:[2,67],111:[2,67],119:[2,67],127:[2,67],129:[2,67],130:[2,67],131:[1,125],132:[1,126],133:[2,67],134:[2,67],135:[2,67],136:[2,67],137:[2,67],138:[2,67],139:[1,127]},{1:[2,178],6:[2,178],26:[2,178],27:[2,178],47:[2,178],52:[2,178],55:[2,178],70:[2,178],76:[2,178],84:[2,178],89:[2,178],91:[2,178],103:[2,178],105:[2,178],106:[2,178],107:[2,178],111:[2,178],119:[2,178],122:[1,129],127:[2,178],129:[2,178],130:[2,178],133:[2,178],134:[2,178],135:[2,178],136:[2,178],137:[2,178],138:[2,178]},{5:130,26:[1,5]},{29:[1,132],100:131},{5:133,26:[1,5]},{1:[2,146],6:[2,146],26:[2,146],27:[2,146],47:[2,146],52:[2,146],55:[2,146],70:[2,146],76:[2,146],84:[2,146],89:[2,146],91:[2,146],103:[2,146],105:[2,146],106:[2,146],107:[2,146],111:[2,146],119:[2,146],127:[2,146],129:[2,146],130:[2,146],133:[2,146],134:[2,146],135:[2,146],136:[2,146],137:[2,146],138:[2,146]},{5:134,26:[1,5]},{8:135,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,136],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,91],5:137,6:[2,91],14:121,15:122,26:[1,5],27:[2,91],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:123,42:62,47:[2,91],52:[2,91],55:[2,91],56:49,57:50,59:139,61:26,62:27,63:28,70:[2,91],74:[1,69],76:[2,91],78:[1,138],81:[1,29],84:[2,91],86:[1,57],87:[1,58],88:[1,56],89:[2,91],91:[2,91],102:[1,55],103:[2,91],105:[2,91],106:[2,91],107:[2,91],111:[2,91],119:[2,91],127:[2,91],129:[2,91],130:[2,91],133:[2,91],134:[2,91],135:[2,91],136:[2,91],137:[2,91],138:[2,91]},{1:[2,44],6:[2,44],8:140,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,27:[2,44],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],103:[2,44],104:41,105:[2,44],107:[2,44],108:42,109:[1,66],110:43,111:[2,44],112:68,120:[1,44],125:38,126:[1,63],127:[2,44],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:141,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,45],6:[2,45],26:[2,45],27:[2,45],52:[2,45],76:[2,45],103:[2,45],105:[2,45],107:[2,45],111:[2,45],127:[2,45]},{1:[2,68],6:[2,68],26:[2,68],27:[2,68],38:[2,68],47:[2,68],52:[2,68],55:[2,68],64:[2,68],65:[2,68],66:[2,68],68:[2,68],70:[2,68],71:[2,68],72:[2,68],76:[2,68],82:[2,68],83:[2,68],84:[2,68],89:[2,68],91:[2,68],103:[2,68],105:[2,68],106:[2,68],107:[2,68],111:[2,68],119:[2,68],127:[2,68],129:[2,68],130:[2,68],133:[2,68],134:[2,68],135:[2,68],136:[2,68],137:[2,68],138:[2,68]},{1:[2,69],6:[2,69],26:[2,69],27:[2,69],38:[2,69],47:[2,69],52:[2,69],55:[2,69],64:[2,69],65:[2,69],66:[2,69],68:[2,69],70:[2,69],71:[2,69],72:[2,69],76:[2,69],82:[2,69],83:[2,69],84:[2,69],89:[2,69],91:[2,69],103:[2,69],105:[2,69],106:[2,69],107:[2,69],111:[2,69],119:[2,69],127:[2,69],129:[2,69],130:[2,69],133:[2,69],134:[2,69],135:[2,69],136:[2,69],137:[2,69],138:[2,69]},{1:[2,30],6:[2,30],26:[2,30],27:[2,30],47:[2,30],52:[2,30],55:[2,30],64:[2,30],65:[2,30],66:[2,30],68:[2,30],70:[2,30],71:[2,30],72:[2,30],76:[2,30],82:[2,30],83:[2,30],84:[2,30],89:[2,30],91:[2,30],103:[2,30],105:[2,30],106:[2,30],107:[2,30],111:[2,30],119:[2,30],127:[2,30],129:[2,30],130:[2,30],133:[2,30],134:[2,30],135:[2,30],136:[2,30],137:[2,30],138:[2,30]},{1:[2,31],6:[2,31],26:[2,31],27:[2,31],47:[2,31],52:[2,31],55:[2,31],64:[2,31],65:[2,31],66:[2,31],68:[2,31],70:[2,31],71:[2,31],72:[2,31],76:[2,31],82:[2,31],83:[2,31],84:[2,31],89:[2,31],91:[2,31],103:[2,31],105:[2,31],106:[2,31],107:[2,31],111:[2,31],119:[2,31],127:[2,31],129:[2,31],130:[2,31],133:[2,31],134:[2,31],135:[2,31],136:[2,31],137:[2,31],138:[2,31]},{1:[2,32],6:[2,32],26:[2,32],27:[2,32],47:[2,32],52:[2,32],55:[2,32],64:[2,32],65:[2,32],66:[2,32],68:[2,32],70:[2,32],71:[2,32],72:[2,32],76:[2,32],82:[2,32],83:[2,32],84:[2,32],89:[2,32],91:[2,32],103:[2,32],105:[2,32],106:[2,32],107:[2,32],111:[2,32],119:[2,32],127:[2,32],129:[2,32],130:[2,32],133:[2,32],134:[2,32],135:[2,32],136:[2,32],137:[2,32],138:[2,32]},{1:[2,33],6:[2,33],26:[2,33],27:[2,33],47:[2,33],52:[2,33],55:[2,33],64:[2,33],65:[2,33],66:[2,33],68:[2,33],70:[2,33],71:[2,33],72:[2,33],76:[2,33],82:[2,33],83:[2,33],84:[2,33],89:[2,33],91:[2,33],103:[2,33],105:[2,33],106:[2,33],107:[2,33],111:[2,33],119:[2,33],127:[2,33],129:[2,33],130:[2,33],133:[2,33],134:[2,33],135:[2,33],136:[2,33],137:[2,33],138:[2,33]},{4:142,7:4,8:6,9:7,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,143],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:144,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,148],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,58:149,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],85:146,86:[1,57],87:[1,58],88:[1,56],89:[1,145],92:147,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,107],6:[2,107],26:[2,107],27:[2,107],47:[2,107],52:[2,107],55:[2,107],64:[2,107],65:[2,107],66:[2,107],68:[2,107],70:[2,107],71:[2,107],72:[2,107],76:[2,107],82:[2,107],83:[2,107],84:[2,107],89:[2,107],91:[2,107],103:[2,107],105:[2,107],106:[2,107],107:[2,107],111:[2,107],119:[2,107],127:[2,107],129:[2,107],130:[2,107],133:[2,107],134:[2,107],135:[2,107],136:[2,107],137:[2,107],138:[2,107]},{1:[2,108],6:[2,108],26:[2,108],27:[2,108],28:150,29:[1,72],47:[2,108],52:[2,108],55:[2,108],64:[2,108],65:[2,108],66:[2,108],68:[2,108],70:[2,108],71:[2,108],72:[2,108],76:[2,108],82:[2,108],83:[2,108],84:[2,108],89:[2,108],91:[2,108],103:[2,108],105:[2,108],106:[2,108],107:[2,108],111:[2,108],119:[2,108],127:[2,108],129:[2,108],130:[2,108],133:[2,108],134:[2,108],135:[2,108],136:[2,108],137:[2,108],138:[2,108]},{26:[2,48]},{26:[2,49]},{1:[2,63],6:[2,63],26:[2,63],27:[2,63],38:[2,63],47:[2,63],52:[2,63],55:[2,63],64:[2,63],65:[2,63],66:[2,63],68:[2,63],70:[2,63],71:[2,63],72:[2,63],76:[2,63],78:[2,63],82:[2,63],83:[2,63],84:[2,63],89:[2,63],91:[2,63],103:[2,63],105:[2,63],106:[2,63],107:[2,63],111:[2,63],119:[2,63],127:[2,63],129:[2,63],130:[2,63],131:[2,63],132:[2,63],133:[2,63],134:[2,63],135:[2,63],136:[2,63],137:[2,63],138:[2,63],139:[2,63]},{1:[2,66],6:[2,66],26:[2,66],27:[2,66],38:[2,66],47:[2,66],52:[2,66],55:[2,66],64:[2,66],65:[2,66],66:[2,66],68:[2,66],70:[2,66],71:[2,66],72:[2,66],76:[2,66],78:[2,66],82:[2,66],83:[2,66],84:[2,66],89:[2,66],91:[2,66],103:[2,66],105:[2,66],106:[2,66],107:[2,66],111:[2,66],119:[2,66],127:[2,66],129:[2,66],130:[2,66],131:[2,66],132:[2,66],133:[2,66],134:[2,66],135:[2,66],136:[2,66],137:[2,66],138:[2,66],139:[2,66]},{8:151,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:152,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:153,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{5:154,8:155,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,5],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{28:160,29:[1,72],56:161,57:162,62:156,74:[1,69],88:[1,56],114:157,115:[1,158],116:159},{113:163,117:[1,164],118:[1,165]},{6:[2,86],12:169,26:[2,86],28:170,29:[1,72],30:171,31:[1,70],32:[1,71],39:167,40:168,42:172,44:[1,48],52:[2,86],75:166,76:[2,86],87:[1,113]},{1:[2,28],6:[2,28],26:[2,28],27:[2,28],41:[2,28],47:[2,28],52:[2,28],55:[2,28],64:[2,28],65:[2,28],66:[2,28],68:[2,28],70:[2,28],71:[2,28],72:[2,28],76:[2,28],82:[2,28],83:[2,28],84:[2,28],89:[2,28],91:[2,28],103:[2,28],105:[2,28],106:[2,28],107:[2,28],111:[2,28],119:[2,28],127:[2,28],129:[2,28],130:[2,28],133:[2,28],134:[2,28],135:[2,28],136:[2,28],137:[2,28],138:[2,28]},{1:[2,29],6:[2,29],26:[2,29],27:[2,29],41:[2,29],47:[2,29],52:[2,29],55:[2,29],64:[2,29],65:[2,29],66:[2,29],68:[2,29],70:[2,29],71:[2,29],72:[2,29],76:[2,29],82:[2,29],83:[2,29],84:[2,29],89:[2,29],91:[2,29],103:[2,29],105:[2,29],106:[2,29],107:[2,29],111:[2,29],119:[2,29],127:[2,29],129:[2,29],130:[2,29],133:[2,29],134:[2,29],135:[2,29],136:[2,29],137:[2,29],138:[2,29]},{1:[2,27],6:[2,27],26:[2,27],27:[2,27],38:[2,27],41:[2,27],47:[2,27],52:[2,27],55:[2,27],64:[2,27],65:[2,27],66:[2,27],68:[2,27],70:[2,27],71:[2,27],72:[2,27],76:[2,27],78:[2,27],82:[2,27],83:[2,27],84:[2,27],89:[2,27],91:[2,27],103:[2,27],105:[2,27],106:[2,27],107:[2,27],111:[2,27],117:[2,27],118:[2,27],119:[2,27],127:[2,27],129:[2,27],130:[2,27],131:[2,27],132:[2,27],133:[2,27],134:[2,27],135:[2,27],136:[2,27],137:[2,27],138:[2,27],139:[2,27]},{1:[2,6],6:[2,6],7:173,8:6,9:7,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,27:[2,6],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],103:[2,6],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,3]},{1:[2,25],6:[2,25],26:[2,25],27:[2,25],47:[2,25],52:[2,25],55:[2,25],70:[2,25],76:[2,25],84:[2,25],89:[2,25],91:[2,25],96:[2,25],97:[2,25],103:[2,25],105:[2,25],106:[2,25],107:[2,25],111:[2,25],119:[2,25],122:[2,25],124:[2,25],127:[2,25],129:[2,25],130:[2,25],133:[2,25],134:[2,25],135:[2,25],136:[2,25],137:[2,25],138:[2,25]},{6:[1,73],27:[1,174]},{1:[2,189],6:[2,189],26:[2,189],27:[2,189],47:[2,189],52:[2,189],55:[2,189],70:[2,189],76:[2,189],84:[2,189],89:[2,189],91:[2,189],103:[2,189],105:[2,189],106:[2,189],107:[2,189],111:[2,189],119:[2,189],127:[2,189],129:[2,189],130:[2,189],133:[2,189],134:[2,189],135:[2,189],136:[2,189],137:[2,189],138:[2,189]},{8:175,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:176,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:177,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:178,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:179,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:180,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:181,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:182,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,145],6:[2,145],26:[2,145],27:[2,145],47:[2,145],52:[2,145],55:[2,145],70:[2,145],76:[2,145],84:[2,145],89:[2,145],91:[2,145],103:[2,145],105:[2,145],106:[2,145],107:[2,145],111:[2,145],119:[2,145],127:[2,145],129:[2,145],130:[2,145],133:[2,145],134:[2,145],135:[2,145],136:[2,145],137:[2,145],138:[2,145]},{1:[2,150],6:[2,150],26:[2,150],27:[2,150],47:[2,150],52:[2,150],55:[2,150],70:[2,150],76:[2,150],84:[2,150],89:[2,150],91:[2,150],103:[2,150],105:[2,150],106:[2,150],107:[2,150],111:[2,150],119:[2,150],127:[2,150],129:[2,150],130:[2,150],133:[2,150],134:[2,150],135:[2,150],136:[2,150],137:[2,150],138:[2,150]},{8:183,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,144],6:[2,144],26:[2,144],27:[2,144],47:[2,144],52:[2,144],55:[2,144],70:[2,144],76:[2,144],84:[2,144],89:[2,144],91:[2,144],103:[2,144],105:[2,144],106:[2,144],107:[2,144],111:[2,144],119:[2,144],127:[2,144],129:[2,144],130:[2,144],133:[2,144],134:[2,144],135:[2,144],136:[2,144],137:[2,144],138:[2,144]},{1:[2,149],6:[2,149],26:[2,149],27:[2,149],47:[2,149],52:[2,149],55:[2,149],70:[2,149],76:[2,149],84:[2,149],89:[2,149],91:[2,149],103:[2,149],105:[2,149],106:[2,149],107:[2,149],111:[2,149],119:[2,149],127:[2,149],129:[2,149],130:[2,149],133:[2,149],134:[2,149],135:[2,149],136:[2,149],137:[2,149],138:[2,149]},{80:184,83:[1,105]},{1:[2,64],6:[2,64],26:[2,64],27:[2,64],38:[2,64],47:[2,64],52:[2,64],55:[2,64],64:[2,64],65:[2,64],66:[2,64],68:[2,64],70:[2,64],71:[2,64],72:[2,64],76:[2,64],78:[2,64],82:[2,64],83:[2,64],84:[2,64],89:[2,64],91:[2,64],103:[2,64],105:[2,64],106:[2,64],107:[2,64],111:[2,64],119:[2,64],127:[2,64],129:[2,64],130:[2,64],131:[2,64],132:[2,64],133:[2,64],134:[2,64],135:[2,64],136:[2,64],137:[2,64],138:[2,64],139:[2,64]},{83:[2,104]},{28:185,29:[1,72]},{28:186,29:[1,72]},{1:[2,78],6:[2,78],26:[2,78],27:[2,78],28:187,29:[1,72],38:[2,78],47:[2,78],52:[2,78],55:[2,78],64:[2,78],65:[2,78],66:[2,78],68:[2,78],70:[2,78],71:[2,78],72:[2,78],76:[2,78],78:[2,78],82:[2,78],83:[2,78],84:[2,78],89:[2,78],91:[2,78],103:[2,78],105:[2,78],106:[2,78],107:[2,78],111:[2,78],119:[2,78],127:[2,78],129:[2,78],130:[2,78],131:[2,78],132:[2,78],133:[2,78],134:[2,78],135:[2,78],136:[2,78],137:[2,78],138:[2,78],139:[2,78]},{1:[2,79],6:[2,79],26:[2,79],27:[2,79],38:[2,79],47:[2,79],52:[2,79],55:[2,79],64:[2,79],65:[2,79],66:[2,79],68:[2,79],70:[2,79],71:[2,79],72:[2,79],76:[2,79],78:[2,79],82:[2,79],83:[2,79],84:[2,79],89:[2,79],91:[2,79],103:[2,79],105:[2,79],106:[2,79],107:[2,79],111:[2,79],119:[2,79],127:[2,79],129:[2,79],130:[2,79],131:[2,79],132:[2,79],133:[2,79],134:[2,79],135:[2,79],136:[2,79],137:[2,79],138:[2,79],139:[2,79]},{8:189,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],55:[1,193],56:49,57:50,59:37,61:26,62:27,63:28,69:188,73:190,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],90:191,91:[1,192],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{67:194,68:[1,98],71:[1,99],72:[1,100]},{67:195,68:[1,98],71:[1,99],72:[1,100]},{80:196,83:[1,105]},{1:[2,65],6:[2,65],26:[2,65],27:[2,65],38:[2,65],47:[2,65],52:[2,65],55:[2,65],64:[2,65],65:[2,65],66:[2,65],68:[2,65],70:[2,65],71:[2,65],72:[2,65],76:[2,65],78:[2,65],82:[2,65],83:[2,65],84:[2,65],89:[2,65],91:[2,65],103:[2,65],105:[2,65],106:[2,65],107:[2,65],111:[2,65],119:[2,65],127:[2,65],129:[2,65],130:[2,65],131:[2,65],132:[2,65],133:[2,65],134:[2,65],135:[2,65],136:[2,65],137:[2,65],138:[2,65],139:[2,65]},{8:197,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,198],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,102],6:[2,102],26:[2,102],27:[2,102],47:[2,102],52:[2,102],55:[2,102],64:[2,102],65:[2,102],66:[2,102],68:[2,102],70:[2,102],71:[2,102],72:[2,102],76:[2,102],82:[2,102],83:[2,102],84:[2,102],89:[2,102],91:[2,102],103:[2,102],105:[2,102],106:[2,102],107:[2,102],111:[2,102],119:[2,102],127:[2,102],129:[2,102],130:[2,102],133:[2,102],134:[2,102],135:[2,102],136:[2,102],137:[2,102],138:[2,102]},{8:201,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,148],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,58:149,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],84:[1,199],85:200,86:[1,57],87:[1,58],88:[1,56],92:147,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{47:[1,202],52:[1,203]},{47:[2,53],52:[2,53]},{38:[1,205],47:[2,55],52:[2,55],55:[1,204]},{38:[2,58],47:[2,58],52:[2,58],55:[2,58]},{38:[2,59],47:[2,59],52:[2,59],55:[2,59]},{38:[2,60],47:[2,60],52:[2,60],55:[2,60]},{38:[2,61],47:[2,61],52:[2,61],55:[2,61]},{28:150,29:[1,72]},{8:201,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,148],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,58:149,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],85:146,86:[1,57],87:[1,58],88:[1,56],89:[1,145],92:147,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,47],6:[2,47],26:[2,47],27:[2,47],47:[2,47],52:[2,47],55:[2,47],70:[2,47],76:[2,47],84:[2,47],89:[2,47],91:[2,47],103:[2,47],105:[2,47],106:[2,47],107:[2,47],111:[2,47],119:[2,47],127:[2,47],129:[2,47],130:[2,47],133:[2,47],134:[2,47],135:[2,47],136:[2,47],137:[2,47],138:[2,47]},{1:[2,182],6:[2,182],26:[2,182],27:[2,182],47:[2,182],52:[2,182],55:[2,182],70:[2,182],76:[2,182],84:[2,182],89:[2,182],91:[2,182],103:[2,182],104:86,105:[2,182],106:[2,182],107:[2,182],110:87,111:[2,182],112:68,119:[2,182],127:[2,182],129:[2,182],130:[2,182],133:[1,77],134:[2,182],135:[2,182],136:[2,182],137:[2,182],138:[2,182]},{104:89,105:[1,64],107:[1,65],110:90,111:[1,67],112:68,127:[1,88]},{1:[2,183],6:[2,183],26:[2,183],27:[2,183],47:[2,183],52:[2,183],55:[2,183],70:[2,183],76:[2,183],84:[2,183],89:[2,183],91:[2,183],103:[2,183],104:86,105:[2,183],106:[2,183],107:[2,183],110:87,111:[2,183],112:68,119:[2,183],127:[2,183],129:[2,183],130:[2,183],133:[1,77],134:[2,183],135:[2,183],136:[2,183],137:[2,183],138:[2,183]},{1:[2,184],6:[2,184],26:[2,184],27:[2,184],47:[2,184],52:[2,184],55:[2,184],70:[2,184],76:[2,184],84:[2,184],89:[2,184],91:[2,184],103:[2,184],104:86,105:[2,184],106:[2,184],107:[2,184],110:87,111:[2,184],112:68,119:[2,184],127:[2,184],129:[2,184],130:[2,184],133:[1,77],134:[2,184],135:[2,184],136:[2,184],137:[2,184],138:[2,184]},{1:[2,185],6:[2,185],26:[2,185],27:[2,185],47:[2,185],52:[2,185],55:[2,185],64:[2,67],65:[2,67],66:[2,67],68:[2,67],70:[2,185],71:[2,67],72:[2,67],76:[2,185],82:[2,67],83:[2,67],84:[2,185],89:[2,185],91:[2,185],103:[2,185],105:[2,185],106:[2,185],107:[2,185],111:[2,185],119:[2,185],127:[2,185],129:[2,185],130:[2,185],133:[2,185],134:[2,185],135:[2,185],136:[2,185],137:[2,185],138:[2,185]},{60:92,64:[1,94],65:[1,95],66:[1,96],67:97,68:[1,98],71:[1,99],72:[1,100],79:91,82:[1,93],83:[2,103]},{60:102,64:[1,94],65:[1,95],66:[1,96],67:97,68:[1,98],71:[1,99],72:[1,100],79:101,82:[1,93],83:[2,103]},{1:[2,70],6:[2,70],26:[2,70],27:[2,70],47:[2,70],52:[2,70],55:[2,70],64:[2,70],65:[2,70],66:[2,70],68:[2,70],70:[2,70],71:[2,70],72:[2,70],76:[2,70],82:[2,70],83:[2,70],84:[2,70],89:[2,70],91:[2,70],103:[2,70],105:[2,70],106:[2,70],107:[2,70],111:[2,70],119:[2,70],127:[2,70],129:[2,70],130:[2,70],133:[2,70],134:[2,70],135:[2,70],136:[2,70],137:[2,70],138:[2,70]},{1:[2,186],6:[2,186],26:[2,186],27:[2,186],47:[2,186],52:[2,186],55:[2,186],64:[2,67],65:[2,67],66:[2,67],68:[2,67],70:[2,186],71:[2,67],72:[2,67],76:[2,186],82:[2,67],83:[2,67],84:[2,186],89:[2,186],91:[2,186],103:[2,186],105:[2,186],106:[2,186],107:[2,186],111:[2,186],119:[2,186],127:[2,186],129:[2,186],130:[2,186],133:[2,186],134:[2,186],135:[2,186],136:[2,186],137:[2,186],138:[2,186]},{1:[2,187],6:[2,187],26:[2,187],27:[2,187],47:[2,187],52:[2,187],55:[2,187],70:[2,187],76:[2,187],84:[2,187],89:[2,187],91:[2,187],103:[2,187],105:[2,187],106:[2,187],107:[2,187],111:[2,187],119:[2,187],127:[2,187],129:[2,187],130:[2,187],133:[2,187],134:[2,187],135:[2,187],136:[2,187],137:[2,187],138:[2,187]},{1:[2,188],6:[2,188],26:[2,188],27:[2,188],47:[2,188],52:[2,188],55:[2,188],70:[2,188],76:[2,188],84:[2,188],89:[2,188],91:[2,188],103:[2,188],105:[2,188],106:[2,188],107:[2,188],111:[2,188],119:[2,188],127:[2,188],129:[2,188],130:[2,188],133:[2,188],134:[2,188],135:[2,188],136:[2,188],137:[2,188],138:[2,188]},{8:206,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,207],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:208,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{5:209,26:[1,5],126:[1,210]},{1:[2,127],6:[2,127],26:[2,127],27:[2,127],47:[2,127],52:[2,127],55:[2,127],70:[2,127],76:[2,127],84:[2,127],89:[2,127],91:[2,127],95:211,96:[1,212],97:[1,213],103:[2,127],105:[2,127],106:[2,127],107:[2,127],111:[2,127],119:[2,127],127:[2,127],129:[2,127],130:[2,127],133:[2,127],134:[2,127],135:[2,127],136:[2,127],137:[2,127],138:[2,127]},{1:[2,133],6:[2,133],26:[2,133],27:[2,133],47:[2,133],52:[2,133],55:[2,133],64:[1,215],70:[2,133],76:[2,133],84:[2,133],89:[2,133],91:[2,133],101:[1,214],103:[2,133],105:[2,133],106:[2,133],107:[2,133],111:[2,133],119:[2,133],127:[2,133],129:[2,133],130:[2,133],133:[2,133],134:[2,133],135:[2,133],136:[2,133],137:[2,133],138:[2,133]},{1:[2,135],6:[2,135],26:[2,135],27:[2,135],47:[2,135],52:[2,135],55:[2,135],64:[2,135],70:[2,135],76:[2,135],84:[2,135],89:[2,135],91:[2,135],101:[2,135],103:[2,135],105:[2,135],106:[2,135],107:[2,135],111:[2,135],119:[2,135],127:[2,135],129:[2,135],130:[2,135],133:[2,135],134:[2,135],135:[2,135],136:[2,135],137:[2,135],138:[2,135]},{1:[2,143],6:[2,143],26:[2,143],27:[2,143],47:[2,143],52:[2,143],55:[2,143],70:[2,143],76:[2,143],84:[2,143],89:[2,143],91:[2,143],103:[2,143],105:[2,143],106:[2,143],107:[2,143],111:[2,143],119:[2,143],127:[2,143],129:[2,143],130:[2,143],133:[2,143],134:[2,143],135:[2,143],136:[2,143],137:[2,143],138:[2,143]},{1:[2,151],6:[2,151],26:[2,151],27:[2,151],47:[2,151],52:[2,151],55:[2,151],70:[2,151],76:[2,151],84:[2,151],89:[2,151],91:[2,151],103:[2,151],105:[2,151],106:[2,151],107:[2,151],111:[2,151],119:[2,151],127:[2,151],129:[2,151],130:[2,151],133:[2,151],134:[2,151],135:[2,151],136:[2,151],137:[2,151],138:[2,151]},{26:[1,216],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{121:217,123:218,124:[1,219]},{1:[2,92],6:[2,92],26:[2,92],27:[2,92],47:[2,92],52:[2,92],55:[2,92],70:[2,92],76:[2,92],84:[2,92],89:[2,92],91:[2,92],103:[2,92],105:[2,92],106:[2,92],107:[2,92],111:[2,92],119:[2,92],127:[2,92],129:[2,92],130:[2,92],133:[2,92],134:[2,92],135:[2,92],136:[2,92],137:[2,92],138:[2,92]},{14:220,15:122,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:123,42:62,56:49,57:50,59:221,61:26,62:27,63:28,74:[1,69],81:[1,29],86:[1,57],87:[1,58],88:[1,56],102:[1,55]},{1:[2,95],5:222,6:[2,95],26:[1,5],27:[2,95],47:[2,95],52:[2,95],55:[2,95],64:[2,67],65:[2,67],66:[2,67],68:[2,67],70:[2,95],71:[2,67],72:[2,67],76:[2,95],78:[1,223],82:[2,67],83:[2,67],84:[2,95],89:[2,95],91:[2,95],103:[2,95],105:[2,95],106:[2,95],107:[2,95],111:[2,95],119:[2,95],127:[2,95],129:[2,95],130:[2,95],133:[2,95],134:[2,95],135:[2,95],136:[2,95],137:[2,95],138:[2,95]},{1:[2,43],6:[2,43],27:[2,43],103:[2,43],104:86,105:[2,43],107:[2,43],110:87,111:[2,43],112:68,127:[2,43],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,132],6:[2,132],27:[2,132],103:[2,132],104:86,105:[2,132],107:[2,132],110:87,111:[2,132],112:68,127:[2,132],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{6:[1,73],103:[1,224]},{4:225,7:4,8:6,9:7,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{6:[2,123],26:[2,123],52:[2,123],55:[1,227],89:[2,123],90:226,91:[1,192],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,110],6:[2,110],26:[2,110],27:[2,110],38:[2,110],47:[2,110],52:[2,110],55:[2,110],64:[2,110],65:[2,110],66:[2,110],68:[2,110],70:[2,110],71:[2,110],72:[2,110],76:[2,110],82:[2,110],83:[2,110],84:[2,110],89:[2,110],91:[2,110],103:[2,110],105:[2,110],106:[2,110],107:[2,110],111:[2,110],117:[2,110],118:[2,110],119:[2,110],127:[2,110],129:[2,110],130:[2,110],133:[2,110],134:[2,110],135:[2,110],136:[2,110],137:[2,110],138:[2,110]},{6:[2,50],26:[2,50],51:228,52:[1,229],89:[2,50]},{6:[2,118],26:[2,118],27:[2,118],52:[2,118],84:[2,118],89:[2,118]},{8:201,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,148],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,58:149,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],85:230,86:[1,57],87:[1,58],88:[1,56],92:147,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{6:[2,124],26:[2,124],27:[2,124],52:[2,124],84:[2,124],89:[2,124]},{1:[2,109],6:[2,109],26:[2,109],27:[2,109],38:[2,109],41:[2,109],47:[2,109],52:[2,109],55:[2,109],64:[2,109],65:[2,109],66:[2,109],68:[2,109],70:[2,109],71:[2,109],72:[2,109],76:[2,109],78:[2,109],82:[2,109],83:[2,109],84:[2,109],89:[2,109],91:[2,109],103:[2,109],105:[2,109],106:[2,109],107:[2,109],111:[2,109],119:[2,109],127:[2,109],129:[2,109],130:[2,109],131:[2,109],132:[2,109],133:[2,109],134:[2,109],135:[2,109],136:[2,109],137:[2,109],138:[2,109],139:[2,109]},{5:231,26:[1,5],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,139],6:[2,139],26:[2,139],27:[2,139],47:[2,139],52:[2,139],55:[2,139],70:[2,139],76:[2,139],84:[2,139],89:[2,139],91:[2,139],103:[2,139],104:86,105:[1,64],106:[1,232],107:[1,65],110:87,111:[1,67],112:68,119:[2,139],127:[2,139],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,141],6:[2,141],26:[2,141],27:[2,141],47:[2,141],52:[2,141],55:[2,141],70:[2,141],76:[2,141],84:[2,141],89:[2,141],91:[2,141],103:[2,141],104:86,105:[1,64],106:[1,233],107:[1,65],110:87,111:[1,67],112:68,119:[2,141],127:[2,141],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,147],6:[2,147],26:[2,147],27:[2,147],47:[2,147],52:[2,147],55:[2,147],70:[2,147],76:[2,147],84:[2,147],89:[2,147],91:[2,147],103:[2,147],105:[2,147],106:[2,147],107:[2,147],111:[2,147],119:[2,147],127:[2,147],129:[2,147],130:[2,147],133:[2,147],134:[2,147],135:[2,147],136:[2,147],137:[2,147],138:[2,147]},{1:[2,148],6:[2,148],26:[2,148],27:[2,148],47:[2,148],52:[2,148],55:[2,148],70:[2,148],76:[2,148],84:[2,148],89:[2,148],91:[2,148],103:[2,148],104:86,105:[1,64],106:[2,148],107:[1,65],110:87,111:[1,67],112:68,119:[2,148],127:[2,148],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,152],6:[2,152],26:[2,152],27:[2,152],47:[2,152],52:[2,152],55:[2,152],70:[2,152],76:[2,152],84:[2,152],89:[2,152],91:[2,152],103:[2,152],105:[2,152],106:[2,152],107:[2,152],111:[2,152],119:[2,152],127:[2,152],129:[2,152],130:[2,152],133:[2,152],134:[2,152],135:[2,152],136:[2,152],137:[2,152],138:[2,152]},{117:[2,154],118:[2,154]},{28:160,29:[1,72],56:161,57:162,74:[1,69],88:[1,114],114:234,116:159},{52:[1,235],117:[2,159],118:[2,159]},{52:[2,156],117:[2,156],118:[2,156]},{52:[2,157],117:[2,157],118:[2,157]},{52:[2,158],117:[2,158],118:[2,158]},{1:[2,153],6:[2,153],26:[2,153],27:[2,153],47:[2,153],52:[2,153],55:[2,153],70:[2,153],76:[2,153],84:[2,153],89:[2,153],91:[2,153],103:[2,153],105:[2,153],106:[2,153],107:[2,153],111:[2,153],119:[2,153],127:[2,153],129:[2,153],130:[2,153],133:[2,153],134:[2,153],135:[2,153],136:[2,153],137:[2,153],138:[2,153]},{8:236,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:237,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{6:[2,50],26:[2,50],51:238,52:[1,239],76:[2,50]},{6:[2,87],26:[2,87],27:[2,87],52:[2,87],76:[2,87]},{6:[2,36],26:[2,36],27:[2,36],41:[1,240],52:[2,36],76:[2,36]},{6:[2,39],26:[2,39],27:[2,39],52:[2,39],76:[2,39]},{6:[2,40],26:[2,40],27:[2,40],41:[2,40],52:[2,40],76:[2,40]},{6:[2,41],26:[2,41],27:[2,41],41:[2,41],52:[2,41],76:[2,41]},{6:[2,42],26:[2,42],27:[2,42],41:[2,42],52:[2,42],76:[2,42]},{1:[2,5],6:[2,5],27:[2,5],103:[2,5]},{1:[2,26],6:[2,26],26:[2,26],27:[2,26],47:[2,26],52:[2,26],55:[2,26],70:[2,26],76:[2,26],84:[2,26],89:[2,26],91:[2,26],96:[2,26],97:[2,26],103:[2,26],105:[2,26],106:[2,26],107:[2,26],111:[2,26],119:[2,26],122:[2,26],124:[2,26],127:[2,26],129:[2,26],130:[2,26],133:[2,26],134:[2,26],135:[2,26],136:[2,26],137:[2,26],138:[2,26]},{1:[2,190],6:[2,190],26:[2,190],27:[2,190],47:[2,190],52:[2,190],55:[2,190],70:[2,190],76:[2,190],84:[2,190],89:[2,190],91:[2,190],103:[2,190],104:86,105:[2,190],106:[2,190],107:[2,190],110:87,111:[2,190],112:68,119:[2,190],127:[2,190],129:[2,190],130:[2,190],133:[1,77],134:[1,80],135:[2,190],136:[2,190],137:[2,190],138:[2,190]},{1:[2,191],6:[2,191],26:[2,191],27:[2,191],47:[2,191],52:[2,191],55:[2,191],70:[2,191],76:[2,191],84:[2,191],89:[2,191],91:[2,191],103:[2,191],104:86,105:[2,191],106:[2,191],107:[2,191],110:87,111:[2,191],112:68,119:[2,191],127:[2,191],129:[2,191],130:[2,191],133:[1,77],134:[1,80],135:[2,191],136:[2,191],137:[2,191],138:[2,191]},{1:[2,192],6:[2,192],26:[2,192],27:[2,192],47:[2,192],52:[2,192],55:[2,192],70:[2,192],76:[2,192],84:[2,192],89:[2,192],91:[2,192],103:[2,192],104:86,105:[2,192],106:[2,192],107:[2,192],110:87,111:[2,192],112:68,119:[2,192],127:[2,192],129:[2,192],130:[2,192],133:[1,77],134:[2,192],135:[2,192],136:[2,192],137:[2,192],138:[2,192]},{1:[2,193],6:[2,193],26:[2,193],27:[2,193],47:[2,193],52:[2,193],55:[2,193],70:[2,193],76:[2,193],84:[2,193],89:[2,193],91:[2,193],103:[2,193],104:86,105:[2,193],106:[2,193],107:[2,193],110:87,111:[2,193],112:68,119:[2,193],127:[2,193],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[2,193],136:[2,193],137:[2,193],138:[2,193]},{1:[2,194],6:[2,194],26:[2,194],27:[2,194],47:[2,194],52:[2,194],55:[2,194],70:[2,194],76:[2,194],84:[2,194],89:[2,194],91:[2,194],103:[2,194],104:86,105:[2,194],106:[2,194],107:[2,194],110:87,111:[2,194],112:68,119:[2,194],127:[2,194],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[2,194],137:[2,194],138:[1,84]},{1:[2,195],6:[2,195],26:[2,195],27:[2,195],47:[2,195],52:[2,195],55:[2,195],70:[2,195],76:[2,195],84:[2,195],89:[2,195],91:[2,195],103:[2,195],104:86,105:[2,195],106:[2,195],107:[2,195],110:87,111:[2,195],112:68,119:[2,195],127:[2,195],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[2,195],138:[1,84]},{1:[2,196],6:[2,196],26:[2,196],27:[2,196],47:[2,196],52:[2,196],55:[2,196],70:[2,196],76:[2,196],84:[2,196],89:[2,196],91:[2,196],103:[2,196],104:86,105:[2,196],106:[2,196],107:[2,196],110:87,111:[2,196],112:68,119:[2,196],127:[2,196],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[2,196],137:[2,196],138:[2,196]},{1:[2,181],6:[2,181],26:[2,181],27:[2,181],47:[2,181],52:[2,181],55:[2,181],70:[2,181],76:[2,181],84:[2,181],89:[2,181],91:[2,181],103:[2,181],104:86,105:[1,64],106:[2,181],107:[1,65],110:87,111:[1,67],112:68,119:[2,181],127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,180],6:[2,180],26:[2,180],27:[2,180],47:[2,180],52:[2,180],55:[2,180],70:[2,180],76:[2,180],84:[2,180],89:[2,180],91:[2,180],103:[2,180],104:86,105:[1,64],106:[2,180],107:[1,65],110:87,111:[1,67],112:68,119:[2,180],127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,99],6:[2,99],26:[2,99],27:[2,99],47:[2,99],52:[2,99],55:[2,99],64:[2,99],65:[2,99],66:[2,99],68:[2,99],70:[2,99],71:[2,99],72:[2,99],76:[2,99],82:[2,99],83:[2,99],84:[2,99],89:[2,99],91:[2,99],103:[2,99],105:[2,99],106:[2,99],107:[2,99],111:[2,99],119:[2,99],127:[2,99],129:[2,99],130:[2,99],133:[2,99],134:[2,99],135:[2,99],136:[2,99],137:[2,99],138:[2,99]},{1:[2,75],6:[2,75],26:[2,75],27:[2,75],38:[2,75],47:[2,75],52:[2,75],55:[2,75],64:[2,75],65:[2,75],66:[2,75],68:[2,75],70:[2,75],71:[2,75],72:[2,75],76:[2,75],78:[2,75],82:[2,75],83:[2,75],84:[2,75],89:[2,75],91:[2,75],103:[2,75],105:[2,75],106:[2,75],107:[2,75],111:[2,75],119:[2,75],127:[2,75],129:[2,75],130:[2,75],131:[2,75],132:[2,75],133:[2,75],134:[2,75],135:[2,75],136:[2,75],137:[2,75],138:[2,75],139:[2,75]},{1:[2,76],6:[2,76],26:[2,76],27:[2,76],38:[2,76],47:[2,76],52:[2,76],55:[2,76],64:[2,76],65:[2,76],66:[2,76],68:[2,76],70:[2,76],71:[2,76],72:[2,76],76:[2,76],78:[2,76],82:[2,76],83:[2,76],84:[2,76],89:[2,76],91:[2,76],103:[2,76],105:[2,76],106:[2,76],107:[2,76],111:[2,76],119:[2,76],127:[2,76],129:[2,76],130:[2,76],131:[2,76],132:[2,76],133:[2,76],134:[2,76],135:[2,76],136:[2,76],137:[2,76],138:[2,76],139:[2,76]},{1:[2,77],6:[2,77],26:[2,77],27:[2,77],38:[2,77],47:[2,77],52:[2,77],55:[2,77],64:[2,77],65:[2,77],66:[2,77],68:[2,77],70:[2,77],71:[2,77],72:[2,77],76:[2,77],78:[2,77],82:[2,77],83:[2,77],84:[2,77],89:[2,77],91:[2,77],103:[2,77],105:[2,77],106:[2,77],107:[2,77],111:[2,77],119:[2,77],127:[2,77],129:[2,77],130:[2,77],131:[2,77],132:[2,77],133:[2,77],134:[2,77],135:[2,77],136:[2,77],137:[2,77],138:[2,77],139:[2,77]},{70:[1,241]},{55:[1,193],70:[2,83],90:242,91:[1,192],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{70:[2,84]},{8:243,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{13:[2,112],29:[2,112],31:[2,112],32:[2,112],34:[2,112],35:[2,112],36:[2,112],43:[2,112],44:[2,112],45:[2,112],49:[2,112],50:[2,112],70:[2,112],74:[2,112],77:[2,112],81:[2,112],86:[2,112],87:[2,112],88:[2,112],94:[2,112],98:[2,112],99:[2,112],102:[2,112],105:[2,112],107:[2,112],109:[2,112],111:[2,112],120:[2,112],126:[2,112],128:[2,112],129:[2,112],130:[2,112],131:[2,112],132:[2,112]},{13:[2,113],29:[2,113],31:[2,113],32:[2,113],34:[2,113],35:[2,113],36:[2,113],43:[2,113],44:[2,113],45:[2,113],49:[2,113],50:[2,113],70:[2,113],74:[2,113],77:[2,113],81:[2,113],86:[2,113],87:[2,113],88:[2,113],94:[2,113],98:[2,113],99:[2,113],102:[2,113],105:[2,113],107:[2,113],109:[2,113],111:[2,113],120:[2,113],126:[2,113],128:[2,113],129:[2,113],130:[2,113],131:[2,113],132:[2,113]},{1:[2,81],6:[2,81],26:[2,81],27:[2,81],38:[2,81],47:[2,81],52:[2,81],55:[2,81],64:[2,81],65:[2,81],66:[2,81],68:[2,81],70:[2,81],71:[2,81],72:[2,81],76:[2,81],78:[2,81],82:[2,81],83:[2,81],84:[2,81],89:[2,81],91:[2,81],103:[2,81],105:[2,81],106:[2,81],107:[2,81],111:[2,81],119:[2,81],127:[2,81],129:[2,81],130:[2,81],131:[2,81],132:[2,81],133:[2,81],134:[2,81],135:[2,81],136:[2,81],137:[2,81],138:[2,81],139:[2,81]},{1:[2,82],6:[2,82],26:[2,82],27:[2,82],38:[2,82],47:[2,82],52:[2,82],55:[2,82],64:[2,82],65:[2,82],66:[2,82],68:[2,82],70:[2,82],71:[2,82],72:[2,82],76:[2,82],78:[2,82],82:[2,82],83:[2,82],84:[2,82],89:[2,82],91:[2,82],103:[2,82],105:[2,82],106:[2,82],107:[2,82],111:[2,82],119:[2,82],127:[2,82],129:[2,82],130:[2,82],131:[2,82],132:[2,82],133:[2,82],134:[2,82],135:[2,82],136:[2,82],137:[2,82],138:[2,82],139:[2,82]},{1:[2,100],6:[2,100],26:[2,100],27:[2,100],47:[2,100],52:[2,100],55:[2,100],64:[2,100],65:[2,100],66:[2,100],68:[2,100],70:[2,100],71:[2,100],72:[2,100],76:[2,100],82:[2,100],83:[2,100],84:[2,100],89:[2,100],91:[2,100],103:[2,100],105:[2,100],106:[2,100],107:[2,100],111:[2,100],119:[2,100],127:[2,100],129:[2,100],130:[2,100],133:[2,100],134:[2,100],135:[2,100],136:[2,100],137:[2,100],138:[2,100]},{1:[2,34],6:[2,34],26:[2,34],27:[2,34],47:[2,34],52:[2,34],55:[2,34],70:[2,34],76:[2,34],84:[2,34],89:[2,34],91:[2,34],103:[2,34],104:86,105:[2,34],106:[2,34],107:[2,34],110:87,111:[2,34],112:68,119:[2,34],127:[2,34],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{8:244,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,105],6:[2,105],26:[2,105],27:[2,105],47:[2,105],52:[2,105],55:[2,105],64:[2,105],65:[2,105],66:[2,105],68:[2,105],70:[2,105],71:[2,105],72:[2,105],76:[2,105],82:[2,105],83:[2,105],84:[2,105],89:[2,105],91:[2,105],103:[2,105],105:[2,105],106:[2,105],107:[2,105],111:[2,105],119:[2,105],127:[2,105],129:[2,105],130:[2,105],133:[2,105],134:[2,105],135:[2,105],136:[2,105],137:[2,105],138:[2,105]},{6:[2,50],26:[2,50],51:245,52:[1,229],84:[2,50]},{6:[2,123],26:[2,123],27:[2,123],52:[2,123],55:[1,246],84:[2,123],89:[2,123],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{48:247,49:[1,59],50:[1,60]},{28:109,29:[1,72],42:110,53:248,54:108,56:111,57:112,74:[1,69],87:[1,113],88:[1,114]},{47:[2,56],52:[2,56]},{8:249,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,197],6:[2,197],26:[2,197],27:[2,197],47:[2,197],52:[2,197],55:[2,197],70:[2,197],76:[2,197],84:[2,197],89:[2,197],91:[2,197],103:[2,197],104:86,105:[2,197],106:[2,197],107:[2,197],110:87,111:[2,197],112:68,119:[2,197],127:[2,197],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{8:250,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,199],6:[2,199],26:[2,199],27:[2,199],47:[2,199],52:[2,199],55:[2,199],70:[2,199],76:[2,199],84:[2,199],89:[2,199],91:[2,199],103:[2,199],104:86,105:[2,199],106:[2,199],107:[2,199],110:87,111:[2,199],112:68,119:[2,199],127:[2,199],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,179],6:[2,179],26:[2,179],27:[2,179],47:[2,179],52:[2,179],55:[2,179],70:[2,179],76:[2,179],84:[2,179],89:[2,179],91:[2,179],103:[2,179],105:[2,179],106:[2,179],107:[2,179],111:[2,179],119:[2,179],127:[2,179],129:[2,179],130:[2,179],133:[2,179],134:[2,179],135:[2,179],136:[2,179],137:[2,179],138:[2,179]},{8:251,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,128],6:[2,128],26:[2,128],27:[2,128],47:[2,128],52:[2,128],55:[2,128],70:[2,128],76:[2,128],84:[2,128],89:[2,128],91:[2,128],96:[1,252],103:[2,128],105:[2,128],106:[2,128],107:[2,128],111:[2,128],119:[2,128],127:[2,128],129:[2,128],130:[2,128],133:[2,128],134:[2,128],135:[2,128],136:[2,128],137:[2,128],138:[2,128]},{5:253,26:[1,5]},{28:254,29:[1,72]},{29:[1,255]},{29:[1,256]},{121:257,123:218,124:[1,219]},{27:[1,258],122:[1,259],123:260,124:[1,219]},{27:[2,172],122:[2,172],124:[2,172]},{8:262,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],93:261,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,93],5:263,6:[2,93],26:[1,5],27:[2,93],47:[2,93],52:[2,93],55:[2,93],60:92,64:[1,94],65:[1,95],66:[1,96],67:97,68:[1,98],70:[2,93],71:[1,99],72:[1,100],76:[2,93],79:91,82:[1,93],83:[2,103],84:[2,93],89:[2,93],91:[2,93],103:[2,93],105:[2,93],106:[2,93],107:[2,93],111:[2,93],119:[2,93],127:[2,93],129:[2,93],130:[2,93],133:[2,93],134:[2,93],135:[2,93],136:[2,93],137:[2,93],138:[2,93]},{1:[2,67],6:[2,67],26:[2,67],27:[2,67],47:[2,67],52:[2,67],55:[2,67],64:[2,67],65:[2,67],66:[2,67],68:[2,67],70:[2,67],71:[2,67],72:[2,67],76:[2,67],82:[2,67],83:[2,67],84:[2,67],89:[2,67],91:[2,67],103:[2,67],105:[2,67],106:[2,67],107:[2,67],111:[2,67],119:[2,67],127:[2,67],129:[2,67],130:[2,67],133:[2,67],134:[2,67],135:[2,67],136:[2,67],137:[2,67],138:[2,67]},{1:[2,96],6:[2,96],26:[2,96],27:[2,96],47:[2,96],52:[2,96],55:[2,96],70:[2,96],76:[2,96],84:[2,96],89:[2,96],91:[2,96],103:[2,96],105:[2,96],106:[2,96],107:[2,96],111:[2,96],119:[2,96],127:[2,96],129:[2,96],130:[2,96],133:[2,96],134:[2,96],135:[2,96],136:[2,96],137:[2,96],138:[2,96]},{14:264,15:122,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:123,42:62,56:49,57:50,59:221,61:26,62:27,63:28,74:[1,69],81:[1,29],86:[1,57],87:[1,58],88:[1,56],102:[1,55]},{1:[2,137],6:[2,137],26:[2,137],27:[2,137],47:[2,137],52:[2,137],55:[2,137],64:[2,137],65:[2,137],66:[2,137],68:[2,137],70:[2,137],71:[2,137],72:[2,137],76:[2,137],82:[2,137],83:[2,137],84:[2,137],89:[2,137],91:[2,137],103:[2,137],105:[2,137],106:[2,137],107:[2,137],111:[2,137],119:[2,137],127:[2,137],129:[2,137],130:[2,137],133:[2,137],134:[2,137],135:[2,137],136:[2,137],137:[2,137],138:[2,137]},{6:[1,73],27:[1,265]},{8:266,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{6:[2,62],13:[2,113],26:[2,62],29:[2,113],31:[2,113],32:[2,113],34:[2,113],35:[2,113],36:[2,113],43:[2,113],44:[2,113],45:[2,113],49:[2,113],50:[2,113],52:[2,62],74:[2,113],77:[2,113],81:[2,113],86:[2,113],87:[2,113],88:[2,113],89:[2,62],94:[2,113],98:[2,113],99:[2,113],102:[2,113],105:[2,113],107:[2,113],109:[2,113],111:[2,113],120:[2,113],126:[2,113],128:[2,113],129:[2,113],130:[2,113],131:[2,113],132:[2,113]},{6:[1,268],26:[1,269],89:[1,267]},{6:[2,51],8:201,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[2,51],27:[2,51],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,58:149,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],84:[2,51],86:[1,57],87:[1,58],88:[1,56],89:[2,51],92:270,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{6:[2,50],26:[2,50],27:[2,50],51:271,52:[1,229]},{1:[2,176],6:[2,176],26:[2,176],27:[2,176],47:[2,176],52:[2,176],55:[2,176],70:[2,176],76:[2,176],84:[2,176],89:[2,176],91:[2,176],103:[2,176],105:[2,176],106:[2,176],107:[2,176],111:[2,176],119:[2,176],122:[2,176],127:[2,176],129:[2,176],130:[2,176],133:[2,176],134:[2,176],135:[2,176],136:[2,176],137:[2,176],138:[2,176]},{8:272,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:273,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{117:[2,155],118:[2,155]},{28:160,29:[1,72],56:161,57:162,74:[1,69],88:[1,114],116:274},{1:[2,161],6:[2,161],26:[2,161],27:[2,161],47:[2,161],52:[2,161],55:[2,161],70:[2,161],76:[2,161],84:[2,161],89:[2,161],91:[2,161],103:[2,161],104:86,105:[2,161],106:[1,275],107:[2,161],110:87,111:[2,161],112:68,119:[1,276],127:[2,161],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,162],6:[2,162],26:[2,162],27:[2,162],47:[2,162],52:[2,162],55:[2,162],70:[2,162],76:[2,162],84:[2,162],89:[2,162],91:[2,162],103:[2,162],104:86,105:[2,162],106:[1,277],107:[2,162],110:87,111:[2,162],112:68,119:[2,162],127:[2,162],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{6:[1,279],26:[1,280],76:[1,278]},{6:[2,51],12:169,26:[2,51],27:[2,51],28:170,29:[1,72],30:171,31:[1,70],32:[1,71],39:281,40:168,42:172,44:[1,48],76:[2,51],87:[1,113]},{8:282,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,283],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,80],6:[2,80],26:[2,80],27:[2,80],38:[2,80],47:[2,80],52:[2,80],55:[2,80],64:[2,80],65:[2,80],66:[2,80],68:[2,80],70:[2,80],71:[2,80],72:[2,80],76:[2,80],78:[2,80],82:[2,80],83:[2,80],84:[2,80],89:[2,80],91:[2,80],103:[2,80],105:[2,80],106:[2,80],107:[2,80],111:[2,80],119:[2,80],127:[2,80],129:[2,80],130:[2,80],131:[2,80],132:[2,80],133:[2,80],134:[2,80],135:[2,80],136:[2,80],137:[2,80],138:[2,80],139:[2,80]},{8:284,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,70:[2,116],74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{70:[2,117],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{27:[1,285],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{6:[1,268],26:[1,269],84:[1,286]},{6:[2,62],26:[2,62],27:[2,62],52:[2,62],84:[2,62],89:[2,62]},{5:287,26:[1,5]},{47:[2,54],52:[2,54]},{47:[2,57],52:[2,57],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{27:[1,288],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{5:289,26:[1,5],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{5:290,26:[1,5]},{1:[2,129],6:[2,129],26:[2,129],27:[2,129],47:[2,129],52:[2,129],55:[2,129],70:[2,129],76:[2,129],84:[2,129],89:[2,129],91:[2,129],103:[2,129],105:[2,129],106:[2,129],107:[2,129],111:[2,129],119:[2,129],127:[2,129],129:[2,129],130:[2,129],133:[2,129],134:[2,129],135:[2,129],136:[2,129],137:[2,129],138:[2,129]},{5:291,26:[1,5]},{1:[2,134],6:[2,134],26:[2,134],27:[2,134],47:[2,134],52:[2,134],55:[2,134],70:[2,134],76:[2,134],84:[2,134],89:[2,134],91:[2,134],103:[2,134],105:[2,134],106:[2,134],107:[2,134],111:[2,134],119:[2,134],127:[2,134],129:[2,134],130:[2,134],133:[2,134],134:[2,134],135:[2,134],136:[2,134],137:[2,134],138:[2,134]},{1:[2,136],6:[2,136],26:[2,136],27:[2,136],47:[2,136],52:[2,136],55:[2,136],64:[2,136],70:[2,136],76:[2,136],84:[2,136],89:[2,136],91:[2,136],101:[2,136],103:[2,136],105:[2,136],106:[2,136],107:[2,136],111:[2,136],119:[2,136],127:[2,136],129:[2,136],130:[2,136],133:[2,136],134:[2,136],135:[2,136],136:[2,136],137:[2,136],138:[2,136]},{27:[1,292],122:[1,293],123:260,124:[1,219]},{1:[2,170],6:[2,170],26:[2,170],27:[2,170],47:[2,170],52:[2,170],55:[2,170],70:[2,170],76:[2,170],84:[2,170],89:[2,170],91:[2,170],103:[2,170],105:[2,170],106:[2,170],107:[2,170],111:[2,170],119:[2,170],127:[2,170],129:[2,170],130:[2,170],133:[2,170],134:[2,170],135:[2,170],136:[2,170],137:[2,170],138:[2,170]},{5:294,26:[1,5]},{27:[2,173],122:[2,173],124:[2,173]},{5:295,26:[1,5],52:[1,296]},{26:[2,125],52:[2,125],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,94],6:[2,94],26:[2,94],27:[2,94],47:[2,94],52:[2,94],55:[2,94],70:[2,94],76:[2,94],84:[2,94],89:[2,94],91:[2,94],103:[2,94],105:[2,94],106:[2,94],107:[2,94],111:[2,94],119:[2,94],127:[2,94],129:[2,94],130:[2,94],133:[2,94],134:[2,94],135:[2,94],136:[2,94],137:[2,94],138:[2,94]},{1:[2,97],5:297,6:[2,97],26:[1,5],27:[2,97],47:[2,97],52:[2,97],55:[2,97],60:92,64:[1,94],65:[1,95],66:[1,96],67:97,68:[1,98],70:[2,97],71:[1,99],72:[1,100],76:[2,97],79:91,82:[1,93],83:[2,103],84:[2,97],89:[2,97],91:[2,97],103:[2,97],105:[2,97],106:[2,97],107:[2,97],111:[2,97],119:[2,97],127:[2,97],129:[2,97],130:[2,97],133:[2,97],134:[2,97],135:[2,97],136:[2,97],137:[2,97],138:[2,97]},{103:[1,298]},{89:[1,299],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,111],6:[2,111],26:[2,111],27:[2,111],38:[2,111],47:[2,111],52:[2,111],55:[2,111],64:[2,111],65:[2,111],66:[2,111],68:[2,111],70:[2,111],71:[2,111],72:[2,111],76:[2,111],82:[2,111],83:[2,111],84:[2,111],89:[2,111],91:[2,111],103:[2,111],105:[2,111],106:[2,111],107:[2,111],111:[2,111],117:[2,111],118:[2,111],119:[2,111],127:[2,111],129:[2,111],130:[2,111],133:[2,111],134:[2,111],135:[2,111],136:[2,111],137:[2,111],138:[2,111]},{8:201,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,58:149,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],92:300,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:201,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,26:[1,148],28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,58:149,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],85:301,86:[1,57],87:[1,58],88:[1,56],92:147,94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{6:[2,119],26:[2,119],27:[2,119],52:[2,119],84:[2,119],89:[2,119]},{6:[1,268],26:[1,269],27:[1,302]},{1:[2,140],6:[2,140],26:[2,140],27:[2,140],47:[2,140],52:[2,140],55:[2,140],70:[2,140],76:[2,140],84:[2,140],89:[2,140],91:[2,140],103:[2,140],104:86,105:[1,64],106:[2,140],107:[1,65],110:87,111:[1,67],112:68,119:[2,140],127:[2,140],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,142],6:[2,142],26:[2,142],27:[2,142],47:[2,142],52:[2,142],55:[2,142],70:[2,142],76:[2,142],84:[2,142],89:[2,142],91:[2,142],103:[2,142],104:86,105:[1,64],106:[2,142],107:[1,65],110:87,111:[1,67],112:68,119:[2,142],127:[2,142],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{117:[2,160],118:[2,160]},{8:303,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:304,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:305,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,85],6:[2,85],26:[2,85],27:[2,85],38:[2,85],47:[2,85],52:[2,85],55:[2,85],64:[2,85],65:[2,85],66:[2,85],68:[2,85],70:[2,85],71:[2,85],72:[2,85],76:[2,85],82:[2,85],83:[2,85],84:[2,85],89:[2,85],91:[2,85],103:[2,85],105:[2,85],106:[2,85],107:[2,85],111:[2,85],117:[2,85],118:[2,85],119:[2,85],127:[2,85],129:[2,85],130:[2,85],133:[2,85],134:[2,85],135:[2,85],136:[2,85],137:[2,85],138:[2,85]},{12:169,28:170,29:[1,72],30:171,31:[1,70],32:[1,71],39:306,40:168,42:172,44:[1,48],87:[1,113]},{6:[2,86],12:169,26:[2,86],27:[2,86],28:170,29:[1,72],30:171,31:[1,70],32:[1,71],39:167,40:168,42:172,44:[1,48],52:[2,86],75:307,87:[1,113]},{6:[2,88],26:[2,88],27:[2,88],52:[2,88],76:[2,88]},{6:[2,37],26:[2,37],27:[2,37],52:[2,37],76:[2,37],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{8:308,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{70:[2,115],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,35],6:[2,35],26:[2,35],27:[2,35],47:[2,35],52:[2,35],55:[2,35],70:[2,35],76:[2,35],84:[2,35],89:[2,35],91:[2,35],103:[2,35],105:[2,35],106:[2,35],107:[2,35],111:[2,35],119:[2,35],127:[2,35],129:[2,35],130:[2,35],133:[2,35],134:[2,35],135:[2,35],136:[2,35],137:[2,35],138:[2,35]},{1:[2,106],6:[2,106],26:[2,106],27:[2,106],47:[2,106],52:[2,106],55:[2,106],64:[2,106],65:[2,106],66:[2,106],68:[2,106],70:[2,106],71:[2,106],72:[2,106],76:[2,106],82:[2,106],83:[2,106],84:[2,106],89:[2,106],91:[2,106],103:[2,106],105:[2,106],106:[2,106],107:[2,106],111:[2,106],119:[2,106],127:[2,106],129:[2,106],130:[2,106],133:[2,106],134:[2,106],135:[2,106],136:[2,106],137:[2,106],138:[2,106]},{1:[2,46],6:[2,46],26:[2,46],27:[2,46],47:[2,46],52:[2,46],55:[2,46],70:[2,46],76:[2,46],84:[2,46],89:[2,46],91:[2,46],103:[2,46],105:[2,46],106:[2,46],107:[2,46],111:[2,46],119:[2,46],127:[2,46],129:[2,46],130:[2,46],133:[2,46],134:[2,46],135:[2,46],136:[2,46],137:[2,46],138:[2,46]},{1:[2,198],6:[2,198],26:[2,198],27:[2,198],47:[2,198],52:[2,198],55:[2,198],70:[2,198],76:[2,198],84:[2,198],89:[2,198],91:[2,198],103:[2,198],105:[2,198],106:[2,198],107:[2,198],111:[2,198],119:[2,198],127:[2,198],129:[2,198],130:[2,198],133:[2,198],134:[2,198],135:[2,198],136:[2,198],137:[2,198],138:[2,198]},{1:[2,177],6:[2,177],26:[2,177],27:[2,177],47:[2,177],52:[2,177],55:[2,177],70:[2,177],76:[2,177],84:[2,177],89:[2,177],91:[2,177],103:[2,177],105:[2,177],106:[2,177],107:[2,177],111:[2,177],119:[2,177],122:[2,177],127:[2,177],129:[2,177],130:[2,177],133:[2,177],134:[2,177],135:[2,177],136:[2,177],137:[2,177],138:[2,177]},{1:[2,130],6:[2,130],26:[2,130],27:[2,130],47:[2,130],52:[2,130],55:[2,130],70:[2,130],76:[2,130],84:[2,130],89:[2,130],91:[2,130],103:[2,130],105:[2,130],106:[2,130],107:[2,130],111:[2,130],119:[2,130],127:[2,130],129:[2,130],130:[2,130],133:[2,130],134:[2,130],135:[2,130],136:[2,130],137:[2,130],138:[2,130]},{1:[2,131],6:[2,131],26:[2,131],27:[2,131],47:[2,131],52:[2,131],55:[2,131],70:[2,131],76:[2,131],84:[2,131],89:[2,131],91:[2,131],96:[2,131],103:[2,131],105:[2,131],106:[2,131],107:[2,131],111:[2,131],119:[2,131],127:[2,131],129:[2,131],130:[2,131],133:[2,131],134:[2,131],135:[2,131],136:[2,131],137:[2,131],138:[2,131]},{1:[2,168],6:[2,168],26:[2,168],27:[2,168],47:[2,168],52:[2,168],55:[2,168],70:[2,168],76:[2,168],84:[2,168],89:[2,168],91:[2,168],103:[2,168],105:[2,168],106:[2,168],107:[2,168],111:[2,168],119:[2,168],127:[2,168],129:[2,168],130:[2,168],133:[2,168],134:[2,168],135:[2,168],136:[2,168],137:[2,168],138:[2,168]},{5:309,26:[1,5]},{27:[1,310]},{6:[1,311],27:[2,174],122:[2,174],124:[2,174]},{8:312,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{1:[2,98],6:[2,98],26:[2,98],27:[2,98],47:[2,98],52:[2,98],55:[2,98],70:[2,98],76:[2,98],84:[2,98],89:[2,98],91:[2,98],103:[2,98],105:[2,98],106:[2,98],107:[2,98],111:[2,98],119:[2,98],127:[2,98],129:[2,98],130:[2,98],133:[2,98],134:[2,98],135:[2,98],136:[2,98],137:[2,98],138:[2,98]},{1:[2,138],6:[2,138],26:[2,138],27:[2,138],47:[2,138],52:[2,138],55:[2,138],64:[2,138],65:[2,138],66:[2,138],68:[2,138],70:[2,138],71:[2,138],72:[2,138],76:[2,138],82:[2,138],83:[2,138],84:[2,138],89:[2,138],91:[2,138],103:[2,138],105:[2,138],106:[2,138],107:[2,138],111:[2,138],119:[2,138],127:[2,138],129:[2,138],130:[2,138],133:[2,138],134:[2,138],135:[2,138],136:[2,138],137:[2,138],138:[2,138]},{1:[2,114],6:[2,114],26:[2,114],27:[2,114],47:[2,114],52:[2,114],55:[2,114],64:[2,114],65:[2,114],66:[2,114],68:[2,114],70:[2,114],71:[2,114],72:[2,114],76:[2,114],82:[2,114],83:[2,114],84:[2,114],89:[2,114],91:[2,114],103:[2,114],105:[2,114],106:[2,114],107:[2,114],111:[2,114],119:[2,114],127:[2,114],129:[2,114],130:[2,114],133:[2,114],134:[2,114],135:[2,114],136:[2,114],137:[2,114],138:[2,114]},{6:[2,120],26:[2,120],27:[2,120],52:[2,120],84:[2,120],89:[2,120]},{6:[2,50],26:[2,50],27:[2,50],51:313,52:[1,229]},{6:[2,121],26:[2,121],27:[2,121],52:[2,121],84:[2,121],89:[2,121]},{1:[2,163],6:[2,163],26:[2,163],27:[2,163],47:[2,163],52:[2,163],55:[2,163],70:[2,163],76:[2,163],84:[2,163],89:[2,163],91:[2,163],103:[2,163],104:86,105:[2,163],106:[2,163],107:[2,163],110:87,111:[2,163],112:68,119:[1,314],127:[2,163],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,165],6:[2,165],26:[2,165],27:[2,165],47:[2,165],52:[2,165],55:[2,165],70:[2,165],76:[2,165],84:[2,165],89:[2,165],91:[2,165],103:[2,165],104:86,105:[2,165],106:[1,315],107:[2,165],110:87,111:[2,165],112:68,119:[2,165],127:[2,165],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,164],6:[2,164],26:[2,164],27:[2,164],47:[2,164],52:[2,164],55:[2,164],70:[2,164],76:[2,164],84:[2,164],89:[2,164],91:[2,164],103:[2,164],104:86,105:[2,164],106:[2,164],107:[2,164],110:87,111:[2,164],112:68,119:[2,164],127:[2,164],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{6:[2,89],26:[2,89],27:[2,89],52:[2,89],76:[2,89]},{6:[2,50],26:[2,50],27:[2,50],51:316,52:[1,239]},{27:[1,317],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{27:[1,318]},{1:[2,171],6:[2,171],26:[2,171],27:[2,171],47:[2,171],52:[2,171],55:[2,171],70:[2,171],76:[2,171],84:[2,171],89:[2,171],91:[2,171],103:[2,171],105:[2,171],106:[2,171],107:[2,171],111:[2,171],119:[2,171],127:[2,171],129:[2,171],130:[2,171],133:[2,171],134:[2,171],135:[2,171],136:[2,171],137:[2,171],138:[2,171]},{27:[2,175],122:[2,175],124:[2,175]},{26:[2,126],52:[2,126],104:86,105:[1,64],107:[1,65],110:87,111:[1,67],112:68,127:[1,85],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{6:[1,268],26:[1,269],27:[1,319]},{8:320,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{8:321,9:117,10:20,11:21,12:22,13:[1,23],14:8,15:9,16:10,17:11,18:12,19:13,20:14,21:15,22:16,23:17,24:18,25:19,28:61,29:[1,72],30:51,31:[1,70],32:[1,71],33:25,34:[1,52],35:[1,53],36:[1,54],37:24,42:62,43:[1,46],44:[1,48],45:[1,30],48:31,49:[1,59],50:[1,60],56:49,57:50,59:37,61:26,62:27,63:28,74:[1,69],77:[1,45],81:[1,29],86:[1,57],87:[1,58],88:[1,56],94:[1,39],98:[1,47],99:[1,40],102:[1,55],104:41,105:[1,64],107:[1,65],108:42,109:[1,66],110:43,111:[1,67],112:68,120:[1,44],125:38,126:[1,63],128:[1,32],129:[1,33],130:[1,34],131:[1,35],132:[1,36]},{6:[1,279],26:[1,280],27:[1,322]},{6:[2,38],26:[2,38],27:[2,38],52:[2,38],76:[2,38]},{1:[2,169],6:[2,169],26:[2,169],27:[2,169],47:[2,169],52:[2,169],55:[2,169],70:[2,169],76:[2,169],84:[2,169],89:[2,169],91:[2,169],103:[2,169],105:[2,169],106:[2,169],107:[2,169],111:[2,169],119:[2,169],127:[2,169],129:[2,169],130:[2,169],133:[2,169],134:[2,169],135:[2,169],136:[2,169],137:[2,169],138:[2,169]},{6:[2,122],26:[2,122],27:[2,122],52:[2,122],84:[2,122],89:[2,122]},{1:[2,166],6:[2,166],26:[2,166],27:[2,166],47:[2,166],52:[2,166],55:[2,166],70:[2,166],76:[2,166],84:[2,166],89:[2,166],91:[2,166],103:[2,166],104:86,105:[2,166],106:[2,166],107:[2,166],110:87,111:[2,166],112:68,119:[2,166],127:[2,166],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{1:[2,167],6:[2,167],26:[2,167],27:[2,167],47:[2,167],52:[2,167],55:[2,167],70:[2,167],76:[2,167],84:[2,167],89:[2,167],91:[2,167],103:[2,167],104:86,105:[2,167],106:[2,167],107:[2,167],110:87,111:[2,167],112:68,119:[2,167],127:[2,167],129:[1,79],130:[1,78],133:[1,77],134:[1,80],135:[1,81],136:[1,82],137:[1,83],138:[1,84]},{6:[2,90],26:[2,90],27:[2,90],52:[2,90],76:[2,90]}],
defaultActions: {59:[2,48],60:[2,49],74:[2,3],93:[2,104],190:[2,84]},
parseError: function parseError(str, hash) {
    throw new Error(str);
},
parse: function parse(input) {
    var self = this,
        stack = [0],
        vstack = [null], // semantic value stack
        lstack = [], // location stack
        table = this.table,
        yytext = '',
        yylineno = 0,
        yyleng = 0,
        recovering = 0,
        TERROR = 2,
        EOF = 1;

    //this.reductionCount = this.shiftCount = 0;

    this.lexer.setInput(input);
    this.lexer.yy = this.yy;
    this.yy.lexer = this.lexer;
    if (typeof this.lexer.yylloc == 'undefined')
        this.lexer.yylloc = {};
    var yyloc = this.lexer.yylloc;
    lstack.push(yyloc);

    if (typeof this.yy.parseError === 'function')
        this.parseError = this.yy.parseError;

    function popStack (n) {
        stack.length = stack.length - 2*n;
        vstack.length = vstack.length - n;
        lstack.length = lstack.length - n;
    }

    function lex() {
        var token;
        token = self.lexer.lex() || 1; // $end = 1
        // if token isn't its numeric value, convert
        if (typeof token !== 'number') {
            token = self.symbols_[token] || token;
        }
        return token;
    };

    var symbol, preErrorSymbol, state, action, a, r, yyval={},p,len,newState, expected;
    while (true) {
        // retreive state number from top of stack
        state = stack[stack.length-1];

        // use default actions if available
        if (this.defaultActions[state]) {
            action = this.defaultActions[state];
        } else {
            if (symbol == null)
                symbol = lex();
            // read action for current state and first input
            action = table[state] && table[state][symbol];
        }

        // handle parse error
        if (typeof action === 'undefined' || !action.length || !action[0]) {

            if (!recovering) {
                // Report error
                expected = [];
                for (p in table[state]) if (this.terminals_[p] && p > 2) {
                    expected.push("'"+this.terminals_[p]+"'");
                }
                var errStr = '';
                if (this.lexer.showPosition) {
                    errStr = 'Parse error on line '+(yylineno+1)+":\n"+this.lexer.showPosition()+'\nExpecting '+expected.join(', ');
                } else {
                    errStr = 'Parse error on line '+(yylineno+1)+": Unexpected " +
                                  (symbol == 1 /*EOF*/ ? "end of input" :
                                              ("'"+(this.terminals_[symbol] || symbol)+"'"));
                }
                this.parseError(errStr,
                    {text: this.lexer.match, token: this.terminals_[symbol] || symbol, line: this.lexer.yylineno, loc: yyloc, expected: expected});
            }

            // just recovered from another error
            if (recovering == 3) {
                if (symbol == EOF) {
                    throw new Error(errStr || 'Parsing halted.');
                }

                // discard current lookahead and grab another
                yyleng = this.lexer.yyleng;
                yytext = this.lexer.yytext;
                yylineno = this.lexer.yylineno;
                yyloc = this.lexer.yylloc;
                symbol = lex();
            }

            // try to recover from error
            while (1) {
                // check for error recovery rule in this state
                if ((TERROR.toString()) in table[state]) {
                    break;
                }
                if (state == 0) {
                    throw new Error(errStr || 'Parsing halted.');
                }
                popStack(1);
                state = stack[stack.length-1];
            }

            preErrorSymbol = symbol; // save the lookahead token
            symbol = TERROR;         // insert generic error symbol as new lookahead
            state = stack[stack.length-1];
            action = table[state] && table[state][TERROR];
            recovering = 3; // allow 3 real symbols to be shifted before reporting a new error
        }

        // this shouldn't happen, unless resolve defaults are off
        if (action[0] instanceof Array && action.length > 1) {
            throw new Error('Parse Error: multiple actions possible at state: '+state+', token: '+symbol);
        }

        switch (action[0]) {

            case 1: // shift
                //this.shiftCount++;

                stack.push(symbol);
                vstack.push(this.lexer.yytext);
                lstack.push(this.lexer.yylloc);
                stack.push(action[1]); // push state
                symbol = null;
                if (!preErrorSymbol) { // normal execution/no error
                    yyleng = this.lexer.yyleng;
                    yytext = this.lexer.yytext;
                    yylineno = this.lexer.yylineno;
                    yyloc = this.lexer.yylloc;
                    if (recovering > 0)
                        recovering--;
                } else { // error just occurred, resume old lookahead f/ before error
                    symbol = preErrorSymbol;
                    preErrorSymbol = null;
                }
                break;

            case 2: // reduce
                //this.reductionCount++;

                len = this.productions_[action[1]][1];

                // perform semantic action
                yyval.$ = vstack[vstack.length-len]; // default to $$ = $1
                // default location, uses first token for firsts, last for lasts
                yyval._$ = {
                    first_line: lstack[lstack.length-(len||1)].first_line,
                    last_line: lstack[lstack.length-1].last_line,
                    first_column: lstack[lstack.length-(len||1)].first_column,
                    last_column: lstack[lstack.length-1].last_column
                };
                r = this.performAction.call(yyval, yytext, yyleng, yylineno, this.yy, action[1], vstack, lstack);

                if (typeof r !== 'undefined') {
                    return r;
                }

                // pop off stack
                if (len) {
                    stack = stack.slice(0,-1*len*2);
                    vstack = vstack.slice(0, -1*len);
                    lstack = lstack.slice(0, -1*len);
                }

                stack.push(this.productions_[action[1]][0]);    // push nonterminal (reduce)
                vstack.push(yyval.$);
                lstack.push(yyval._$);
                // goto new state = table[STATE][NONTERMINAL]
                newState = table[stack[stack.length-2]][stack[stack.length-1]];
                stack.push(newState);
                break;

            case 3: // accept
                return true;
        }

    }

    return true;
}};
return parser;
})();
if (typeof require !== 'undefined' && typeof exports !== 'undefined') {
exports.parser = parser;
exports.parse = function () { return parser.parse.apply(parser, arguments); }
exports.main = function commonjsMain(args) {
    if (!args[1])
        throw new Error('Usage: '+args[0]+' FILE');
    if (typeof process !== 'undefined') {
        var source = require('fs').readFileSync(require('path').join(process.cwd(), args[1]), "utf8");
    } else {
        var cwd = require("file").path(require("file").cwd());
        var source = cwd.join(args[1]).read({charset: "utf-8"});
    }
    return exports.parser.parse(source);
}
if (typeof module !== 'undefined' && require.main === module) {
  exports.main(typeof process !== 'undefined' ? process.argv.slice(1) : require("system").args);
}
}
};require['./scope'] = new function() {
  var exports = this;
  (function() {
  var Scope, extend, last, _ref;
  _ref = require('./helpers'), extend = _ref.extend, last = _ref.last;
  exports.Scope = Scope = (function() {
    Scope.root = null;
    function Scope(parent, expressions, method) {
      this.parent = parent;
      this.expressions = expressions;
      this.method = method;
      this.variables = [
        {
          name: 'arguments',
          type: 'arguments'
        }
      ];
      this.positions = {};
      if (!this.parent) {
        Scope.root = this;
      }
    }
    Scope.prototype.add = function(name, type, immediate) {
      var pos;
      if (this.shared && !immediate) {
        return this.parent.add(name, type, immediate);
      }
      if (typeof (pos = this.positions[name]) === 'number') {
        return this.variables[pos].type = type;
      } else {
        return this.positions[name] = this.variables.push({
          name: name,
          type: type
        }) - 1;
      }
    };
    Scope.prototype.find = function(name, options) {
      if (this.check(name, options)) {
        return true;
      }
      this.add(name, 'var');
      return false;
    };
    Scope.prototype.parameter = function(name) {
      if (this.shared && this.parent.check(name, true)) {
        return;
      }
      return this.add(name, 'param');
    };
    Scope.prototype.check = function(name, immediate) {
      var found, _ref2;
      found = !!this.type(name);
      if (found || immediate) {
        return found;
      }
      return !!((_ref2 = this.parent) != null ? _ref2.check(name) : void 0);
    };
    Scope.prototype.temporary = function(name, index) {
      if (name.length > 1) {
        return '_' + name + (index > 1 ? index : '');
      } else {
        return '_' + (index + parseInt(name, 36)).toString(36).replace(/\d/g, 'a');
      }
    };
    Scope.prototype.type = function(name) {
      var v, _i, _len, _ref2;
      _ref2 = this.variables;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        v = _ref2[_i];
        if (v.name === name) {
          return v.type;
        }
      }
      return null;
    };
    Scope.prototype.freeVariable = function(type) {
      var index, temp;
      index = 0;
      while (this.check((temp = this.temporary(type, index)))) {
        index++;
      }
      this.add(temp, 'var', true);
      return temp;
    };
    Scope.prototype.assign = function(name, value) {
      this.add(name, {
        value: value,
        assigned: true
      });
      return this.hasAssignments = true;
    };
    Scope.prototype.hasDeclarations = function() {
      return !!this.declaredVariables().length;
    };
    Scope.prototype.declaredVariables = function() {
      var realVars, tempVars, v, _i, _len, _ref2;
      realVars = [];
      tempVars = [];
      _ref2 = this.variables;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        v = _ref2[_i];
        if (v.type === 'var') {
          (v.name.charAt(0) === '_' ? tempVars : realVars).push(v.name);
        }
      }
      return realVars.sort().concat(tempVars.sort());
    };
    Scope.prototype.assignedVariables = function() {
      var v, _i, _len, _ref2, _results;
      _ref2 = this.variables;
      _results = [];
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        v = _ref2[_i];
        if (v.type.assigned) {
          _results.push("" + v.name + " = " + v.type.value);
        }
      }
      return _results;
    };
    return Scope;
  })();
}).call(this);

};require['./nodes'] = new function() {
  var exports = this;
  (function() {
  var Access, Arr, Assign, Base, Block, Call, Class, Closure, Code, Comment, Existence, Extends, For, IDENTIFIER, IDENTIFIER_STR, IS_STRING, If, In, Include, Index, LEVEL_ACCESS, LEVEL_COND, LEVEL_LIST, LEVEL_OP, LEVEL_PAREN, LEVEL_TOP, Literal, METHOD_DEF, NEGATE, NO, Namespace, Obj, Op, Param, Parens, Push, Range, Return, SIMPLENUM, Scope, Slice, Splat, Switch, TAB, THIS, Throw, Try, UTILITIES, Value, While, YES, compact, del, ends, extend, flatten, last, merge, multident, starts, unfoldSoak, utility, _ref;
  var __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
    for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
    function ctor() { this.constructor = child; }
    ctor.prototype = parent.prototype;
    child.prototype = new ctor;
    child.__super__ = parent.prototype;
    return child;
  }, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __indexOf = Array.prototype.indexOf || function(item) {
    for (var i = 0, l = this.length; i < l; i++) {
      if (this[i] === item) return i;
    }
    return -1;
  };
  Scope = require('./scope').Scope;
  _ref = require('./helpers'), compact = _ref.compact, flatten = _ref.flatten, extend = _ref.extend, merge = _ref.merge, del = _ref.del, starts = _ref.starts, ends = _ref.ends, last = _ref.last;
  exports.extend = extend;
  YES = function() {
    return true;
  };
  NO = function() {
    return false;
  };
  THIS = function() {
    return this;
  };
  NEGATE = function() {
    this.negated = !this.negated;
    return this;
  };
  exports.Base = Base = (function() {
    function Base() {}
    Base.prototype.compile = function(o, lvl) {
      var node;
      o = extend({}, o);
      if (lvl) {
        o.level = lvl;
      }
      node = this.unfoldSoak(o) || this;
      node.tab = o.indent;
      if (o.level === LEVEL_TOP || !node.isStatement(o)) {
        return node.compileNode(o);
      } else {
        return node.compileClosure(o);
      }
    };
    Base.prototype.compileClosure = function(o) {
      if (this.jumps() || this instanceof Throw) {
        throw SyntaxError('cannot use a pure statement in an expression.');
      }
      o.sharedScope = true;
      return Closure.wrap(this).compileNode(o);
    };
    Base.prototype.cache = function(o, level, reused) {
      var ref, sub;
      if (!this.isComplex()) {
        ref = level ? this.compile(o, level) : this;
        return [ref, ref];
      } else {
        ref = new Literal(reused || o.scope.freeVariable('ref'));
        sub = new Assign(ref, this);
        if (level) {
          return [sub.compile(o, level), ref.value];
        } else {
          return [sub, ref];
        }
      }
    };
    Base.prototype.compileLoopReference = function(o, name) {
      var src, tmp;
      src = tmp = this.compile(o, LEVEL_LIST);
      if (!((-Infinity < +src && +src < Infinity) || IDENTIFIER.test(src) && o.scope.check(src, true))) {
        src = "" + (tmp = o.scope.freeVariable(name)) + " = " + src;
      }
      return [src, tmp];
    };
    Base.prototype.makeReturn = function() {
      return new Return(this);
    };
    Base.prototype.contains = function(pred) {
      var contains;
      contains = false;
      this.traverseChildren(false, function(node) {
        if (pred(node)) {
          contains = true;
          return false;
        }
      });
      return contains;
    };
    Base.prototype.containsType = function(type) {
      return this instanceof type || this.contains(function(node) {
        return node instanceof type;
      });
    };
    Base.prototype.lastNonComment = function(list) {
      var i;
      i = list.length;
      while (i--) {
        if (!(list[i] instanceof Comment)) {
          return list[i];
        }
      }
      return null;
    };
    Base.prototype.toString = function(idt, name) {
      var tree;
      if (idt == null) {
        idt = '';
      }
      if (name == null) {
        name = this.constructor.name;
      }
      tree = '\n' + idt + name;
      if (this.soak) {
        tree += '?';
      }
      this.eachChild(function(node) {
        return tree += node.toString(idt + TAB);
      });
      return tree;
    };
    Base.prototype.eachChild = function(func) {
      var attr, child, _i, _j, _len, _len2, _ref2, _ref3;
      if (!this.children) {
        return this;
      }
      _ref2 = this.children;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        attr = _ref2[_i];
        if (this[attr]) {
          _ref3 = flatten([this[attr]]);
          for (_j = 0, _len2 = _ref3.length; _j < _len2; _j++) {
            child = _ref3[_j];
            if (func(child) === false) {
              return this;
            }
          }
        }
      }
      return this;
    };
    Base.prototype.traverseChildren = function(crossScope, func) {
      return this.eachChild(function(child) {
        if (func(child) === false) {
          return false;
        }
        return child.traverseChildren(crossScope, func);
      });
    };
    Base.prototype.invert = function() {
      return new Op('!', this);
    };
    Base.prototype.unwrapAll = function() {
      var node;
      node = this;
      while (node !== (node = node.unwrap())) {
        continue;
      }
      return node;
    };
    Base.prototype.children = [];
    Base.prototype.isStatement = NO;
    Base.prototype.jumps = NO;
    Base.prototype.isComplex = YES;
    Base.prototype.isChainable = NO;
    Base.prototype.isAssignable = NO;
    Base.prototype.unwrap = THIS;
    Base.prototype.unfoldSoak = NO;
    Base.prototype.assigns = NO;
    return Base;
  })();
  exports.Block = Block = (function() {
    __extends(Block, Base);
    function Block(nodes) {
      this.expressions = compact(flatten(nodes || []));
    }
    Block.prototype.children = ['expressions'];
    Block.prototype.push = function(node) {
      this.expressions.push(node);
      return this;
    };
    Block.prototype.pop = function() {
      return this.expressions.pop();
    };
    Block.prototype.unshift = function(node) {
      this.expressions.unshift(node);
      return this;
    };
    Block.prototype.unwrap = function() {
      if (this.expressions.length === 1) {
        return this.expressions[0];
      } else {
        return this;
      }
    };
    Block.prototype.isEmpty = function() {
      return !this.expressions.length;
    };
    Block.prototype.isStatement = function(o) {
      var exp, _i, _len, _ref2;
      _ref2 = this.expressions;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        exp = _ref2[_i];
        if (exp.isStatement(o)) {
          return true;
        }
      }
      return false;
    };
    Block.prototype.jumps = function(o) {
      var exp, _i, _len, _ref2;
      _ref2 = this.expressions;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        exp = _ref2[_i];
        if (exp.jumps(o)) {
          return exp;
        }
      }
    };
    Block.prototype.makeReturn = function() {
      var expr, len;
      len = this.expressions.length;
      while (len--) {
        expr = this.expressions[len];
        if (!(expr instanceof Comment)) {
          this.expressions[len] = expr.makeReturn();
          if (expr instanceof Return && !expr.expression) {
            this.expressions.splice(len, 1);
          }
          break;
        }
      }
      return this;
    };
    Block.prototype.compile = function(o, level) {
      if (o == null) {
        o = {};
      }
      if (o.scope) {
        return Block.__super__.compile.call(this, o, level);
      } else {
        return this.compileRoot(o);
      }
    };
    Block.prototype.compileNode = function(o) {
      var code, codes, node, top, _i, _len, _ref2;
      this.tab = o.indent;
      top = o.level === LEVEL_TOP;
      codes = [];
      _ref2 = this.expressions;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        node = _ref2[_i];
        node = node.unwrapAll();
        node = node.unfoldSoak(o) || node;
        if (node instanceof Block) {
          codes.push(node.compileNode(o));
        } else if (top) {
          node.front = true;
          code = node.compile(o);
          codes.push(node.isStatement(o) ? code : this.tab + code + ';');
        } else {
          codes.push(node.compile(o, LEVEL_LIST));
        }
      }
      if (top) {
        return codes.join('\n');
      }
      code = codes.join(', ') || 'void 0';
      if (codes.length > 1 && o.level >= LEVEL_LIST) {
        return "(" + code + ")";
      } else {
        return code;
      }
    };
    Block.prototype.compileRoot = function(o) {
      var aliases, code, comparator, idt, inc, includes, includesJs, name, provides;
      o.indent = this.tab = o.bare ? '' : TAB;
      o.scope = new Scope(null, this, null);
      o.level = LEVEL_TOP;
      code = this.compileWithDeclarations(o);
      if (o.google) {
        provides = o.google.provides;
        provides.sort();
        provides = (function() {
          var _i, _len, _results;
          _results = [];
          for (_i = 0, _len = provides.length; _i < _len; _i++) {
            name = provides[_i];
            _results.push("goog.provide('" + name + "');");
          }
          return _results;
        })();
        provides = provides.join('\n');
        includes = o.google.includes;
        comparator = function(a, b) {
          return a.name.localeCompare(b.name);
        };
        includes.sort(comparator);
        includesJs = (function() {
          var _i, _len, _results;
          _results = [];
          for (_i = 0, _len = includes.length; _i < _len; _i++) {
            inc = includes[_i];
            _results.push("goog.require('" + inc.name + "');");
          }
          return _results;
        })();
        includesJs = includesJs.join('\n');
        aliases = (function() {
          var _i, _len, _results;
          _results = [];
          for (_i = 0, _len = includes.length; _i < _len; _i++) {
            inc = includes[_i];
            if (inc.alias) {
              _results.push(inc);
            }
          }
          return _results;
        })();
        aliases.sort(comparator);
        idt = this.tab + TAB;
        aliases = (function() {
          var _i, _len, _results;
          _results = [];
          for (_i = 0, _len = aliases.length; _i < _len; _i++) {
            inc = aliases[_i];
            _results.push("" + idt + "var " + inc.alias + " = " + inc.name + ";");
          }
          return _results;
        })();
        aliases = aliases.join('\n');
        code = "" + provides + "\n\n" + includesJs + "\n\ngoog.scope(function() {\n" + aliases + "\n" + code + "\n\n}); // close goog.scope()";
      }
      if (o.bare) {
        return code;
      } else {
        return "(function() {\n" + code + "\n}).call(this);\n";
      }
    };
    Block.prototype.compileWithDeclarations = function(o) {
      var assigns, code, declars, exp, i, post, rest, scope, _len, _ref2;
      code = post = '';
      _ref2 = this.expressions;
      for (i = 0, _len = _ref2.length; i < _len; i++) {
        exp = _ref2[i];
        exp = exp.unwrap();
        if (!(exp instanceof Comment || exp instanceof Literal)) {
          break;
        }
      }
      o = merge(o, {
        level: LEVEL_TOP
      });
      if (i) {
        rest = this.expressions.splice(i, this.expressions.length);
        code = this.compileNode(o);
        this.expressions = rest;
      }
      post = this.compileNode(o);
      scope = o.scope;
      if (scope.expressions === this) {
        declars = o.scope.hasDeclarations();
        assigns = scope.hasAssignments;
        if ((declars || assigns) && i) {
          code += '\n';
        }
        if (declars) {
          code += "" + this.tab + "var " + (scope.declaredVariables().join(', ')) + ";\n";
        }
        if (assigns) {
          code += "" + this.tab + "var " + (multident(scope.assignedVariables().join(', '), this.tab)) + ";\n";
        }
      }
      return code + post;
    };
    Block.wrap = function(nodes) {
      if (nodes.length === 1 && nodes[0] instanceof Block) {
        return nodes[0];
      }
      return new Block(nodes);
    };
    return Block;
  })();
  exports.Literal = Literal = (function() {
    __extends(Literal, Base);
    function Literal(value) {
      this.value = value;
    }
    Literal.prototype.makeReturn = function() {
      if (this.isStatement()) {
        return this;
      } else {
        return new Return(this);
      }
    };
    Literal.prototype.isAssignable = function() {
      return IDENTIFIER.test(this.value);
    };
    Literal.prototype.isStatement = function() {
      var _ref2;
      return (_ref2 = this.value) === 'break' || _ref2 === 'continue' || _ref2 === 'debugger';
    };
    Literal.prototype.isComplex = NO;
    Literal.prototype.assigns = function(name) {
      return name === this.value;
    };
    Literal.prototype.jumps = function(o) {
      if (!this.isStatement()) {
        return false;
      }
      if (!(o && (o.loop || o.block && (this.value !== 'continue')))) {
        return this;
      } else {
        return false;
      }
    };
    Literal.prototype.compileNode = function(o) {
      var code;
      code = this.isUndefined ? o.level >= LEVEL_ACCESS ? '(void 0)' : 'void 0' : this.value.reserved ? "\"" + this.value + "\"" : this.value;
      if (this.isStatement()) {
        return "" + this.tab + code + ";";
      } else {
        return code;
      }
    };
    Literal.prototype.toString = function() {
      return ' "' + this.value + '"';
    };
    return Literal;
  })();
  exports.Return = Return = (function() {
    __extends(Return, Base);
    function Return(expr) {
      if (expr && !expr.unwrap().isUndefined) {
        this.expression = expr;
      }
    }
    Return.prototype.children = ['expression'];
    Return.prototype.isStatement = YES;
    Return.prototype.makeReturn = THIS;
    Return.prototype.jumps = THIS;
    Return.prototype.compile = function(o, level) {
      var expr, _ref2;
      expr = (_ref2 = this.expression) != null ? _ref2.makeReturn() : void 0;
      if (expr && !(expr instanceof Return)) {
        return expr.compile(o, level);
      } else {
        return Return.__super__.compile.call(this, o, level);
      }
    };
    Return.prototype.compileNode = function(o) {
      return this.tab + ("return" + (this.expression ? ' ' + this.expression.compile(o, LEVEL_PAREN) : '') + ";");
    };
    return Return;
  })();
  exports.Value = Value = (function() {
    __extends(Value, Base);
    function Value(base, props, tag) {
      if (!props && base instanceof Value) {
        return base;
      }
      this.base = base;
      this.properties = props || [];
      if (tag) {
        this[tag] = true;
      }
      return this;
    }
    Value.prototype.children = ['base', 'properties'];
    Value.prototype.push = function(prop) {
      this.properties.push(prop);
      return this;
    };
    Value.prototype.hasProperties = function() {
      return !!this.properties.length;
    };
    Value.prototype.isArray = function() {
      return !this.properties.length && this.base instanceof Arr;
    };
    Value.prototype.isComplex = function() {
      return this.hasProperties() || this.base.isComplex();
    };
    Value.prototype.isAssignable = function() {
      return this.hasProperties() || this.base.isAssignable();
    };
    Value.prototype.isSimpleNumber = function() {
      return this.base instanceof Literal && SIMPLENUM.test(this.base.value);
    };
    Value.prototype.isAtomic = function() {
      var node, _i, _len, _ref2;
      _ref2 = this.properties.concat(this.base);
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        node = _ref2[_i];
        if (node.soak || node instanceof Call) {
          return false;
        }
      }
      return true;
    };
    Value.prototype.isStatement = function(o) {
      return !this.properties.length && this.base.isStatement(o);
    };
    Value.prototype.assigns = function(name) {
      return !this.properties.length && this.base.assigns(name);
    };
    Value.prototype.jumps = function(o) {
      return !this.properties.length && this.base.jumps(o);
    };
    Value.prototype.isObject = function(onlyGenerated) {
      if (this.properties.length) {
        return false;
      }
      return (this.base instanceof Obj) && (!onlyGenerated || this.base.generated);
    };
    Value.prototype.isSplice = function() {
      return last(this.properties) instanceof Slice;
    };
    Value.prototype.makeReturn = function() {
      if (this.properties.length) {
        return Value.__super__.makeReturn.call(this);
      } else {
        return this.base.makeReturn();
      }
    };
    Value.prototype.unwrap = function() {
      if (this.properties.length) {
        return this;
      } else {
        return this.base;
      }
    };
    Value.prototype.cacheReference = function(o) {
      var base, bref, name, nref;
      name = last(this.properties);
      if (this.properties.length < 2 && !this.base.isComplex() && !(name != null ? name.isComplex() : void 0)) {
        return [this, this];
      }
      base = new Value(this.base, this.properties.slice(0, -1));
      if (base.isComplex()) {
        bref = new Literal(o.scope.freeVariable('base'));
        base = new Value(new Parens(new Assign(bref, base)));
      }
      if (!name) {
        return [base, bref];
      }
      if (name.isComplex()) {
        nref = new Literal(o.scope.freeVariable('name'));
        name = new Index(new Assign(nref, name.index));
        nref = new Index(nref);
      }
      return [base.push(name), new Value(bref || base.base, [nref || name])];
    };
    Value.prototype.compileNode = function(o) {
      var code, prop, props, _i, _len;
      this.base.front = this.front;
      props = this.properties;
      code = this.base.compile(o, props.length ? LEVEL_ACCESS : null);
      if ((this.base instanceof Parens || props.length) && SIMPLENUM.test(code)) {
        code = "" + code + ".";
      }
      for (_i = 0, _len = props.length; _i < _len; _i++) {
        prop = props[_i];
        code += prop.compile(o);
      }
      return code;
    };
    Value.prototype.unfoldSoak = function(o) {
      var result;
      if (this.unfoldedSoak != null) {
        return this.unfoldedSoak;
      }
      result = __bind(function() {
        var fst, i, ifn, prop, ref, snd, _len, _ref2;
        if (ifn = this.base.unfoldSoak(o)) {
          Array.prototype.push.apply(ifn.body.properties, this.properties);
          return ifn;
        }
        _ref2 = this.properties;
        for (i = 0, _len = _ref2.length; i < _len; i++) {
          prop = _ref2[i];
          if (prop.soak) {
            prop.soak = false;
            fst = new Value(this.base, this.properties.slice(0, i));
            snd = new Value(this.base, this.properties.slice(i));
            if (fst.isComplex()) {
              ref = new Literal(o.scope.freeVariable('ref'));
              fst = new Parens(new Assign(ref, fst));
              snd.base = ref;
            }
            return new If(new Existence(fst), snd, {
              soak: true
            });
          }
        }
        return null;
      }, this)();
      return this.unfoldedSoak = result || false;
    };
    return Value;
  })();
  exports.Comment = Comment = (function() {
    __extends(Comment, Base);
    function Comment(comment) {
      this.comment = comment;
    }
    Comment.prototype.isStatement = YES;
    Comment.prototype.makeReturn = THIS;
    Comment.prototype.compileNode = function(o, level) {
      var code;
      code = '/*' + multident(this.comment, this.tab) + '*/';
      if ((level || o.level) === LEVEL_TOP) {
        code = o.indent + code;
      }
      return code;
    };
    return Comment;
  })();
  exports.Call = Call = (function() {
    __extends(Call, Base);
    function Call(variable, args, soak) {
      this.args = args != null ? args : [];
      this.soak = soak;
      this.isNew = false;
      this.isSuper = variable === 'super';
      this.variable = this.isSuper ? null : variable;
    }
    Call.prototype.children = ['variable', 'args'];
    Call.prototype.newInstance = function() {
      var base;
      base = this.variable.base || this.variable;
      if (base instanceof Call && !base.isNew) {
        base.newInstance();
      } else {
        this.isNew = true;
      }
      return this;
    };
    Call.prototype.superReference = function(o) {
      var method, name;
      method = o.scope.method;
      if (!method) {
        throw SyntaxError('cannot call super outside of a function.');
      }
      name = method.name;
      if (name == null) {
        throw SyntaxError('cannot call super on an anonymous function.');
      }
      if (o.google) {
        if (method.klass) {
          return (new Value(new Literal(method.klass), [new Access(new Literal("superClass_")), new Access(new Literal(name))])).compile(o);
        } else if (method.ctorParent) {
          return method.ctorParent.compile(o);
        } else {
          throw SyntaxError("super() called without a parent class");
        }
      } else {
        if (method.klass) {
          return (new Value(new Literal(method.klass), [new Access(new Literal("__super__")), new Access(new Literal(name))])).compile(o);
        } else {
          return "" + name + ".__super__.constructor";
        }
      }
    };
    Call.prototype.unfoldSoak = function(o) {
      var call, ifn, left, list, rite, _i, _len, _ref2, _ref3;
      if (this.soak) {
        if (this.variable) {
          if (ifn = unfoldSoak(o, this, 'variable')) {
            return ifn;
          }
          _ref2 = new Value(this.variable).cacheReference(o), left = _ref2[0], rite = _ref2[1];
        } else {
          left = new Literal(this.superReference(o));
          rite = new Value(left);
        }
        rite = new Call(rite, this.args);
        rite.isNew = this.isNew;
        left = new Literal("typeof " + (left.compile(o)) + " === \"function\"");
        return new If(left, new Value(rite), {
          soak: true
        });
      }
      call = this;
      list = [];
      while (true) {
        if (call.variable instanceof Call) {
          list.push(call);
          call = call.variable;
          continue;
        }
        if (!(call.variable instanceof Value)) {
          break;
        }
        list.push(call);
        if (!((call = call.variable.base) instanceof Call)) {
          break;
        }
      }
      _ref3 = list.reverse();
      for (_i = 0, _len = _ref3.length; _i < _len; _i++) {
        call = _ref3[_i];
        if (ifn) {
          if (call.variable instanceof Call) {
            call.variable = ifn;
          } else {
            call.variable.base = ifn;
          }
        }
        ifn = unfoldSoak(o, call, 'variable');
      }
      return ifn;
    };
    Call.prototype.filterImplicitObjects = function(list) {
      var node, nodes, obj, prop, properties, _i, _j, _len, _len2, _ref2;
      nodes = [];
      for (_i = 0, _len = list.length; _i < _len; _i++) {
        node = list[_i];
        if (!((typeof node.isObject === "function" ? node.isObject() : void 0) && node.base.generated)) {
          nodes.push(node);
          continue;
        }
        obj = null;
        _ref2 = node.base.properties;
        for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
          prop = _ref2[_j];
          if (prop instanceof Assign || prop instanceof Comment) {
            if (!obj) {
              nodes.push(obj = new Obj(properties = [], true));
            }
            properties.push(prop);
          } else {
            nodes.push(prop);
            obj = null;
          }
        }
      }
      return nodes;
    };
    Call.prototype.compileNode = function(o) {
      var arg, args, code, _ref2;
      if ((_ref2 = this.variable) != null) {
        _ref2.front = this.front;
      }
      if (code = Splat.compileSplattedArray(o, this.args, true)) {
        return this.compileSplat(o, code);
      }
      args = this.filterImplicitObjects(this.args);
      args = ((function() {
        var _i, _len, _results;
        _results = [];
        for (_i = 0, _len = args.length; _i < _len; _i++) {
          arg = args[_i];
          _results.push(arg.compile(o, LEVEL_LIST));
        }
        return _results;
      })()).join(', ');
      if (this.isSuper) {
        return this.superReference(o) + (".call(this" + (args && ', ' + args) + ")");
      } else {
        return (this.isNew ? 'new ' : '') + this.variable.compile(o, LEVEL_ACCESS) + ("(" + args + ")");
      }
    };
    Call.prototype.compileSuper = function(args, o) {
      return "" + (this.superReference(o)) + ".call(this" + (args.length ? ', ' : '') + args + ")";
    };
    Call.prototype.compileSplat = function(o, splatArgs) {
      var base, fun, idt, name, ref;
      if (this.isSuper) {
        return "" + (this.superReference(o)) + ".apply(this, " + splatArgs + ")";
      }
      if (this.isNew) {
        idt = this.tab + TAB;
        return "(function(func, args, ctor) {\n" + idt + "ctor.prototype = func.prototype;\n" + idt + "var child = new ctor, result = func.apply(child, args);\n" + idt + "return typeof result === \"object\" ? result : child;\n" + this.tab + "})(" + (this.variable.compile(o, LEVEL_LIST)) + ", " + splatArgs + ", function() {})";
      }
      base = new Value(this.variable);
      if ((name = base.properties.pop()) && base.isComplex()) {
        ref = o.scope.freeVariable('ref');
        fun = "(" + ref + " = " + (base.compile(o, LEVEL_LIST)) + ")" + (name.compile(o));
      } else {
        fun = base.compile(o, LEVEL_ACCESS);
        if (SIMPLENUM.test(fun)) {
          fun = "(" + fun + ")";
        }
        if (name) {
          ref = fun;
          fun += name.compile(o);
        } else {
          ref = 'null';
        }
      }
      return "" + fun + ".apply(" + ref + ", " + splatArgs + ")";
    };
    return Call;
  })();
  exports.Extends = Extends = (function() {
    __extends(Extends, Base);
    function Extends(child, parent) {
      this.child = child;
      this.parent = parent;
    }
    Extends.prototype.children = ['child', 'parent'];
    Extends.prototype.compile = function(o) {
      var inheritsFunction;
      if (!o.google) {
        utility('hasProp');
      }
      if (o.google) {
        inheritsFunction = new Value(new Literal('goog.inherits'));
      } else {
        inheritsFunction = new Value(new Literal(utility('extends')));
      }
      return new Call(inheritsFunction, [this.child, this.parent]).compile(o);
    };
    return Extends;
  })();
  exports.Access = Access = (function() {
    __extends(Access, Base);
    function Access(name, tag) {
      this.name = name;
      this.name.asKey = true;
      this.proto = tag === 'proto' ? '.prototype' : '';
      this.soak = tag === 'soak';
    }
    Access.prototype.children = ['name'];
    Access.prototype.compile = function(o) {
      var name;
      name = this.name.compile(o);
      return this.proto + (IDENTIFIER.test(name) ? "." + name : "[" + name + "]");
    };
    Access.prototype.isComplex = NO;
    return Access;
  })();
  exports.Index = Index = (function() {
    __extends(Index, Base);
    function Index(index) {
      this.index = index;
    }
    Index.prototype.children = ['index'];
    Index.prototype.compile = function(o) {
      return (this.proto ? '.prototype' : '') + ("[" + (this.index.compile(o, LEVEL_PAREN)) + "]");
    };
    Index.prototype.isComplex = function() {
      return this.index.isComplex();
    };
    return Index;
  })();
  exports.Range = Range = (function() {
    __extends(Range, Base);
    Range.prototype.children = ['from', 'to'];
    function Range(from, to, tag) {
      this.from = from;
      this.to = to;
      this.exclusive = tag === 'exclusive';
      this.equals = this.exclusive ? '' : '=';
    }
    Range.prototype.compileVariables = function(o) {
      var step, _ref2, _ref3, _ref4, _ref5;
      o = merge(o, {
        top: true
      });
      _ref2 = this.from.cache(o, LEVEL_LIST), this.fromC = _ref2[0], this.fromVar = _ref2[1];
      _ref3 = this.to.cache(o, LEVEL_LIST), this.toC = _ref3[0], this.toVar = _ref3[1];
      if (step = del(o, 'step')) {
        _ref4 = step.cache(o, LEVEL_LIST), this.step = _ref4[0], this.stepVar = _ref4[1];
      }
      _ref5 = [this.fromVar.match(SIMPLENUM), this.toVar.match(SIMPLENUM)], this.fromNum = _ref5[0], this.toNum = _ref5[1];
      if (this.stepVar) {
        return this.stepNum = this.stepVar.match(SIMPLENUM);
      }
    };
    Range.prototype.compileNode = function(o) {
      var cond, condPart, from, gt, idx, known, lt, stepPart, to, varPart, _ref2, _ref3;
      if (!this.fromVar) {
        this.compileVariables(o);
      }
      if (!o.index) {
        return this.compileArray(o);
      }
      known = this.fromNum && this.toNum;
      idx = del(o, 'index');
      varPart = "" + idx + " = " + this.fromC;
      if (this.toC !== this.toVar) {
        varPart += ", " + this.toC;
      }
      if (this.step !== this.stepVar) {
        varPart += ", " + this.step;
      }
      _ref2 = ["" + idx + " <" + this.equals, "" + idx + " >" + this.equals], lt = _ref2[0], gt = _ref2[1];
      condPart = this.stepNum ? condPart = +this.stepNum > 0 ? "" + lt + " " + this.toVar : "" + gt + " " + this.toVar : known ? ((_ref3 = [+this.fromNum, +this.toNum], from = _ref3[0], to = _ref3[1], _ref3), condPart = from <= to ? "" + lt + " " + to : "" + gt + " " + to) : (cond = "" + this.fromVar + " <= " + this.toVar, condPart = "" + cond + " ? " + lt + " " + this.toVar + " : " + gt + " " + this.toVar);
      stepPart = this.stepVar ? "" + idx + " += " + this.stepVar : known ? from <= to ? "" + idx + "++" : "" + idx + "--" : "" + cond + " ? " + idx + "++ : " + idx + "--";
      return "" + varPart + "; " + condPart + "; " + stepPart;
    };
    Range.prototype.compileArray = function(o) {
      var args, body, cond, hasArgs, i, idt, post, pre, range, result, vars, _i, _ref2, _ref3, _results;
      if (this.fromNum && this.toNum && Math.abs(this.fromNum - this.toNum) <= 20) {
        range = (function() {
          _results = [];
          for (var _i = _ref2 = +this.fromNum, _ref3 = +this.toNum; _ref2 <= _ref3 ? _i <= _ref3 : _i >= _ref3; _ref2 <= _ref3 ? _i++ : _i--){ _results.push(_i); }
          return _results;
        }).apply(this);
        if (this.exclusive) {
          range.pop();
        }
        return "[" + (range.join(', ')) + "]";
      }
      idt = this.tab + TAB;
      i = o.scope.freeVariable('i');
      result = o.scope.freeVariable('results');
      pre = "\n" + idt + result + " = [];";
      if (this.fromNum && this.toNum) {
        o.index = i;
        body = this.compileNode(o);
      } else {
        vars = ("" + i + " = " + this.fromC) + (this.toC !== this.toVar ? ", " + this.toC : '');
        cond = "" + this.fromVar + " <= " + this.toVar;
        body = "var " + vars + "; " + cond + " ? " + i + " <" + this.equals + " " + this.toVar + " : " + i + " >" + this.equals + " " + this.toVar + "; " + cond + " ? " + i + "++ : " + i + "--";
      }
      post = "{ " + result + ".push(" + i + "); }\n" + idt + "return " + result + ";\n" + o.indent;
      hasArgs = function(node) {
        return node != null ? node.contains(function(n) {
          return n instanceof Literal && n.value === 'arguments' && !n.asKey;
        }) : void 0;
      };
      if (hasArgs(this.from) || hasArgs(this.to)) {
        args = ', arguments';
      }
      return "(function() {" + pre + "\n" + idt + "for (" + body + ")" + post + "}).apply(this" + (args != null ? args : '') + ")";
    };
    return Range;
  })();
  exports.Slice = Slice = (function() {
    __extends(Slice, Base);
    Slice.prototype.children = ['range'];
    function Slice(range) {
      this.range = range;
      Slice.__super__.constructor.call(this);
    }
    Slice.prototype.compileNode = function(o) {
      var compiled, from, fromStr, to, toStr, _ref2;
      _ref2 = this.range, to = _ref2.to, from = _ref2.from;
      fromStr = from && from.compile(o, LEVEL_PAREN) || '0';
      compiled = to && to.compile(o, LEVEL_PAREN);
      if (to && !(!this.range.exclusive && +compiled === -1)) {
        toStr = ', ' + (this.range.exclusive ? compiled : SIMPLENUM.test(compiled) ? (+compiled + 1).toString() : "(" + compiled + " + 1) || 9e9");
      }
      return ".slice(" + fromStr + (toStr || '') + ")";
    };
    return Slice;
  })();
  exports.Obj = Obj = (function() {
    __extends(Obj, Base);
    function Obj(props, generated) {
      this.generated = generated != null ? generated : false;
      this.objects = this.properties = props || [];
    }
    Obj.prototype.children = ['properties'];
    Obj.prototype.compileNode = function(o) {
      var i, idt, indent, join, lastNoncom, node, obj, prop, props, _i, _len;
      props = this.properties;
      if (!props.length) {
        if (this.front) {
          return '({})';
        } else {
          return '{}';
        }
      }
      if (this.generated) {
        for (_i = 0, _len = props.length; _i < _len; _i++) {
          node = props[_i];
          if (node instanceof Value) {
            throw new Error('cannot have an implicit value in an implicit object');
          }
        }
      }
      idt = o.indent += TAB;
      lastNoncom = this.lastNonComment(this.properties);
      props = (function() {
        var _len2, _results;
        _results = [];
        for (i = 0, _len2 = props.length; i < _len2; i++) {
          prop = props[i];
          join = i === props.length - 1 ? '' : prop === lastNoncom || prop instanceof Comment ? '\n' : ',\n';
          indent = prop instanceof Comment ? '' : idt;
          if (prop instanceof Value && prop["this"]) {
            prop = new Assign(prop.properties[0].name, prop, 'object');
          }
          if (!(prop instanceof Comment)) {
            if (!(prop instanceof Assign)) {
              prop = new Assign(prop, prop, 'object');
            }
            (prop.variable.base || prop.variable).asKey = true;
          }
          _results.push(indent + prop.compile(o, LEVEL_TOP) + join);
        }
        return _results;
      })();
      props = props.join('');
      obj = "{" + (props && '\n' + props + '\n' + this.tab) + "}";
      if (this.front) {
        return "(" + obj + ")";
      } else {
        return obj;
      }
    };
    Obj.prototype.assigns = function(name) {
      var prop, _i, _len, _ref2;
      _ref2 = this.properties;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        prop = _ref2[_i];
        if (prop.assigns(name)) {
          return true;
        }
      }
      return false;
    };
    return Obj;
  })();
  exports.Arr = Arr = (function() {
    __extends(Arr, Base);
    function Arr(objs) {
      this.objects = objs || [];
    }
    Arr.prototype.children = ['objects'];
    Arr.prototype.filterImplicitObjects = Call.prototype.filterImplicitObjects;
    Arr.prototype.compileNode = function(o) {
      var code, obj, objs;
      if (!this.objects.length) {
        return '[]';
      }
      o.indent += TAB;
      objs = this.filterImplicitObjects(this.objects);
      if (code = Splat.compileSplattedArray(o, objs)) {
        return code;
      }
      code = ((function() {
        var _i, _len, _results;
        _results = [];
        for (_i = 0, _len = objs.length; _i < _len; _i++) {
          obj = objs[_i];
          _results.push(obj.compile(o, LEVEL_LIST));
        }
        return _results;
      })()).join(', ');
      if (code.indexOf('\n') >= 0) {
        return "[\n" + o.indent + code + "\n" + this.tab + "]";
      } else {
        return "[" + code + "]";
      }
    };
    Arr.prototype.assigns = function(name) {
      var obj, _i, _len, _ref2;
      _ref2 = this.objects;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        obj = _ref2[_i];
        if (obj.assigns(name)) {
          return true;
        }
      }
      return false;
    };
    return Arr;
  })();
  exports.Class = Class = (function() {
    __extends(Class, Base);
    function Class(variable, parent, body) {
      this.variable = variable;
      this.parent = parent;
      this.body = body != null ? body : new Block;
      this.boundFuncs = [];
      this.body.classBody = true;
    }
    Class.prototype.children = ['variable', 'parent', 'body'];
    Class.prototype.determineName = function(o) {
      var decl, tail;
      if (!this.variable) {
        return null;
      }
      if (o.google) {
        return this.variable.compile(o);
      }
      decl = (tail = last(this.variable.properties)) ? tail instanceof Access && tail.name.value : this.variable.base.value;
      return decl && (decl = IDENTIFIER.test(decl) && decl);
    };
    Class.prototype.setContext = function(name) {
      return this.body.traverseChildren(false, function(node) {
        if (node.classBody) {
          return false;
        }
        if (node instanceof Literal && node.value === 'this') {
          return node.value = name;
        } else if (node instanceof Code) {
          node.klass = name;
          if (node.bound) {
            return node.context = name;
          }
        }
      });
    };
    Class.prototype.addBoundFunctions = function(o) {
      var bvar, lhs, _i, _len, _ref2, _results;
      if (this.boundFuncs.length) {
        _ref2 = this.boundFuncs;
        _results = [];
        for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
          bvar = _ref2[_i];
          lhs = (new Value(new Literal("this"), [new Access(bvar)])).compile(o);
          _results.push(this.ctor.body.unshift(new Literal("" + lhs + " = " + (utility('bind')) + "(" + lhs + ", this)")));
        }
        return _results;
      }
    };
    Class.prototype.addProperties = function(node, name, o) {
      var assign, base, exprs, func, props;
      props = node.base.properties.slice(0);
      exprs = (function() {
        var _results;
        _results = [];
        while (assign = props.shift()) {
          if (assign instanceof Assign) {
            base = assign.variable.base;
            delete assign.context;
            func = assign.value;
            if (base.value === 'constructor') {
              if (this.ctor) {
                throw new Error('cannot define more than one constructor in a class');
              }
              if (func.bound) {
                throw new Error('cannot define a constructor as a bound function');
              }
              if (func instanceof Code) {
                assign = this.ctor = func;
              } else {
                this.externalCtor = o.scope.freeVariable('class');
                assign = new Assign(new Literal(this.externalCtor), func);
              }
            } else {
              if (!assign.variable["this"]) {
                assign.variable = new Value(new Literal(name), [new Access(base, 'proto')]);
              }
              if (func instanceof Code && func.bound) {
                this.boundFuncs.push(base);
                func.bound = false;
              }
            }
          }
          _results.push(assign);
        }
        return _results;
      }).call(this);
      return compact(exprs);
    };
    Class.prototype.walkBody = function(name, o) {
      return this.traverseChildren(false, __bind(function(child) {
        var exps, i, node, _len, _ref2;
        if (child instanceof Class) {
          return false;
        }
        if (child instanceof Block) {
          _ref2 = exps = child.expressions;
          for (i = 0, _len = _ref2.length; i < _len; i++) {
            node = _ref2[i];
            if (node instanceof Value && node.isObject(true)) {
              exps[i] = this.addProperties(node, name, o);
            }
          }
          return child.expressions = exps = flatten(exps);
        }
      }, this));
    };
    Class.prototype.ensureConstructor = function(name, o) {
      if (!this.ctor) {
        this.ctor = new Code;
        if (!o.google) {
          if (this.parent) {
            this.ctor.body.push(new Literal("" + name + ".__super__.constructor.apply(this, arguments)"));
          }
          if (this.externalCtor) {
            this.ctor.body.push(new Literal("" + this.externalCtor + ".apply(this, arguments)"));
          }
        }
        this.body.expressions.unshift(this.ctor);
      }
      this.ctor.ctor = this.ctor.name = name;
      this.ctor.klass = null;
      this.ctor.noReturn = true;
      return this.ctor.ctorParent = this.parent;
    };
    Class.prototype.compileNode = function(o) {
      var decl, klass, lname, name;
      decl = this.determineName(o);
      name = decl || this.name || '_Class';
      lname = new Literal(name);
      this.setContext(name);
      this.walkBody(name, o);
      this.ensureConstructor(name, o);
      if (this.parent && !o.google) {
        this.body.expressions.unshift(new Extends(lname, this.parent));
      }
      if (!(this.ctor instanceof Code)) {
        this.body.expressions.unshift(this.ctor);
      }
      if (!o.google) {
        this.body.expressions.push(lname);
      }
      this.addBoundFunctions(o);
      if (o.google) {
        return this.body.compile(o);
      } else {
        klass = new Parens(Closure.wrap(this.body), true);
        if (this.variable) {
          klass = new Assign(this.variable, klass);
        }
        return klass.compile(o);
      }
    };
    return Class;
  })();
  exports.Assign = Assign = (function() {
    __extends(Assign, Base);
    function Assign(variable, value, context, options) {
      this.variable = variable;
      this.value = value;
      this.context = context;
      this.param = options && options.param;
    }
    Assign.prototype.children = ['variable', 'value'];
    Assign.prototype.isStatement = function(o) {
      return (o != null ? o.level : void 0) === LEVEL_TOP && this.context && __indexOf.call(this.context, "?") >= 0;
    };
    Assign.prototype.assigns = function(name) {
      return this[this.context === 'object' ? 'value' : 'variable'].assigns(name);
    };
    Assign.prototype.unfoldSoak = function(o) {
      return unfoldSoak(o, this, 'variable');
    };
    Assign.prototype.compileNode = function(o) {
      var isValue, match, name, val, _ref2, _ref3, _ref4, _ref5;
      if (isValue = this.variable instanceof Value) {
        if (this.variable.isArray() || this.variable.isObject()) {
          return this.compilePatternMatch(o);
        }
        if (this.variable.isSplice()) {
          return this.compileSplice(o);
        }
        if ((_ref2 = this.context) === '||=' || _ref2 === '&&=' || _ref2 === '?=') {
          return this.compileConditional(o);
        }
      }
      name = this.variable.compile(o, LEVEL_LIST);
      if (!(this.context || this.variable.isAssignable())) {
        throw SyntaxError("\"" + (this.variable.compile(o)) + "\" cannot be assigned.");
      }
      if (!(this.context || isValue && (this.variable.namespaced || this.variable.hasProperties()))) {
        if (this.param) {
          o.scope.add(name, 'var');
        } else {
          o.scope.find(name);
        }
      }
      if (this.value instanceof Code && (match = METHOD_DEF.exec(name))) {
        if (match[1]) {
          this.value.klass = match[1];
        }
        this.value.name = (_ref3 = (_ref4 = (_ref5 = match[2]) != null ? _ref5 : match[3]) != null ? _ref4 : match[4]) != null ? _ref3 : match[5];
      }
      val = this.value.compile(o, LEVEL_LIST);
      if (this.context === 'object') {
        return "" + name + ": " + val;
      }
      val = name + (" " + (this.context || '=') + " ") + val;
      if (o.level <= LEVEL_LIST) {
        return val;
      } else {
        return "(" + val + ")";
      }
    };
    Assign.prototype.compilePatternMatch = function(o) {
      var acc, assigns, code, i, idx, isObject, ivar, obj, objects, olen, ref, rest, splat, top, val, value, vvar, _len, _ref2, _ref3, _ref4, _ref5;
      top = o.level === LEVEL_TOP;
      value = this.value;
      objects = this.variable.base.objects;
      if (!(olen = objects.length)) {
        code = value.compile(o);
        if (o.level >= LEVEL_OP) {
          return "(" + code + ")";
        } else {
          return code;
        }
      }
      isObject = this.variable.isObject();
      if (top && olen === 1 && !((obj = objects[0]) instanceof Splat)) {
        if (obj instanceof Assign) {
          _ref2 = obj, idx = _ref2.variable.base, obj = _ref2.value;
        } else {
          if (obj.base instanceof Parens) {
            _ref3 = new Value(obj.unwrapAll()).cacheReference(o), obj = _ref3[0], idx = _ref3[1];
          } else {
            idx = isObject ? obj["this"] ? obj.properties[0].name : obj : new Literal(0);
          }
        }
        acc = IDENTIFIER.test(idx.unwrap().value || 0);
        value = new Value(value);
        value.properties.push(new (acc ? Access : Index)(idx));
        return new Assign(obj, value, null, {
          param: this.param
        }).compile(o, LEVEL_TOP);
      }
      vvar = value.compile(o, LEVEL_LIST);
      assigns = [];
      splat = false;
      if (!IDENTIFIER.test(vvar) || this.variable.assigns(vvar)) {
        assigns.push("" + (ref = o.scope.freeVariable('ref')) + " = " + vvar);
        vvar = ref;
      }
      for (i = 0, _len = objects.length; i < _len; i++) {
        obj = objects[i];
        idx = i;
        if (isObject) {
          if (obj instanceof Assign) {
            _ref4 = obj, idx = _ref4.variable.base, obj = _ref4.value;
          } else {
            if (obj.base instanceof Parens) {
              _ref5 = new Value(obj.unwrapAll()).cacheReference(o), obj = _ref5[0], idx = _ref5[1];
            } else {
              idx = obj["this"] ? obj.properties[0].name : obj;
            }
          }
        }
        if (!splat && obj instanceof Splat) {
          val = "" + olen + " <= " + vvar + ".length ? " + (utility('slice')) + ".call(" + vvar + ", " + i;
          if (rest = olen - i - 1) {
            ivar = o.scope.freeVariable('i');
            val += ", " + ivar + " = " + vvar + ".length - " + rest + ") : (" + ivar + " = " + i + ", [])";
          } else {
            val += ") : []";
          }
          val = new Literal(val);
          splat = "" + ivar + "++";
        } else {
          if (obj instanceof Splat) {
            obj = obj.name.compile(o);
            throw SyntaxError("multiple splats are disallowed in an assignment: " + obj + " ...");
          }
          if (typeof idx === 'number') {
            idx = new Literal(splat || idx);
            acc = false;
          } else {
            acc = isObject && IDENTIFIER.test(idx.unwrap().value || 0);
          }
          val = new Value(new Literal(vvar), [new (acc ? Access : Index)(idx)]);
        }
        assigns.push(new Assign(obj, val, null, {
          param: this.param
        }).compile(o, LEVEL_TOP));
      }
      if (!top) {
        assigns.push(vvar);
      }
      code = assigns.join(', ');
      if (o.level < LEVEL_LIST) {
        return code;
      } else {
        return "(" + code + ")";
      }
    };
    Assign.prototype.compileConditional = function(o) {
      var left, rite, _ref2;
      _ref2 = this.variable.cacheReference(o), left = _ref2[0], rite = _ref2[1];
      if (__indexOf.call(this.context, "?") >= 0) {
        o.isExistentialEquals = true;
      }
      return new Op(this.context.slice(0, -1), left, new Assign(rite, this.value, '=')).compile(o);
    };
    Assign.prototype.compileSplice = function(o) {
      var code, exclusive, from, fromDecl, fromRef, name, to, valDef, valRef, _ref2, _ref3, _ref4;
      _ref2 = this.variable.properties.pop().range, from = _ref2.from, to = _ref2.to, exclusive = _ref2.exclusive;
      name = this.variable.compile(o);
      _ref3 = (from != null ? from.cache(o, LEVEL_OP) : void 0) || ['0', '0'], fromDecl = _ref3[0], fromRef = _ref3[1];
      if (to) {
        if ((from != null ? from.isSimpleNumber() : void 0) && to.isSimpleNumber()) {
          to = +to.compile(o) - +fromRef;
          if (!exclusive) {
            to += 1;
          }
        } else {
          to = to.compile(o) + ' - ' + fromRef;
          if (!exclusive) {
            to += ' + 1';
          }
        }
      } else {
        to = "9e9";
      }
      _ref4 = this.value.cache(o, LEVEL_LIST), valDef = _ref4[0], valRef = _ref4[1];
      code = "[].splice.apply(" + name + ", [" + fromDecl + ", " + to + "].concat(" + valDef + ")), " + valRef;
      if (o.level > LEVEL_TOP) {
        return "(" + code + ")";
      } else {
        return code;
      }
    };
    return Assign;
  })();
  exports.Code = Code = (function() {
    __extends(Code, Base);
    function Code(params, body, tag) {
      this.params = params || [];
      this.body = body || new Block;
      this.bound = tag === 'boundfunc';
      if (this.bound) {
        this.context = 'this';
      }
    }
    Code.prototype.children = ['params', 'body'];
    Code.prototype.isStatement = function() {
      return !!this.ctor;
    };
    Code.prototype.jumps = NO;
    Code.prototype.compileNode = function(o) {
      var code, exprs, extendsJsDoc, extendsNode, i, idt, isGoogleConstructor, lit, p, param, parentClassName, ref, splats, v, val, vars, wasEmpty, _i, _j, _k, _len, _len2, _len3, _len4, _ref2, _ref3, _ref4, _ref5;
      o.scope = new Scope(o.scope, this.body, this);
      o.scope.shared = del(o, 'sharedScope');
      o.indent += TAB;
      delete o.bare;
      vars = [];
      exprs = [];
      _ref2 = this.params;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        param = _ref2[_i];
        if (param.splat) {
          _ref3 = this.params;
          for (_j = 0, _len2 = _ref3.length; _j < _len2; _j++) {
            p = _ref3[_j];
            if (p.name.value) {
              o.scope.add(p.name.value, 'var', true);
            }
          }
          splats = new Assign(new Value(new Arr((function() {
            var _k, _len3, _ref4, _results;
            _ref4 = this.params;
            _results = [];
            for (_k = 0, _len3 = _ref4.length; _k < _len3; _k++) {
              p = _ref4[_k];
              _results.push(p.asReference(o));
            }
            return _results;
          }).call(this))), new Value(new Literal('arguments')));
          break;
        }
      }
      _ref4 = this.params;
      for (_k = 0, _len3 = _ref4.length; _k < _len3; _k++) {
        param = _ref4[_k];
        if (param.isComplex()) {
          val = ref = param.asReference(o);
          if (param.value) {
            val = new Op('?', ref, param.value);
          }
          exprs.push(new Assign(new Value(param.name), val, '=', {
            param: true
          }));
        } else {
          ref = param;
          if (param.value) {
            lit = new Literal(ref.name.value + ' == null');
            val = new Assign(new Value(param.name), param.value, '=');
            exprs.push(new If(lit, val));
          }
        }
        if (!splats) {
          vars.push(ref);
        }
      }
      wasEmpty = this.body.isEmpty();
      if (splats) {
        exprs.unshift(splats);
      }
      if (exprs.length) {
        (_ref5 = this.body.expressions).unshift.apply(_ref5, exprs);
      }
      if (!splats) {
        for (i = 0, _len4 = vars.length; i < _len4; i++) {
          v = vars[i];
          o.scope.parameter(vars[i] = v.compile(o));
        }
      }
      if (!(wasEmpty || this.noReturn)) {
        this.body.makeReturn();
      }
      idt = o.indent;
      isGoogleConstructor = o.google && this.ctor;
      if (isGoogleConstructor) {
        if (this.ctorParent) {
          parentClassName = this.ctorParent.compile(o);
          o.google.includes.push({
            name: parentClassName,
            alias: null
          });
          extendsJsDoc = "" + this.tab + " * @extends {" + parentClassName + "}\n";
        } else {
          extendsJsDoc = '';
        }
        o.google.provides.push(this.name);
        code = "" + this.tab + "/**\n" + this.tab + " * @constructor\n" + extendsJsDoc + this.tab + " */\n" + this.tab + this.name + " = function";
      } else {
        code = 'function';
        if (this.ctor) {
          code += ' ' + this.name;
        }
      }
      code += '(' + vars.join(', ') + ') {';
      if (!this.body.isEmpty()) {
        code += "\n" + (this.body.compileWithDeclarations(o)) + "\n" + this.tab;
      }
      if (isGoogleConstructor) {
        code += '};';
        if (this.ctorParent) {
          extendsNode = new Extends(new Literal(this.name), this.ctorParent);
          code += '\n' + extendsNode.compile(o) + ';';
        }
        code += '\n';
      } else {
        code += '}';
      }
      if (this.ctor) {
        return this.tab + code;
      }
      if (this.bound) {
        return utility('bind') + ("(" + code + ", " + this.context + ")");
      }
      if (this.front || (o.level >= LEVEL_ACCESS)) {
        return "(" + code + ")";
      } else {
        return code;
      }
    };
    Code.prototype.traverseChildren = function(crossScope, func) {
      if (crossScope) {
        return Code.__super__.traverseChildren.call(this, crossScope, func);
      }
    };
    return Code;
  })();
  exports.Param = Param = (function() {
    __extends(Param, Base);
    function Param(name, value, splat) {
      this.name = name;
      this.value = value;
      this.splat = splat;
    }
    Param.prototype.children = ['name', 'value'];
    Param.prototype.compile = function(o) {
      return this.name.compile(o, LEVEL_LIST);
    };
    Param.prototype.asReference = function(o) {
      var node;
      if (this.reference) {
        return this.reference;
      }
      node = this.name;
      if (node["this"]) {
        node = node.properties[0].name;
        if (node.value.reserved) {
          node = new Literal('_' + node.value);
        }
      } else if (node.isComplex()) {
        node = new Literal(o.scope.freeVariable('arg'));
      }
      node = new Value(node);
      if (this.splat) {
        node = new Splat(node);
      }
      return this.reference = node;
    };
    Param.prototype.isComplex = function() {
      return this.name.isComplex();
    };
    return Param;
  })();
  exports.Splat = Splat = (function() {
    __extends(Splat, Base);
    Splat.prototype.children = ['name'];
    Splat.prototype.isAssignable = YES;
    function Splat(name) {
      this.name = name.compile ? name : new Literal(name);
    }
    Splat.prototype.assigns = function(name) {
      return this.name.assigns(name);
    };
    Splat.prototype.compile = function(o) {
      if (this.index != null) {
        return this.compileParam(o);
      } else {
        return this.name.compile(o);
      }
    };
    Splat.compileSplattedArray = function(o, list, apply) {
      var args, base, code, i, index, node, _len;
      index = -1;
      while ((node = list[++index]) && !(node instanceof Splat)) {
        continue;
      }
      if (index >= list.length) {
        return '';
      }
      if (list.length === 1) {
        code = list[0].compile(o, LEVEL_LIST);
        if (apply) {
          return code;
        }
        return "" + (utility('slice')) + ".call(" + code + ")";
      }
      args = list.slice(index);
      for (i = 0, _len = args.length; i < _len; i++) {
        node = args[i];
        code = node.compile(o, LEVEL_LIST);
        args[i] = node instanceof Splat ? "" + (utility('slice')) + ".call(" + code + ")" : "[" + code + "]";
      }
      if (index === 0) {
        return args[0] + (".concat(" + (args.slice(1).join(', ')) + ")");
      }
      base = (function() {
        var _i, _len2, _ref2, _results;
        _ref2 = list.slice(0, index);
        _results = [];
        for (_i = 0, _len2 = _ref2.length; _i < _len2; _i++) {
          node = _ref2[_i];
          _results.push(node.compile(o, LEVEL_LIST));
        }
        return _results;
      })();
      return "[" + (base.join(', ')) + "].concat(" + (args.join(', ')) + ")";
    };
    return Splat;
  })();
  exports.While = While = (function() {
    __extends(While, Base);
    function While(condition, options) {
      this.condition = (options != null ? options.invert : void 0) ? condition.invert() : condition;
      this.guard = options != null ? options.guard : void 0;
    }
    While.prototype.children = ['condition', 'guard', 'body'];
    While.prototype.isStatement = YES;
    While.prototype.makeReturn = function() {
      this.returns = true;
      return this;
    };
    While.prototype.addBody = function(body) {
      this.body = body;
      return this;
    };
    While.prototype.jumps = function() {
      var expressions, node, _i, _len;
      expressions = this.body.expressions;
      if (!expressions.length) {
        return false;
      }
      for (_i = 0, _len = expressions.length; _i < _len; _i++) {
        node = expressions[_i];
        if (node.jumps({
          loop: true
        })) {
          return node;
        }
      }
      return false;
    };
    While.prototype.compileNode = function(o) {
      var body, code, rvar, set;
      o.indent += TAB;
      set = '';
      body = this.body;
      if (body.isEmpty()) {
        body = '';
      } else {
        if (o.level > LEVEL_TOP || this.returns) {
          rvar = o.scope.freeVariable('results');
          set = "" + this.tab + rvar + " = [];\n";
          if (body) {
            body = Push.wrap(rvar, body);
          }
        }
        if (this.guard) {
          body = Block.wrap([new If(this.guard, body)]);
        }
        body = "\n" + (body.compile(o, LEVEL_TOP)) + "\n" + this.tab;
      }
      code = set + this.tab + ("while (" + (this.condition.compile(o, LEVEL_PAREN)) + ") {" + body + "}");
      if (this.returns) {
        code += "\n" + this.tab + "return " + rvar + ";";
      }
      return code;
    };
    return While;
  })();
  exports.Op = Op = (function() {
    var CONVERSIONS, INVERSIONS;
    __extends(Op, Base);
    function Op(op, first, second, flip) {
      var call;
      if (op === 'in') {
        return new In(first, second);
      }
      if (op === 'do') {
        call = new Call(first, first.params || []);
        call["do"] = true;
        return call;
      }
      if (op === 'new') {
        if (first instanceof Call && !first["do"] && !first.isNew) {
          return first.newInstance();
        }
        if (first instanceof Code && first.bound || first["do"]) {
          first = new Parens(first);
        }
      }
      this.operator = CONVERSIONS[op] || op;
      this.first = first;
      this.second = second;
      this.flip = !!flip;
      return this;
    }
    CONVERSIONS = {
      '==': '===',
      '!=': '!==',
      'of': 'in'
    };
    INVERSIONS = {
      '!==': '===',
      '===': '!=='
    };
    Op.prototype.children = ['first', 'second'];
    Op.prototype.isSimpleNumber = NO;
    Op.prototype.isUnary = function() {
      return !this.second;
    };
    Op.prototype.isComplex = function() {
      var _ref2;
      return !(this.isUnary() && ((_ref2 = this.operator) === '+' || _ref2 === '-')) || this.first.isComplex();
    };
    Op.prototype.isChainable = function() {
      var _ref2;
      return (_ref2 = this.operator) === '<' || _ref2 === '>' || _ref2 === '>=' || _ref2 === '<=' || _ref2 === '===' || _ref2 === '!==';
    };
    Op.prototype.invert = function() {
      var allInvertable, curr, fst, op, _ref2;
      if (this.isChainable() && this.first.isChainable()) {
        allInvertable = true;
        curr = this;
        while (curr && curr.operator) {
          allInvertable && (allInvertable = curr.operator in INVERSIONS);
          curr = curr.first;
        }
        if (!allInvertable) {
          return new Parens(this).invert();
        }
        curr = this;
        while (curr && curr.operator) {
          curr.invert = !curr.invert;
          curr.operator = INVERSIONS[curr.operator];
          curr = curr.first;
        }
        return this;
      } else if (op = INVERSIONS[this.operator]) {
        this.operator = op;
        if (this.first.unwrap() instanceof Op) {
          this.first.invert();
        }
        return this;
      } else if (this.second) {
        return new Parens(this).invert();
      } else if (this.operator === '!' && (fst = this.first.unwrap()) instanceof Op && ((_ref2 = fst.operator) === '!' || _ref2 === 'in' || _ref2 === 'instanceof')) {
        return fst;
      } else {
        return new Op('!', this);
      }
    };
    Op.prototype.unfoldSoak = function(o) {
      var _ref2;
      return ((_ref2 = this.operator) === '++' || _ref2 === '--' || _ref2 === 'delete') && unfoldSoak(o, this, 'first');
    };
    Op.prototype.compileNode = function(o) {
      var code;
      if (this.isUnary()) {
        return this.compileUnary(o);
      }
      if (this.isChainable() && this.first.isChainable()) {
        return this.compileChain(o);
      }
      if (this.operator === '?') {
        return this.compileExistence(o);
      }
      this.first.front = this.front;
      code = this.first.compile(o, LEVEL_OP) + ' ' + this.operator + ' ' + this.second.compile(o, LEVEL_OP);
      if (o.level <= LEVEL_OP) {
        return code;
      } else {
        return "(" + code + ")";
      }
    };
    Op.prototype.compileChain = function(o) {
      var code, fst, shared, _ref2;
      _ref2 = this.first.second.cache(o), this.first.second = _ref2[0], shared = _ref2[1];
      fst = this.first.compile(o, LEVEL_OP);
      code = "" + fst + " " + (this.invert ? '&&' : '||') + " " + (shared.compile(o)) + " " + this.operator + " " + (this.second.compile(o, LEVEL_OP));
      return "(" + code + ")";
    };
    Op.prototype.compileExistence = function(o) {
      var fst, ref;
      if (this.first.isComplex()) {
        ref = new Literal(o.scope.freeVariable('ref'));
        fst = new Parens(new Assign(ref, this.first));
      } else {
        fst = this.first;
        ref = fst;
      }
      return new If(new Existence(fst), ref, {
        type: 'if'
      }).addElse(this.second).compile(o);
    };
    Op.prototype.compileUnary = function(o) {
      var op, parts;
      parts = [op = this.operator];
      if ((op === 'new' || op === 'typeof' || op === 'delete') || (op === '+' || op === '-') && this.first instanceof Op && this.first.operator === op) {
        parts.push(' ');
      }
      if (op === 'new' && this.first.isStatement(o)) {
        this.first = new Parens(this.first);
      }
      parts.push(this.first.compile(o, LEVEL_OP));
      if (this.flip) {
        parts.reverse();
      }
      return parts.join('');
    };
    Op.prototype.toString = function(idt) {
      return Op.__super__.toString.call(this, idt, this.constructor.name + ' ' + this.operator);
    };
    return Op;
  })();
  exports.In = In = (function() {
    __extends(In, Base);
    function In(object, array) {
      this.object = object;
      this.array = array;
    }
    In.prototype.children = ['object', 'array'];
    In.prototype.invert = NEGATE;
    In.prototype.compileNode = function(o) {
      var hasSplat, obj, _i, _len, _ref2;
      if (this.array instanceof Value && this.array.isArray()) {
        _ref2 = this.array.base.objects;
        for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
          obj = _ref2[_i];
          if (obj instanceof Splat) {
            hasSplat = true;
            break;
          }
        }
        if (!hasSplat) {
          return this.compileOrTest(o);
        }
      }
      return this.compileLoopTest(o);
    };
    In.prototype.compileOrTest = function(o) {
      var cmp, cnj, i, item, ref, sub, tests, _ref2, _ref3;
      _ref2 = this.object.cache(o, LEVEL_OP), sub = _ref2[0], ref = _ref2[1];
      _ref3 = this.negated ? [' !== ', ' && '] : [' === ', ' || '], cmp = _ref3[0], cnj = _ref3[1];
      tests = (function() {
        var _len, _ref4, _results;
        _ref4 = this.array.base.objects;
        _results = [];
        for (i = 0, _len = _ref4.length; i < _len; i++) {
          item = _ref4[i];
          _results.push((i ? ref : sub) + cmp + item.compile(o, LEVEL_OP));
        }
        return _results;
      }).call(this);
      if (tests.length === 0) {
        return 'false';
      }
      tests = tests.join(cnj);
      if (o.level < LEVEL_OP) {
        return tests;
      } else {
        return "(" + tests + ")";
      }
    };
    In.prototype.compileLoopTest = function(o) {
      var code, ref, sub, _ref2;
      _ref2 = this.object.cache(o, LEVEL_LIST), sub = _ref2[0], ref = _ref2[1];
      code = utility('indexOf') + (".call(" + (this.array.compile(o, LEVEL_LIST)) + ", " + ref + ") ") + (this.negated ? '< 0' : '>= 0');
      if (sub === ref) {
        return code;
      }
      code = sub + ', ' + code;
      if (o.level < LEVEL_LIST) {
        return code;
      } else {
        return "(" + code + ")";
      }
    };
    In.prototype.toString = function(idt) {
      return In.__super__.toString.call(this, idt, this.constructor.name + (this.negated ? '!' : ''));
    };
    return In;
  })();
  exports.Try = Try = (function() {
    __extends(Try, Base);
    function Try(attempt, error, recovery, ensure) {
      this.attempt = attempt;
      this.error = error;
      this.recovery = recovery;
      this.ensure = ensure;
    }
    Try.prototype.children = ['attempt', 'recovery', 'ensure'];
    Try.prototype.isStatement = YES;
    Try.prototype.jumps = function(o) {
      var _ref2;
      return this.attempt.jumps(o) || ((_ref2 = this.recovery) != null ? _ref2.jumps(o) : void 0);
    };
    Try.prototype.makeReturn = function() {
      if (this.attempt) {
        this.attempt = this.attempt.makeReturn();
      }
      if (this.recovery) {
        this.recovery = this.recovery.makeReturn();
      }
      return this;
    };
    Try.prototype.compileNode = function(o) {
      var catchPart, errorPart;
      o.indent += TAB;
      errorPart = this.error ? " (" + (this.error.compile(o)) + ") " : ' ';
      catchPart = this.recovery ? (o.scope.add(this.error.value, 'param'), " catch" + errorPart + "{\n" + (this.recovery.compile(o, LEVEL_TOP)) + "\n" + this.tab + "}") : !(this.ensure || this.recovery) ? ' catch (_e) {}' : void 0;
      return ("" + this.tab + "try {\n" + (this.attempt.compile(o, LEVEL_TOP)) + "\n" + this.tab + "}" + (catchPart || '')) + (this.ensure ? " finally {\n" + (this.ensure.compile(o, LEVEL_TOP)) + "\n" + this.tab + "}" : '');
    };
    return Try;
  })();
  exports.Throw = Throw = (function() {
    __extends(Throw, Base);
    function Throw(expression) {
      this.expression = expression;
    }
    Throw.prototype.children = ['expression'];
    Throw.prototype.isStatement = YES;
    Throw.prototype.jumps = NO;
    Throw.prototype.makeReturn = THIS;
    Throw.prototype.compileNode = function(o) {
      return this.tab + ("throw " + (this.expression.compile(o)) + ";");
    };
    return Throw;
  })();
  exports.Include = Include = (function() {
    __extends(Include, Base);
    function Include(namespace, alias) {
      this.namespace = namespace;
      this.alias = alias != null ? alias : null;
    }
    Include.prototype.compileNode = function(o) {
      o.google.includes.push({
        name: this.namespace.flatten(),
        alias: this.alias
      });
      return "";
    };
    return Include;
  })();
  exports.Namespace = Namespace = (function() {
    __extends(Namespace, Base);
    function Namespace(identifier, namespace) {
      this.identifier = identifier;
      this.namespace = namespace != null ? namespace : null;
    }
    Namespace.prototype.flatten = function() {
      var ids, ns;
      ns = this.namespace;
      ids = [this.identifier];
      while (ns) {
        ids.unshift(ns.identifier);
        ns = ns.namespace;
      }
      return ids.join('.');
    };
    Namespace.prototype.compileNode = function(o) {
      return this.flatten();
    };
    return Namespace;
  })();
  exports.Existence = Existence = (function() {
    __extends(Existence, Base);
    function Existence(expression) {
      this.expression = expression;
    }
    Existence.prototype.children = ['expression'];
    Existence.prototype.invert = NEGATE;
    Existence.prototype.compileNode = function(o) {
      var cmp, cnj, code, _ref2;
      code = this.expression.compile(o, LEVEL_OP);
      code = IDENTIFIER.test(code) && !o.scope.check(code) ? ((_ref2 = this.negated ? ['===', '||'] : ['!==', '&&'], cmp = _ref2[0], cnj = _ref2[1], _ref2), "typeof " + code + " " + cmp + " \"undefined\" " + cnj + " " + code + " " + cmp + " null") : "" + code + " " + (this.negated ? '==' : '!=') + " null";
      if (o.level <= LEVEL_COND) {
        return code;
      } else {
        return "(" + code + ")";
      }
    };
    return Existence;
  })();
  exports.Parens = Parens = (function() {
    __extends(Parens, Base);
    function Parens(body) {
      this.body = body;
    }
    Parens.prototype.children = ['body'];
    Parens.prototype.unwrap = function() {
      return this.body;
    };
    Parens.prototype.isComplex = function() {
      return this.body.isComplex();
    };
    Parens.prototype.makeReturn = function() {
      return this.body.makeReturn();
    };
    Parens.prototype.compileNode = function(o) {
      var bare, code, expr;
      expr = this.body.unwrap();
      if (expr instanceof Value && expr.isAtomic()) {
        expr.front = this.front;
        return expr.compile(o);
      }
      code = expr.compile(o, LEVEL_PAREN);
      bare = o.level < LEVEL_OP && (expr instanceof Op || expr instanceof Call || (expr instanceof For && expr.returns));
      if (bare) {
        return code;
      } else {
        return "(" + code + ")";
      }
    };
    return Parens;
  })();
  exports.For = For = (function() {
    __extends(For, Base);
    function For(body, source) {
      var _ref2;
      this.source = source.source, this.guard = source.guard, this.step = source.step, this.name = source.name, this.index = source.index;
      this.body = Block.wrap([body]);
      this.own = !!source.own;
      this.object = !!source.object;
      if (this.object) {
        _ref2 = [this.index, this.name], this.name = _ref2[0], this.index = _ref2[1];
      }
      if (this.index instanceof Value) {
        throw SyntaxError('index cannot be a pattern matching expression');
      }
      this.range = this.source instanceof Value && this.source.base instanceof Range && !this.source.properties.length;
      this.pattern = this.name instanceof Value;
      if (this.range && this.index) {
        throw SyntaxError('indexes do not apply to range loops');
      }
      if (this.range && this.pattern) {
        throw SyntaxError('cannot pattern match over range loops');
      }
      this.returns = false;
    }
    For.prototype.children = ['body', 'source', 'guard', 'step'];
    For.prototype.isStatement = YES;
    For.prototype.jumps = While.prototype.jumps;
    For.prototype.makeReturn = function() {
      this.returns = true;
      return this;
    };
    For.prototype.compileNode = function(o) {
      var body, defPart, forPart, forVarPart, guardPart, idt1, index, ivar, lastJumps, lvar, name, namePart, ref, resultPart, returnResult, rvar, scope, source, stepPart, stepvar, svar, varPart, _ref2;
      body = Block.wrap([this.body]);
      lastJumps = (_ref2 = last(body.expressions)) != null ? _ref2.jumps() : void 0;
      if (lastJumps && lastJumps instanceof Return) {
        this.returns = false;
      }
      source = this.range ? this.source.base : this.source;
      scope = o.scope;
      name = this.name && this.name.compile(o, LEVEL_LIST);
      index = this.index && this.index.compile(o, LEVEL_LIST);
      if (name && !this.pattern) {
        scope.find(name, {
          immediate: true
        });
      }
      if (index) {
        scope.find(index, {
          immediate: true
        });
      }
      if (this.returns) {
        rvar = scope.freeVariable('results');
      }
      ivar = (this.range ? name : index) || scope.freeVariable('i');
      if (this.step && !this.range) {
        stepvar = scope.freeVariable("step");
      }
      if (this.pattern) {
        name = ivar;
      }
      varPart = '';
      guardPart = '';
      defPart = '';
      idt1 = this.tab + TAB;
      if (this.range) {
        forPart = source.compile(merge(o, {
          index: ivar,
          step: this.step
        }));
      } else {
        svar = this.source.compile(o, LEVEL_LIST);
        if ((name || this.own) && !IDENTIFIER.test(svar)) {
          defPart = "" + this.tab + (ref = scope.freeVariable('ref')) + " = " + svar + ";\n";
          svar = ref;
        }
        if (name && !this.pattern) {
          namePart = "" + name + " = " + svar + "[" + ivar + "]";
        }
        if (!this.object) {
          lvar = scope.freeVariable('len');
          forVarPart = ("" + ivar + " = 0, " + lvar + " = " + svar + ".length") + (this.step ? ", " + stepvar + " = " + (this.step.compile(o, LEVEL_OP)) : '');
          stepPart = this.step ? "" + ivar + " += " + stepvar : "" + ivar + "++";
          forPart = "" + forVarPart + "; " + ivar + " < " + lvar + "; " + stepPart;
        }
      }
      if (this.returns) {
        resultPart = "" + this.tab + rvar + " = [];\n";
        returnResult = "\n" + this.tab + "return " + rvar + ";";
        body = Push.wrap(rvar, body);
      }
      if (this.guard) {
        body = Block.wrap([new If(this.guard, body)]);
      }
      if (this.pattern) {
        body.expressions.unshift(new Assign(this.name, new Literal("" + svar + "[" + ivar + "]")));
      }
      defPart += this.pluckDirectCall(o, body);
      if (namePart) {
        varPart = "\n" + idt1 + namePart + ";";
      }
      if (this.object) {
        forPart = "" + ivar + " in " + svar;
        if (this.own) {
          guardPart = "\n" + idt1 + "if (!" + (utility('hasProp')) + ".call(" + svar + ", " + ivar + ")) continue;";
        }
      }
      body = body.compile(merge(o, {
        indent: idt1
      }), LEVEL_TOP);
      if (body) {
        body = '\n' + body + '\n';
      }
      return "" + defPart + (resultPart || '') + this.tab + "for (" + forPart + ") {" + guardPart + varPart + body + this.tab + "}" + (returnResult || '');
    };
    For.prototype.pluckDirectCall = function(o, body) {
      var base, defs, expr, fn, idx, ref, val, _len, _ref2, _ref3, _ref4, _ref5, _ref6, _ref7;
      defs = '';
      _ref2 = body.expressions;
      for (idx = 0, _len = _ref2.length; idx < _len; idx++) {
        expr = _ref2[idx];
        expr = expr.unwrapAll();
        if (!(expr instanceof Call)) {
          continue;
        }
        val = expr.variable.unwrapAll();
        if (!((val instanceof Code) || (val instanceof Value && ((_ref3 = val.base) != null ? _ref3.unwrapAll() : void 0) instanceof Code && val.properties.length === 1 && ((_ref4 = (_ref5 = val.properties[0].name) != null ? _ref5.value : void 0) === 'call' || _ref4 === 'apply')))) {
          continue;
        }
        fn = ((_ref6 = val.base) != null ? _ref6.unwrapAll() : void 0) || val;
        ref = new Literal(o.scope.freeVariable('fn'));
        base = new Value(ref);
        if (val.base) {
          _ref7 = [base, val], val.base = _ref7[0], base = _ref7[1];
        }
        body.expressions[idx] = new Call(base, expr.args);
        defs += this.tab + new Assign(ref, fn).compile(o, LEVEL_TOP) + ';\n';
      }
      return defs;
    };
    return For;
  })();
  exports.Switch = Switch = (function() {
    __extends(Switch, Base);
    function Switch(subject, cases, otherwise) {
      this.subject = subject;
      this.cases = cases;
      this.otherwise = otherwise;
    }
    Switch.prototype.children = ['subject', 'cases', 'otherwise'];
    Switch.prototype.isStatement = YES;
    Switch.prototype.jumps = function(o) {
      var block, conds, _i, _len, _ref2, _ref3, _ref4;
      if (o == null) {
        o = {
          block: true
        };
      }
      _ref2 = this.cases;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        _ref3 = _ref2[_i], conds = _ref3[0], block = _ref3[1];
        if (block.jumps(o)) {
          return block;
        }
      }
      return (_ref4 = this.otherwise) != null ? _ref4.jumps(o) : void 0;
    };
    Switch.prototype.makeReturn = function() {
      var pair, _i, _len, _ref2, _ref3;
      _ref2 = this.cases;
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        pair = _ref2[_i];
        pair[1].makeReturn();
      }
      if ((_ref3 = this.otherwise) != null) {
        _ref3.makeReturn();
      }
      return this;
    };
    Switch.prototype.compileNode = function(o) {
      var block, body, code, cond, conditions, expr, i, idt1, idt2, _i, _len, _len2, _ref2, _ref3, _ref4, _ref5;
      idt1 = o.indent + TAB;
      idt2 = o.indent = idt1 + TAB;
      code = this.tab + ("switch (" + (((_ref2 = this.subject) != null ? _ref2.compile(o, LEVEL_PAREN) : void 0) || false) + ") {\n");
      _ref3 = this.cases;
      for (i = 0, _len = _ref3.length; i < _len; i++) {
        _ref4 = _ref3[i], conditions = _ref4[0], block = _ref4[1];
        _ref5 = flatten([conditions]);
        for (_i = 0, _len2 = _ref5.length; _i < _len2; _i++) {
          cond = _ref5[_i];
          if (!this.subject) {
            cond = cond.invert();
          }
          code += idt1 + ("case " + (cond.compile(o, LEVEL_PAREN)) + ":\n");
        }
        if (body = block.compile(o, LEVEL_TOP)) {
          code += body + '\n';
        }
        if (i === this.cases.length - 1 && !this.otherwise) {
          break;
        }
        expr = this.lastNonComment(block.expressions);
        if (expr instanceof Return || (expr instanceof Literal && expr.jumps() && expr.value !== 'debugger')) {
          continue;
        }
        code += idt2 + 'break;\n';
      }
      if (this.otherwise && this.otherwise.expressions.length) {
        code += idt1 + ("default:\n" + (this.otherwise.compile(o, LEVEL_TOP)) + "\n");
      }
      return code + this.tab + '}';
    };
    return Switch;
  })();
  exports.If = If = (function() {
    __extends(If, Base);
    function If(condition, body, options) {
      this.body = body;
      if (options == null) {
        options = {};
      }
      this.condition = options.type === 'unless' ? condition.invert() : condition;
      this.elseBody = null;
      this.isChain = false;
      this.soak = options.soak;
    }
    If.prototype.children = ['condition', 'body', 'elseBody'];
    If.prototype.bodyNode = function() {
      var _ref2;
      return (_ref2 = this.body) != null ? _ref2.unwrap() : void 0;
    };
    If.prototype.elseBodyNode = function() {
      var _ref2;
      return (_ref2 = this.elseBody) != null ? _ref2.unwrap() : void 0;
    };
    If.prototype.addElse = function(elseBody) {
      if (this.isChain) {
        this.elseBodyNode().addElse(elseBody);
      } else {
        this.isChain = elseBody instanceof If;
        this.elseBody = this.ensureBlock(elseBody);
      }
      return this;
    };
    If.prototype.isStatement = function(o) {
      var _ref2;
      return (o != null ? o.level : void 0) === LEVEL_TOP || this.bodyNode().isStatement(o) || ((_ref2 = this.elseBodyNode()) != null ? _ref2.isStatement(o) : void 0);
    };
    If.prototype.jumps = function(o) {
      var _ref2;
      return this.body.jumps(o) || ((_ref2 = this.elseBody) != null ? _ref2.jumps(o) : void 0);
    };
    If.prototype.compileNode = function(o) {
      if (this.isStatement(o)) {
        return this.compileStatement(o);
      } else {
        return this.compileExpression(o);
      }
    };
    If.prototype.makeReturn = function() {
      this.body && (this.body = new Block([this.body.makeReturn()]));
      this.elseBody && (this.elseBody = new Block([this.elseBody.makeReturn()]));
      return this;
    };
    If.prototype.ensureBlock = function(node) {
      if (node instanceof Block) {
        return node;
      } else {
        return new Block([node]);
      }
    };
    If.prototype.compileStatement = function(o) {
      var body, child, cond, exeq, ifPart;
      child = del(o, 'chainChild');
      exeq = del(o, 'isExistentialEquals');
      if (exeq) {
        return new If(this.condition.invert(), this.elseBodyNode(), {
          type: 'if'
        }).compile(o);
      }
      cond = this.condition.compile(o, LEVEL_PAREN);
      o.indent += TAB;
      body = this.ensureBlock(this.body).compile(o);
      if (body) {
        body = "\n" + body + "\n" + this.tab;
      }
      ifPart = "if (" + cond + ") {" + body + "}";
      if (!child) {
        ifPart = this.tab + ifPart;
      }
      if (!this.elseBody) {
        return ifPart;
      }
      return ifPart + ' else ' + (this.isChain ? (o.indent = this.tab, o.chainChild = true, this.elseBody.unwrap().compile(o, LEVEL_TOP)) : "{\n" + (this.elseBody.compile(o, LEVEL_TOP)) + "\n" + this.tab + "}");
    };
    If.prototype.compileExpression = function(o) {
      var alt, body, code, cond;
      cond = this.condition.compile(o, LEVEL_COND);
      body = this.bodyNode().compile(o, LEVEL_LIST);
      alt = this.elseBodyNode() ? this.elseBodyNode().compile(o, LEVEL_LIST) : 'void 0';
      code = "" + cond + " ? " + body + " : " + alt;
      if (o.level >= LEVEL_COND) {
        return "(" + code + ")";
      } else {
        return code;
      }
    };
    If.prototype.unfoldSoak = function() {
      return this.soak && this;
    };
    return If;
  })();
  Push = {
    wrap: function(name, exps) {
      if (exps.isEmpty() || last(exps.expressions).jumps()) {
        return exps;
      }
      return exps.push(new Call(new Value(new Literal(name), [new Access(new Literal('push'))]), [exps.pop()]));
    }
  };
  Closure = {
    wrap: function(expressions, statement, noReturn) {
      var args, call, func, mentionsArgs, meth;
      if (expressions.jumps()) {
        return expressions;
      }
      func = new Code([], Block.wrap([expressions]));
      args = [];
      if ((mentionsArgs = expressions.contains(this.literalArgs)) || expressions.contains(this.literalThis)) {
        meth = new Literal(mentionsArgs ? 'apply' : 'call');
        args = [new Literal('this')];
        if (mentionsArgs) {
          args.push(new Literal('arguments'));
        }
        func = new Value(func, [new Access(meth)]);
      }
      func.noReturn = noReturn;
      call = new Call(func, args);
      if (statement) {
        return Block.wrap([call]);
      } else {
        return call;
      }
    },
    literalArgs: function(node) {
      return node instanceof Literal && node.value === 'arguments' && !node.asKey;
    },
    literalThis: function(node) {
      return (node instanceof Literal && node.value === 'this' && !node.asKey) || (node instanceof Code && node.bound);
    }
  };
  unfoldSoak = function(o, parent, name) {
    var ifn;
    if (!(ifn = parent[name].unfoldSoak(o))) {
      return;
    }
    parent[name] = ifn.body;
    ifn.body = new Value(parent);
    return ifn;
  };
  UTILITIES = {
    "extends": 'function(child, parent) {\n  for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }\n  function ctor() { this.constructor = child; }\n  ctor.prototype = parent.prototype;\n  child.prototype = new ctor;\n  child.__super__ = parent.prototype;\n  return child;\n}',
    bind: 'function(fn, me){ return function(){ return fn.apply(me, arguments); }; }',
    indexOf: 'Array.prototype.indexOf || function(item) {\n  for (var i = 0, l = this.length; i < l; i++) {\n    if (this[i] === item) return i;\n  }\n  return -1;\n}',
    hasProp: 'Object.prototype.hasOwnProperty',
    slice: 'Array.prototype.slice'
  };
  LEVEL_TOP = 1;
  LEVEL_PAREN = 2;
  LEVEL_LIST = 3;
  LEVEL_COND = 4;
  LEVEL_OP = 5;
  LEVEL_ACCESS = 6;
  TAB = '  ';
  IDENTIFIER_STR = "[$A-Za-z_\\x7f-\\uffff][$\\w\\x7f-\\uffff]*";
  IDENTIFIER = RegExp("^" + IDENTIFIER_STR + "$");
  SIMPLENUM = /^[+-]?\d+$/;
  METHOD_DEF = RegExp("^(?:(" + IDENTIFIER_STR + ")\\.prototype(?:\\.(" + IDENTIFIER_STR + ")|\\[(\"(?:[^\\\\\"\\r\\n]|\\\\.)*\"|'(?:[^\\\\'\\r\\n]|\\\\.)*')\\]|\\[(0x[\\da-fA-F]+|\\d*\\.?\\d+(?:[eE][+-]?\\d+)?)\\]))|(" + IDENTIFIER_STR + ")$");
  IS_STRING = /^['"]/;
  utility = function(name) {
    var ref;
    ref = "__" + name;
    Scope.root.assign(ref, UTILITIES[name]);
    return ref;
  };
  multident = function(code, tab) {
    return code.replace(/\n/g, '$&' + tab);
  };
}).call(this);

};require['./coffee-script'] = new function() {
  var exports = this;
  (function() {
  var Lexer, RESERVED, compile, fs, lexer, parser, path, vm, _ref;
  fs = require('fs');
  path = require('path');
  vm = require('vm');
  _ref = require('./lexer'), Lexer = _ref.Lexer, RESERVED = _ref.RESERVED;
  parser = require('./parser').parser;
  if (require.extensions) {
    require.extensions['.coffee'] = function(module, filename) {
      var content;
      content = compile(fs.readFileSync(filename, 'utf8'), {
        filename: filename
      });
      return module._compile(content, filename);
    };
  } else if (require.registerExtension) {
    require.registerExtension('.coffee', function(content) {
      return compile(content);
    });
  }
  exports.VERSION = '1.1.1';
  exports.RESERVED = RESERVED;
  exports.helpers = require('./helpers');
  exports.compile = compile = function(code, options) {
    var js;
    if (options == null) {
      options = {};
    }
    try {
      js = (parser.parse(lexer.tokenize(code))).compile(options);
      return js + '\n';
    } catch (err) {
      if (options.filename) {
        err.message = "In " + options.filename + ", " + err.message;
      }
      throw err;
    }
  };
  exports.tokens = function(code, options) {
    return lexer.tokenize(code, options);
  };
  exports.nodes = function(source, options) {
    if (typeof source === 'string') {
      return parser.parse(lexer.tokenize(source, options));
    } else {
      return parser.parse(source);
    }
  };
  exports.run = function(code, options) {
    var Module, mainModule;
    mainModule = require.main;
    mainModule.filename = process.argv[1] = options.filename ? fs.realpathSync(options.filename) : '.';
    mainModule.moduleCache && (mainModule.moduleCache = {});
    if (process.binding('natives').module) {
      Module = require('module').Module;
      mainModule.paths = Module._nodeModulePaths(path.dirname(options.filename));
    }
    if (path.extname(mainModule.filename) !== '.coffee' || require.extensions) {
      return mainModule._compile(compile(code, options), mainModule.filename);
    } else {
      return mainModule._compile(code, mainModule.filename);
    }
  };
  exports.eval = function(code, options) {
    var g, js, k, o, sandbox, v, _i, _len, _ref2;
    if (options == null) {
      options = {};
    }
    if (!(code = code.trim())) {
      return;
    }
    sandbox = options.sandbox;
    if (!sandbox) {
      sandbox = {
        require: require,
        module: {
          exports: {}
        }
      };
      _ref2 = Object.getOwnPropertyNames(global);
      for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
        g = _ref2[_i];
        sandbox[g] = global[g];
      }
      sandbox.global = sandbox;
      sandbox.global.global = sandbox.global.root = sandbox.global.GLOBAL = sandbox;
    }
    sandbox.__filename = options.filename || 'eval';
    sandbox.__dirname = path.dirname(sandbox.__filename);
    o = {};
    for (k in options) {
      v = options[k];
      o[k] = v;
    }
    o.bare = true;
    js = compile("_=(" + code + "\n)", o);
    return vm.runInNewContext(js, sandbox, sandbox.__filename);
  };
  lexer = new Lexer;
  parser.lexer = {
    lex: function() {
      var tag, _ref2;
      _ref2 = this.tokens[this.pos++] || [''], tag = _ref2[0], this.yytext = _ref2[1], this.yylineno = _ref2[2];
      return tag;
    },
    setInput: function(tokens) {
      this.tokens = tokens;
      return this.pos = 0;
    },
    upcomingInput: function() {
      return "";
    }
  };
  parser.yy = require('./nodes');
}).call(this);

};require['./browser'] = new function() {
  var exports = this;
  (function() {
  var CoffeeScript, runScripts;
  CoffeeScript = require('./coffee-script');
  CoffeeScript.require = require;
  CoffeeScript.eval = function(code, options) {
    return eval(CoffeeScript.compile(code, options));
  };
  CoffeeScript.run = function(code, options) {
    if (options == null) {
      options = {};
    }
    options.bare = true;
    return Function(CoffeeScript.compile(code, options))();
  };
  if (typeof window === "undefined" || window === null) {
    return;
  }
  CoffeeScript.load = function(url, callback) {
    var xhr;
    xhr = new (window.ActiveXObject || XMLHttpRequest)('Microsoft.XMLHTTP');
    xhr.open('GET', url, true);
    if ('overrideMimeType' in xhr) {
      xhr.overrideMimeType('text/plain');
    }
    xhr.onreadystatechange = function() {
      var _ref;
      if (xhr.readyState === 4) {
        if ((_ref = xhr.status) === 0 || _ref === 200) {
          CoffeeScript.run(xhr.responseText);
        } else {
          throw new Error("Could not load " + url);
        }
        if (callback) {
          return callback();
        }
      }
    };
    return xhr.send(null);
  };
  runScripts = function() {
    var coffees, execute, index, length, s, scripts;
    scripts = document.getElementsByTagName('script');
    coffees = (function() {
      var _i, _len, _results;
      _results = [];
      for (_i = 0, _len = scripts.length; _i < _len; _i++) {
        s = scripts[_i];
        if (s.type === 'text/coffeescript') {
          _results.push(s);
        }
      }
      return _results;
    })();
    index = 0;
    length = coffees.length;
    (execute = function() {
      var script;
      script = coffees[index++];
      if ((script != null ? script.type : void 0) === 'text/coffeescript') {
        if (script.src) {
          return CoffeeScript.load(script.src, execute);
        } else {
          CoffeeScript.run(script.innerHTML);
          return execute();
        }
      }
    })();
    return null;
  };
  if (window.addEventListener) {
    addEventListener('DOMContentLoaded', runScripts, false);
  } else {
    attachEvent('onload', runScripts);
  }
}).call(this);

};
  return require['./coffee-script']
}()