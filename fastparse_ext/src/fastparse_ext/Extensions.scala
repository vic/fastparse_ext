package fastparse_ext

import fastparse.{P, ParserInput, AnyChar, Index}

trait Extensions {
  def Until[_: P](p: => P[_])(implicit whitespace: P[_] => P[Unit]): P[Unit] = {
    (!p ~ AnyChar).rep
  }

  def UpTo[_: P, T](p: => P[T])(implicit whitespace: P[_] => P[Unit]): P[T] = {
    Until(p) ~ p
  }

  def SetIndex(index: Int)(implicit p: P[Any]): P[Unit] = {
    p.freshSuccessUnit(index)
  }

  private def inputWithinIndex(fromIndex: Int, toIndex: Int, input: ParserInput): ParserInput = new ParserInput {
    def assertWithinRange[V](index: Int)(v: => V): V = {
      Predef.assert(isReachable(index), s"Expected index $index to be in input range $fromIndex..$toIndex")
      v
    }

    override def apply(index: Int): Char = assertWithinRange(index)(input.apply(index))

    override def dropBuffer(index: Int): Unit = input.dropBuffer(index)

    override def slice(from: Int, until: Int): String = input.slice(from, until)

    override def length: Int = input.length

    override def innerLength: Int = input.innerLength

    override def isReachable(index: Int): Boolean = index >= fromIndex && index < toIndex

    override def checkTraceable(): Unit = input.checkTraceable()

    override def prettyIndex(index: Int): String = input.prettyIndex(index)
  }

  private def withInputRun[T](parserInput: ParserInput, parser: P[_] => P[T])(implicit run: P[_]): P[T] = {
    val newInputRun = new P[T](
      input = parserInput,
      startIndex = run.startIndex,
      originalParser = run.originalParser,
      traceIndex = run.traceIndex,
      instrument = run.instrument,
      failureTerminalAggregate = run.failureTerminalAggregate,
      failureGroupAggregate = run.failureGroupAggregate,
      shortParserMsg = run.shortParserMsg,
      lastFailureMsg = run.lastFailureMsg,
      failureStack = run.failureStack,
      isSuccess = run.isSuccess,
      logDepth = run.logDepth,
      index = run.index,
      cut = run.cut,
      successValue = run.successValue,
      verboseFailures = run.verboseFailures,
      noDropBuffer = run.noDropBuffer,
      misc = run.misc
    )
    val result = parser(newInputRun)
    run.failureTerminalAggregate = result.failureTerminalAggregate
    run.failureGroupAggregate = result.failureGroupAggregate
    run.shortParserMsg = result.shortParserMsg
    run.lastFailureMsg = result.lastFailureMsg
    run.failureStack = result.failureStack
    run.isSuccess = result.isSuccess
    run.logDepth = result.logDepth
    run.index = result.index
    run.cut = result.cut
    run.successValue = result.successValue
    run.verboseFailures = result.verboseFailures
    run.noDropBuffer = result.noDropBuffer
    run.asInstanceOf[P[T]]
  }

  private def withinIndex[T](fromIndex: Int, toIndex: Int, p: P[_] => P[T])(implicit run: P[_]): P[T] = {
    withInputRun(inputWithinIndex(fromIndex, toIndex, run.input), p)
  }

  def Within[O, I](outer: => P[O], inner: P[_] => P[I])(implicit ctx: P[_], ws: P[_] => P[Unit]) : P[(O, I)] = {
    (Index ~ outer ~ Index).flatMap { case (fromIndex, outerOut, toIndex) =>
      SetIndex(fromIndex) ~
        withinIndex(fromIndex, toIndex, inner).map(innerOut => outerOut -> innerOut) ~
        SetIndex(toIndex)
    }
  }

}
