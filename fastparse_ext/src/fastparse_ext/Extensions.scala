package fastparse_ext

import fastparse._

trait Extensions {

  def Until(p: => P[_])(implicit ctx: P[_], ws: P[_] => P[Unit]): P[Unit] = {
    (!p ~ AnyChar).rep ~ &(p)
  }

  def UpTo[_: P, T](p: => P[T])(implicit ws: P[_] => P[Unit]): P[T] = {
    Until(p) ~ p
  }

  def Back(implicit p: P[Any]): P[Unit] = {
    p.freshSuccessUnit(p.index - 1)
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

  def Within[I](outer: => P[_], inner: P[_] => P[I], endAtOuter: Boolean = false)(
      implicit ctx: P[_],
      ws: P[_] => P[Unit]
  ): P[I] = {
    Within2(outer, inner, endAtOuter = endAtOuter).map(_._2)
  }

  def NotWithin[O](p: => P[O], inner: P[_] => P[_])(implicit ctx: P[_], ws: P[_] => P[Unit]): P[O] = {
    Within2(p, !inner(_), endAtOuter = true).map(_._1)
  }

  /**
    *
    * Constrains an inner parser to run only inside an outer parser range.
    *
    *  If outer matches, the parsing run is backtracked to outer's start position
    *  and inner is run from there but limited to read only up to outer's end position.
    *
    * @param outer A parser which will delimit the range for inner parser
    * @param inner A parser that begins at outer's start position but cannot read beyond its end.
    * @param endAtOuter If true the end position is that of outer, otherwise it will be the end position of inner.
    * @tparam O type of the outer parser output.
    * @tparam I type of the inner parser output.
    */
  def Within2[O, I](outer: => P[O], inner: P[_] => P[I], endAtOuter: Boolean = false)(
      implicit ctx: P[_],
      ws: P[_] => P[Unit]
  ): P[(O, I)] = {
    (Index ~ outer ~ Index).flatMap {
      case (fromIndex, outerOut, toIndex) =>
        SetIndex(fromIndex) ~
          withinIndex(fromIndex, toIndex, inner).map(innerOut => outerOut -> innerOut) ~
          (if (endAtOuter) SetIndex(toIndex) else Pass)
    }
  }

}
