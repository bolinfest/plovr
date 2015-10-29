[TOC]

A Bytecode Compiler for Soy
===========================

This package implements a bytecode compiler for the Soy language.  The high
level goals are to

 * Increase rendering performance over Tofu
 * Allow async rendering so that Soy can pause/resume rendering when
   * It encounters an unfinished future.
   * The output buffer is full.

The general strategy is to generate a new Java class for each Soy `{template}`.
Full details on how different pieces of Soy syntax map to Java code are detailed
below.

##Package design

The jbcsrc implementation is split across several packages.

 * `com.google.template.soy.jbcsrc`

   The base package contains the core compiler implementation and the public
   compiler entry point: `BytecodeCompiler`

 * `com.google.template.soy.jbcsrc.runtime`

   This package contains helper classes and utility routines that are only
   accessed by the generated code.  A lot of the `jbcsrc` runtime is actually
   defined in other soy packages (such as
   `com.google.template.soy.shared.internal.SharedRuntime` or
   `com.google.template.soy.shared.data`), when it is possible to share with
   Tofu.  So this package is really intended for jbcsrc specific functionality.

 * `com.google.template.soy.jbcsrc.api`

   This package contains the public api for rendering jbcsrc compiled templates
   via the `SoySauce` class.

 * `com.google.template.soy.jbcsrc.shared`

   This package contains functionality that is shared by the compiler and
   runtime, but is meant to be private to soy.

Background
----------
The Soy server side renderer is currently implemented as a
[recursive visitor](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/sharedpasses/render/RenderVisitor.java)
on the Soy AST. This implementation is expedient since the renderer uses the
same API as all the parse visitors and other ‘compiler passes’ and thus can
benefit from developer familiarity.  However, this design
makes it very difficult to perform even basic optimizations.  By contrast,
the JS implementation of Soy works by generating JS code and thus can
benefit from all the optimizations in the JS compiler and browser.
The new Python implementation will work in a similar way (generating Python
code).

Finally, Soy rendering is one of the last sources of blocking in modern servers.
Soy will block the request thread when coming across unfinished futures or when
the output buffer becomes full.  The current design of Soy rendering makes it
very difficult to move to a fully asynchronous rendering model.

Overview
--------

For each Soy template we will generate a Java class by generating bytecode
directly from the parse tree.  The Soy language is simple and all
the basic language constructs map directly into Java constructs.  For example
this template:

~~~
{template .foo}
  {@param p : string}
  {@param p2 : string}
  {$p}
  {$p2}
{/template}
~~~

could be implemented by a Java function like:

~~~
  void render(Appendable output, SoyRecord params) {
    params.getField("p").render(output);
    params.getField("p2").render(output);
  }
~~~

Which is, in fact, effectively the code that the current implementation
executes (it is just that between each method call there are many many visitor
operations).  So at least initially most of the benefit would be realized
simply by not traversing the AST.  However, this solution still doesn’t address
our concerns about asynchrony.

There are two kinds of asynchrony we will wish to handle:

  1. Asynchronous data.  Any piece of data passed into Soy is wrapped in a
    `SoyValueProvider`.  If the item is a [`Future`](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html),
    it is wrapped in a `SoyFutureValueProvider`. If `$p` above was passed in as
    a `Future`, then we would block during the `getField` method call.
  2. Asynchronous output.  In a modern HTTP server, it is desirable to handle
    slow clients (e.g. mobile devices). However, in the current Soy design if
    the server writes too fast it will either block the rendering thread
    (causing poor thread utilization) or it will buffer unbounded bytes in RAM.
    If we are buffering too much, it may be better for rendering to pause and
    for the request thread to serve another request.

Given these constraints, the above direct approach will not work.  So instead
we could generate something like this:

~~~
class Foo {
  int state = 0;
  StringData p;
  StringData p2;

  Result render(AdvisingAppendable output, SoyRecord params) {
    switch (state) {
      case 0:
        SoyValueProvider provider = params.getFieldProvider("p");
        Result r = provider.canResolve();
        if (r.type() != Result.Type.DONE) {
          return r;
        }
        p = (StringData) provider.resolve();
        p.render(output);
        state = 1;
        if (output.softLimitReached()) {
          return Result.limited();
        }
      case 1:
        provider = params.getFieldProvider("p2");
        Result r = provider.canResolve();
        if (r.type() != Result.Type.DONE) {
          return r;
        }
        p2 = (StringData) provider.resolve();
        p2.render(output);
        state = 2;
        if (output.softLimitReached()) {
          return Result.limited();
        }
      case 2:
        return Result.done();
      default:
        throw new AssertionError();
    }
  }
}
~~~

