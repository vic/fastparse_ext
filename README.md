# [FastParse](http://www.lihaoyi.com/fastparse/) extensions

![Main workflow](https://github.com/vic/fastparse_ext/workflows/Main%20workflow/badge.svg)
[![jitpack](https://jitpack.io/v/vic/fastparse_ext.svg)](https://jitpack.io/#vic/fastparse_ext)

Small utility extensions for fastparse.

## Usage

```scala
import fastparse_ext._
```

##### `Until(parser: => P[_]): P[Unit]`

Moves the index just to just before `parser` succeeds.

Convenience for: `(!parser ~ AnyChar).rep`

##### `UpTo[T](parser: => P[T]): P[T]`

Ignores anything up to `parser` success.

Convenience for: `Until(parser) ~ parser`

##### `SetIndex(index: Int)`

Moves the parsing index to a certain position.

*Warning:* use with caution.


##### `Within[O,I](outer: => P[O], inner: P[_] => P[I]): P[(O, I)]`

Constrains an inner parser to run only inside an outer parser range.

If outer matches, the parsing run is backtracked to outer's start position
and inner is run from there but limited to read only up to outer's end position.
When inner parser is done, the current position is set to the end of outer parser.
The result of parsing is a tuple with the result of both parsers.

NOTE: The `inner` parser is given partially applied to this function.

In example bellow, the `inner` parser cannot see the `]` char at boundary,
and would see an `End`(end-of-file) instead.

```scala
def outer[_: P] = Until("]" | End)
def inner[_: P] = AnyChar.rep
def parser[_:P] = UpTo("[") ~ Within(outer, inner(_))
```


See [tests](fastparse_ext/test/src/fastparse_ext/FastparseExtTest.scala) for runnable examples.

