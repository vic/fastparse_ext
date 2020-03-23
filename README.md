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
Until(parser: => P[_]): P[Unit]
```

> Moves the index just to just before `parser` succeeds.
>
>  Convenience for: `(!parser ~ AnyChar).rep`

#####  UpTo

```scala
UpTo[T](parser: => P[T]): P[T]
```

> Ignores anything up to `parser` success.
>
> Convenience for: `Until(parser) ~ parser`

#####  SetIndex

`SetIndex(index: Int)`

> Moves the parsing index to a certain position.
>
> _Warning:_ use with caution.


#####  Within

`Within[I](outer: => P[Unit], inner: P[_] => P[I]): P[I]`
`Within2[O,I](outer: => P[O], inner: P[_] => P[I]): P[(O, I)]`

> Constrains an inner parser to run only inside an outer parser range.
>
> If outer matches, the parsing run is backtracked to outer's start position
> and inner is run from there but limited to read only up to outer's end position.
> When inner parser is done, the current position is set to the end of outer parser.
>
> The result of Whithin2 is a tuple with the result of both parsers.
>
> Note that the inner parser must be given as a partial function.

See [tests](fastparse_ext/test/src/fastparse_ext/FastparseExtTest.scala) for runnable examples.