In this example, we are now checking whether the output is full (after every
write operation) and we are checking if the SoyValueProviders
can be 'resolved' without blocking prior to resolving.  Additionally, we are
storing resolved parameters in fields so that we don’t have to re-resolve them
when re-entering the method.

This is the heart of the proposal: to generate for each template a tiny state
machine that can be used to save and restore state up to the point of the last
‘detach’.  A sophisticated rendering client could then use these return types
to detach from the request thread or find other work to do while buffers are
being flushed or futures are completing.

This approach is similar to how `yield` generators are implemented in
C#/Python or how `async/await` are implemented in C#/Scala/Dart.

###Implementation strategy


All the examples below use Java code to demonstrate what the generated code
will look like.  However, the actual implementation will be using
[ASM](http://asm.ow2.org/) to generate bytecode directly.  This comes with a
number of pros and cons.

 * Pros
   * Small library. Fast code generation.
   * Greater control flow flexibility (bytecode GOTO is more powerful than a
     Java switch statement)
   * Can generate debugging information that points directly to the Soy
     template resources
   * Makes refresh-to-reload more straightforward than a source compiler
     based approach would be.
 * Cons
   * Few people are familiar with bytecode.  This may be a high
     barrier to entry for contributions.
   * Verbose/tedious! (we lose all the javac compiler magic that you normally
     get)
   * New compile time dependency for Soy (ASM library)

To demonstrate the control flow issues mentioned above, consider the following
example:

~~~
{template .foo}
  {@param p1 : [f: bool, v: list<string>]}
  {if $p1.f}
    {for $s in $p1.v}
      <div>{$s}</div>
    {/for}
  {/if}
{/template}
~~~

This is a simple template with a `for` loop inside an `if` statement.

To allow the renderer to suspend rendering after print statements or to
implement detaching when handling `$s` we would need to implement something like
this:

~~~
int index;
int state;

public Result render(SoyRecord params, Appendable output) throws IOException {
  while (true) {
    switch (state) {
      case 0:
        SoyRecord soyRecord = (SoyRecord) params.getField("p1");
        if (soyRecord.getField("f").coerceToBoolean()) {
          state = 1;
        } else {
          state = 3;
        }
        break;
      case 1:
        SoyListData vList =
            ((SoyListData) ((SoyRecord) params.getField("p1")).getField("v"));
        if (vList.length() > index) {
          output.append("<div>");
          state = 2;
        } else {
          state = 3;
        }
        break;
      case 2:
        SoyValueProvider s =
            ((SoyListData) ((SoyRecord) params.getField("p1")).getField("v"))
            .asJavaList()
            .get(index);
        Result resolvable = s.canResolve();
        if (resolvable.isDone()) {
          s.resolve().render(output);
          output.append("</div>");
          state = 1;
          index++;
        } else {
          return resolvable;
        }
        break;
      case 3:
        return Result.done();
    }
  }
}
~~~

We could generate code like this, but in doing so we would lose the major
benefits of source generation: human readability and debuggability.  So given
that, we have decided not to generate Java sources and instead to generate
bytecode directly.  For example, if Java had a `goto` keyword we could rewrite
the above as:

~~~
int index;
int state = 0;
public void render(SoyRecord params, Appendable output) throws IOException {
  goto state;
  L0:
  SoyRecord soyRecord = (SoyRecord) params.getField("p1");
  if (soyRecord.getField("f").coerceToBoolean()) {
    List<? extends SoyValueProvider> asJavaList =
        ((SoyListData) soyRecord.getField("v")).asJavaList();
    for (index = 0; index < asJavaList.size(); index++) {
      output.append("<div>");
      L1:
      SoyValueProvider s = asJavaList.get(index);
      Result r = s.canResolve();
      if (!s.isDone()) {
        state = 1;
      }
      s.resolve().render(output);
      output.append("</div>");
    }
  }
}
~~~

The strategy is to generate bytecode that looks like that.

Structure of Compiled Templates
--------------------------------------

For every Soy template we compile a number of classes to implement our
functionality:

 * A [CompiledTemplate](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/jbcsrc/api/CompiledTemplate.java)
   subclass. This has a single `render` method that will render the template
 * A [CompiledTemplate.Factory](api/CompiledTemplate.java) subclass. This
   provides a non-reflective mechanism for constructing CompiledTemplate
   instances
 * A [SoyAbstractCachingValueProvider](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/data/SoyAbstractCachingValueProvider.java)
   subclass for each [CallParamValueNode](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/soytree/CallParamValueNode.java)
   and each [LetValueNode](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/soytree/LetValueNode.java).
   These allow us to implement 'lazy' `{let ...}` and `{param ...}` statements.
 * A [RenderableThunk](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/data/internal/RenderableThunk.java)
   subclass for each [CallParamContentNode](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/soytree/CallParamContentNode.java)
   and each [LetContentNode](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/soytree/LetContentNode.java).
   These allow us to implement 'lazy' `{let ...}` and `{param ...}` statements
   that render content blocks.


### Helper Objects and APIs

Our implementation will depend on a few new helper objects.

#### AdvisingAppendable

  A simple Appendable subtype that exposes an additional method ‘boolean
softLimitReached()’.  This method can be queried to see if writes should be
suspended.

#### Result

A value type that indicates the result of a rendering operation. The 3 kinds
are: Result.done(), meaning that rendering completed fully; Result.limited(),
meaning that the output informed us that the limit was reached;
Result.detach(Future), meaning that rendering found an incomplete future and is
detaching on that.

#### Context

A somewhat catch-all object for propagating cross cutting data items.  Via the
Context object, templates should be able to access:

  * The SoyMessageBundle
  * SoyFunction instances
  * PrintDirective instances
  * renaming maps (css, xid)
  * EscapingDirective instances
  * IJ params

We will propagate this as a single object from the top level (directly through
the render() calls), because this object will be constant per render.

As future work we may want to consider turning many of these into compiler
intrinsics.  For example, instead of looking up the PrintDirective each time we
need to apply it, we could stash the PrintDirective reference in a static field
at class initialization time (ditto for escaping, translations, renaming).

Additionally we will enhance some core APIs to expose additional information:

#### SoyValue

void SoyValue.render(Appendable) will change to ‘Result
render(AdvisingAppendable)’. That will allow individual values to detach
mid-render.  Most Soy values will have trivial implementations of this method,
but for our [lazy transclusion values](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/data/internal/RenderableThunk)
we will need this.

#### SoyValueProvider

SoyValueProviders are used to encapsulate values that aren’t done yet.  This
includes lazy expressions as well as Futures.  In order to identify and
conditionally resolve these providers we will need a new ‘Result canResolve()’
method.

Compilation Strategy
--------------------

The main details of the design will be a discussion of exactly how the code of
the render method is generated.  The Soy language is logically divided into two
parts: The expression language and the command language.  The expression
language is basically everything inside of a set of {}’s while the command
language is everything outside of it.  Since the expression language is the
simplest part, we will start there.

### Compiling Soy expressions

Soy has a relatively simple expression language divided into 4 main parts:

  1. Literals: `1`, `'foo'`, `[1,2,3,4]`, `['k': 'v', 'k2': 'v2']`
  2. Operators: `+`, `-`, `==`, `?:` etc.
  3. Function invocations: `index()`, `isFirst()`, etc.
  4. Data access expressions: `$foo`, `$foo.bar.baz`, `$foo[$key]`, `$foo[1]`

Since expressions are (for the most part) where data access occurs, it is in
the expressions that we must handle resolving SoyValueProviders to SoyValues
and optionally detaching if we come across a future.  One simplifying
assumption we will make is that Soy expressions are idempotent and sufficiently
cheap (relative to a detaching operation) that it is fine to re-execute an
expression when re-attaching.

#### Operators, literals and function invocations

These 3 parts of the expression language translate quite directly and do not
interact with either the output stream or any data that may contain futures and
therefore do not have any complex control flow requirements.

The biggest optimization opportunities exists in this part of the
implementation.  Soy tracks a fair bit of type information in order to flag
issues at parse time as well as to generate type casts in the JS implementation.
However, the Java runtime hasn’t been able to take advantage of any benefits
from specialization due these types.  For example, the expression `$a + $b` has
somewhat complex semantics since Soy has essentially the javascript rules for
the `+` operator.  So in order to execute the operator we need to know if either
of the parameters is a string or a number and then decide to concat or sum.
The current Java implementation is
[here](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/sharedpasses/render/EvalVisitor.java)
and implements this by a sequence of explicit type checks at runtime.  This is
unfortunate since in a large number of cases the types are fixed at parse time.
Because we will generate code for each `+` operator we can specialize the
implementation based on the types of the subexpressions and move many of these
type checks from runtime to compile time.

Finally, another obvious optimization in expression evaluation is the removal
of SoyValue boxes.  If an expression is fully typed, then we could eliminate
all the SoyValue wrappers and instead operate directly on raw longs, doubles
and Strings.


#### Data access

When coming across a data reference we will need to generate code to
conditionally resolve it.  Resolution may mean one of several things:

There are two kinds of data access:

  * VarRef
     * For each of these we will generate a field to hold the resolved SoyValue
     * To access, we first check if the field has been initialized, if it hasn’t
       been we then evaluate the variable
        * For params we fetch it from the params record (or the ij record)
        * For let-expression references we evaluate the generated provider (a
          class field)
        * For let-content references, we simply load the field
     * If the provider is resolvable (via the yet to be defined canResolve
       method), then we resolve(), store to the field and continue.
     * If is isn’t resolvable, we calculate a Result object and return.
     * N.B. we may be able to apply some definite assignment analysis to
       eliminate some checks.  For example, if it is definitely not the first
       access, then we can just read the field, no need to generate any code
       beyond that.
  * DataAccess
     * These are for accessing subfields, map entries or list items
     * There are no fields to check so we grab the item as a provider, check
       canResolve and conditionally return.

For example, a VarRef data access ‘$foo’ referring to a template param may be
implemented as:

~~~
SoyValue fooValue = fooField;
if (fooValue == null) {
  // first access
case N:
  SoyValueProvider valueProvider =
      params.getFieldProvider("foo");
  Result r = valueProvider.canResolve();
  if (r.type() != Type.DONE) {
    return r;
  }
  fooValue = fooField = valueProvider.resolve();
}
state = N +1;
~~~

Obvious optimizations of this code may include:

  * eliminating the field if the var is only accessed once
  * not checking for `null` (or generating a new state) if this is provably not
    the first reference in the template

DataAccess nodes will be similar with the caveat that they will be referencing
subfields of other SoyValues instead of from the params.

## Compiling Soy commands

Soy commands are the most complex part of the design.  The may contain complex
control flow or define complex objects.  The next section will go through all
the Soy commands and discuss exactly how they would be implemented.

### RAW\_TEXT\_NODE

Trivial compilation. Simply translates to:

~~~
case N:
output.append(RAW\_TEXT);
state = N + 1;
if (output.isSoftLimitReached()) {
  return Result.limited();
}
~~~

So for each RAW\_TEXT command we will need to allocate a state and check the
output for being limited after writing.

Issues:

  * The text constant may be very large.  We may want to rewrite as multiple
    write operations if the constant is very large (>1K? >4K?)
  * What about small writes?  maybe we should attempt to eliminate soft limit
    checks if we have only written a few characters (<10 ?)

### PRINT\_NODE, PRINT\_DIRECTIVE\_NODE

The general form of a print command is

~~~
{print <expr>|<directive1>|<directive2>...}
~~~

(Note that the ‘print’ command name is optional and often omitted)

To evaluate this statement we will first use the expression compiler to
generate code that produces a SoyValue object, then we will invoke code that
looks like this:

~~~
N:
expr = …;
expr = context.getPrintDirective("directive1").apply(expr);
expr = context.getPrintDirective("directive2").apply(expr);
state = N + 1;
case N+1:
Result r = expr.render(output);
if (r.type() != Type.DONE) {
  return r;
}
state = N + 2;
~~~

### XID\_NODE, CSS\_NODE

These nodes are truly trivial.  In fact it was probably a mistake to implement
them as commands instead of just a `SoyFunction`.

The one complexity here would be if we want to maintain the single-element
cache approach currently used to optimize renaming.
See [CssNode.renameCache](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/soytree/CssNode.java&l=83)
. This is one of the few examples of an optimization that would be lost in the
redesign.  If we thought it was important we could optimize this (via the same
technique, or possibly by using integer keys and array lookups instead of hash
lookups, which may be simpler/smaller/faster).

###LET\_VALUE\_NODE,LET\_CONTENT\_NODE

Let statements are more complex than you might think!  Due to our desire for
laziness we cannot simply evaluate and stash in a field.  Instead we generate a
class for each `{let}` command.  For let value nodes, we will generate a
`SoyValueProvider` subclass, for `SoyContentNodes` we will generate a
[RenderableThunk](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/data/internal/RenderableThunk.java&l=28)
subclass that will be used to declare a
[StringData.LazyString](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/data/restricted/StringData.java)
or a [SantizedContent.LazyContent](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/data/SanitizedContent.java&l=41)
SoyValue. For example, assume that the template `ns.owner` declares this let
variable `{let $foo : $a + 1 /}`, will generate the following code:

~~~
private static final class let$foo_1 extends AbstractLetValue {
  private final ns$$owner owner;
  private int state;
  let$foo_1(ns$$owner owner) {
    this.owner = owner;
  }
  @Override protected Result doCanResolve() {
     // evaluate expression using normal rules
     // finally take the resolved expression and
     // assign to the value field (defined by our
     // super class)
     this.value = expr;
     return Result.done();
   }
}
~~~

Then the owner class will declare a field of type let$$foo\_1 and initialize it
at the normal declaration point.  Let-content nodes will be very similar with
the caveat that the base class will be different (RenderableThunk).  Unlike
params, the fields for let nodes need to be cleared (nulled out), when they go
out of scope.  This is to sure that they behave properly in loops (re-evaluated
per iteration) and it will also make sure we don’t pin their values in memory
too long.

### IF\_NODE, IF\_COND\_NODE, IF\_ELSE\_NODE

If conditions will translate quite naturally since the Soy semantics and the
java semantics are identical.

### SWITCH\_NODE, SWITCH\_CASE\_NODE, SWITCH\_DEFAULT\_NODE

The behavior of switch is fairly similar to a sequence of if and else-if
statements (and will be implemented just like that), however, because each
comparison references the same SoyValue and we could detach mid-comparison.  We
need to store the switch variable in a field.

Note: this analysis is based on the assumption that switch case statements may
be arbitrary expressions.  The AST and current implementation imply that they
are, but I’m not sure.  If they are constants, then the implementation could
resolve to something like a Java switch() statement, which would be preferable.
 Even if they don’t have to be constants, we could still optimize for the case
statements that are constants.

### FOREACH\_NODE, FOREACH\_NONEMPTY\_NODE, FOREACH\_IFEMPTY\_NODE, FOR\_NODE

For loops are also pretty straightforward with 2 important caveats.

  1. The loop variable, the loop collection, and the current index all need to
     be stored as fields so that the loop state can be recovered when
     reattaching.
  2. The non-empty and if-empty blocks can be implemented via simple loop
     unrolling.

### LOG\_NODE

A `{log}...{/log}` statement is simply a collection of Soy statements that
should render to System.out instead of the user supplied output buffer.  This is
implemented by simply generating code for all the child statements (as normal),
but replace references to ‘output’ with a simply adapter of System.out to the
AdvisingAppendable interface.  Additionally, we can skip generating any and all
softLimitChecks since System.out doesn’t have an implementation.

### DEBUGGER\_NODE

No op implementation.  We can generate a label with a line number here, but
that is about it.

### CALL\_PARAM\_VALUE\_NODE,CALL\_PARAM\_CONTENT\_NODE

See the section on [`{let}` commands](#let_value_node_let_content_node),
`{param}` commands will use an identical strategy for defining the values.
Each one will be stored in a SoyRecord that will be passed as an argument to
the next template.

N.B. None of the `{param}` values or the SoyRecord holding them for a call will
be stored as fields, see the section on
[template calling](#call_basic_nodecall_delegate_node) for a detailed example.


### CALL\_BASIC\_NODE,CALL\_DELEGATE\_NODE

There are several styles of calls for now I will demonstate a normal call with
no data param. e.g.

`{call .foo}{param bar : 1 /}{/call}`

This will generate code that looks like:

~~~
private ns$$foo fooTemplate;
  case N:
    SoyEasyDict record = new SoyEasyDict();
    record.put("bar", <generate bar param>);
    fooTemplate = new ns$$foo(record);
    state = N+1;
  case N+1:
    Result r = fooTemplate.render(output, context);
    if (r.type() != Type.DONE) {
      return r;
    }
~~~

parameters like `data = "all"` or `data="$expr"` will simply modify how the
record is initialized.

TODO(lukes): fill in details on delcall template selection

### MSG\_NODE,MSG\_FALLBACK\_GROUP\_NODE

TODO(lukes):  The way Soy deals with translations is a bit of a mystery to me.
I think we will need to generate code that walks through a SoyMsg object.
Maybe gboyer@ can explain it all to me.

## Compiling Soy Types

The Soy type system mostly follows the JS type system (as understood by the js
compiler).  Notably, it doesn’t really fit into the Java type system.  The
current renderer manages this disconnect via the
[SoyType type hierarchy](https://github.com/google/closure-templates/blob/master/java/src/com/google/template/soy/types/SoyType.java)
and a plethora of runtime type checks.  Currently the runtime checks are a
combination of explicit SoyType operations and the Java <b>instanceof</b>
operator.  This will cause a variety of problems for the compiler:

  * Soy has a number of places where it checks types.  We will need to generate
    code that performs these checks by generating `checkcast` instructions.
  * Soy has union types.  These are not (easily) representable in the Java type
    system, so every union typed variable will most likely by represented by a
    static `SoyValue` type
  * Soy has both `null` values and a `null` type
  * The Soy type system is pluggable (notably for protos).
  * The soy type system is not completely accurate (e.g. nullability is not 
    trustable)

Due to these issues we take a conservative approach to how we make use of the
type system.  The key principals we will use are:

  * If the user declared it, we should enforce it. (with `checkcast` operations)
    This way type errors will get caught early and often.
  * Nullability information from the type system cannot be relied on.  For 
    example, if `map` contains integer values, then `$map['key]'` will be
    assigned the type `int` by the type system, but `int|null` is probably more
    accurate since we don't know whether or not the key exists.  So in general
    we need to be careful when dealing with possibly null expressions.  To deal
    with this we have our own concept of nullability (`Expression.isNullable()`).

## Non-Functional Requirements

Cross cutting architectural issues that influence overall design choices.

  * Refresh to reload.  Soy development mode should not change at all.
     * This should be pretty straightforward with bytecode generation since it
       is just as hard to use it to generate a Jar vs. loading the classes into
       the current VM.
     * For reloading we would just reparse and recompile into a different (heap
        sourced) class loader.  There is a risk that we will leak permgen, so we
        should write leak tests for the classloaders.
  * Stack traces are readable!  Currently the tofu renderer does a lot of work
    to generate stack traces that point to the templates.  We should do the
    same.
  * Efficiency!  This new system should be significantly faster (>20% cpu
    reduction) than the current approach.
  * Reasonable permgen usage.  This will add a lot of new classes to the JVM
    which may consume too much permgen.  In general, we are fine with trading
    server ram for cpu, but there are limits.


## Compatibility

The new implementation will strive for full compatibility with the existing
renderer behavior, unless such compatibility requires reimplementing a bug.

TODO(lukes): document known incompatiblities

A Bytecode Primer
-----------------

### Definitions
 * Runtime/operand stack - The implicit runtime stack of the virtual machine
 * Basic Block - a sequence of instructions with no branches
 * Frame - the set of values on the runtime stack and in the local variable
   table


### Stack Machine
Java bytecode is a 'stack machine', this means that all operators perform some
kind of operations on an implicit runtime stack.  For example, the opcode `IADD`
will pop 2 `int` values off the runtime stack and put their sum back onto the
stack.  Bytecode also has a local variable table that can be used to store
named (well indexed) values.  However, there are no opcodes that can operate
on local variables (other that pushing them onto the stack).

### Types
The Java bytecode type system mostly maps to the normal Java type system with a
notable exception that boolean is not a type, boolean is just any int,
`0 == false` and `non zero == true`. However, types impose some important
constraints on how bytecode can be written. Every value on the runtime stack has
a type associated, as well as every local variable.  At any instruction there
is a notion of an active 'frame'. For a basic block, frames are trivial to
maintain and update.  However, for branch target instructions (jump locations),
the frames at each branching instruction must be identical.  The jvm has
dedicated opcodes to manipulate frame state for branch targets.  For the most
part ASM will calculate these, but errors due to inconsistent frames are easy
to introduce (and do not have pretty failures).

### Opcodes

A good resource for figuring out what each jvm opcode does is from the
[java spec](http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html)

### ASM Tips

The asm library has a lot of benefits. It is small, blazing fast, and well
supported.  However, it can be very error prone to generate bytecode.  In 
particular, asm has no error checking, so when you make a mistake the errors
produced can be inscrutable.  Here is what I know:

 * NegativeArraySizeException is thrown from MethodWriter.visitMaxes.  The most
   likely explanation is that you have accidentally popped too many items off
   the runtime stack.  Look for stray POP instructions, or using the wrong
   branch instruction (IF_IEQ pops two ints, IFEQ pops one).
