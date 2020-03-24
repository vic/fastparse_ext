# [FastParse](http://www.lihaoyi.com/fastparse/) extensions

![Main workflow](https://github.com/vic/fastparse_ext/workflows/Main%20workflow/badge.svg)
[![jitpack](https://jitpack.io/v/vic/fastparse_ext.svg)](https://jitpack.io/#vic/fastparse_ext)

Small utility extensions for fastparse.

## Usage

```scala
import fastparse_ext._
```

#####  Until

```scala
def Until(parser: => P[_]): P[Unit]
```

> Moves the index just to just before `parser` succeeds.
>
>  Convenience for: `(!parser ~ AnyChar).rep ~ &(parser)`

#####  UpTo

```scala
def UpTo[T](parser: => P[T]): P[T]
```

> Ignores anything up to `parser` success.
>
> Convenience for: `(!parser ~ AnyChar).rep ~ parser`

#####  SetIndex

```scala
def SetIndex(index: Int)
```

> Moves the parsing index to a certain position.
>
> _Warning:_ use with caution.


#####  Within

```scala
def Within[I](outer: => P[_], inner: P[_] => P[I], endAtOuter: Boolean = false): P[I]
def Within2[O,I](outer: => P[O], inner: P[_] => P[I], endAtOuter: Boolean = false): P[_] => P[I]): P[(O, I)]
def NotWithin[O](outer: => P[O], inner: P[_] => P[I]): P[_] => P[_]): P[O]
```

> Constrains an inner parser to run only inside an outer parser range.
>
> *IMPORTANT NOTE*
> 
> Inner parser is always passed as partially applied function, because
> it will be run with a new index-delimited input.
>
> Define your inner parser as a function `def someInner[_: P] = ...`
> and provide it partially applied: `someInner(_)`
>
> If `outer` succeeds, the `inner` parser is run from outer's start position,
> but it can never get past outer's end position.
> By default, the end position is that of the `inner` parser, unless `endAtOuter = true` is 
> explicitly given.
>
> The result of Whithin2 is a tuple with the result of both parsers.
>
> NotWithin succeeds only when inner fails inside of outer.

See [tests](fastparse_ext/test/src/fastparse_ext/FastparseExtTest.scala) for runnable examples.

