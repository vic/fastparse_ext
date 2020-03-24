package fastparse_ext
import utest._

object FastparseExtTest extends TestSuite {
  import fastparse._
  import fastparse.NoWhitespace._

  override def tests = Tests {

    test("SetIndex") - {
      test("changes the parser index") - {
        def parser[_: P] = P("A") ~ SetIndex(5) ~ P("B").!
        val result       = parse("AholaB", parser(_), verboseFailures = true)
        assert(result == Parsed.Success("B", 6))
      }
    }

    test("Until") - {
      test("consumes until given parser is found") - {
        def b[_: P]      = P("B")
        def parser[_: P] = P("A") ~ Until(b) ~ b.!
        val result       = parse("AholaB", parser(_), verboseFailures = true)
        assert(result == Parsed.Success("B", 6))
      }

      test("does not consume if given parser is not found") - {
        def parser[_: P] = P("A") ~ (Until("B") | "C").!
        val result       = parse("AC", parser(_), verboseFailures = true)
        assert(result == Parsed.Success("C", 2))
      }
    }

    test("UpTo") - {
      test("consumes up to the given parser") - {
        def parser[_: P] = P("A") ~ UpTo("B").!
        val result       = parse("AholaB", parser(_), verboseFailures = true)
        assert(result == Parsed.Success("holaB", 6))
      }

      test("does not consumes if the given parser fails") - {
        def parser[_: P] = P("A") ~ (UpTo("B") | "C").!
        val result       = parse("AC", parser(_), verboseFailures = true)
        assert(result == Parsed.Success("C", 2))
      }
    }

    test("Within") - {
      test("scopes an inner parser so that it cannot consume past the end of its outer parser") - {
        val input        = "ABCDEF"
        def foo[_: P]    = (!End ~ AnyChar).rep.!
        def parser[_: P] = Within(Until("E"), foo(_))
        val result       = parse(input, parser(_), verboseFailures = true)
        assert(result == Parsed.Success("ABCD", 4))
      }

      test("nested parsing regions") - {
        val input =
          """
            |AREA A
            |  SECTION 1
            |     One Two
            | SECTION 2 Three AREA B
            |SECTION 1
            |  Uno
            |  Dos
            |""".stripMargin

        case class Area(code: String, sections: Seq[Section])
        case class Section(number: Int, words: Seq[String])

        def ws[_: P]: P[Unit] = (" " | "\n").rep

        // NOTE: because we are using the Within combinator,
        // these parsers can consume all words they see in front
        // without having to worry of consuming past their intended scope.
        def word[_: P]: P[String]       = CharIn("A-Za-z").rep(1).!
        def words[_: P]: P[Seq[String]] = word.log("WORD").rep(sep = ws, min = 1)

        def section[_: P]: P[Section] =
          ("SECTION " ~ CharIn("0-9").!.map(Integer.parseInt(_)) ~ ws ~
            Within(Until("SECTION" | End).log("til SECTION"), words(_), endAtOuter = true)).map(Section.tupled)

        def sections[_: P]: P[Seq[Section]] = section.log("whole SECTION").rep(1)

        def area[_: P]: P[Area] =
          ("AREA " ~ CharIn("A-Z").! ~ ws ~
            Within(Until("AREA" | End).log("til AREA"), sections(_), endAtOuter = true)).map(Area.tupled)

        def parser[_: P]: P[Seq[Area]] =
          ws ~ area.log("whole area").rep ~ End

        val expected = Seq(
          Area("A", Seq(Section(1, Seq("One", "Two")), Section(2, Seq("Three")))),
          Area("B", Seq(Section(1, Seq("Uno", "Dos"))))
        )

        val result = parse(input, parser(_), verboseFailures = true)
        assert(result.isSuccess, result.get.value == expected)
      }

      test("endAtOuter=false makes end position that of inner") - {
        def foo[_: P]              = "A".!
        def parser[_: P]           = Within(Until("B"), foo(_))
        val Parsed.Success("A", 1) = parse("ACDBE", parser(_), verboseFailures = true)
      }

      test("endAtOuter=true makes end position that of outer") - {
        def foo[_: P]              = "A".!
        def parser[_: P]           = Within(Until("B"), inner = foo(_), endAtOuter = true)
        val Parsed.Success("A", 3) = parse("ACDBE", parser(_), verboseFailures = true)
      }

      test("fails on empty outer") - {
        def foo[_: P]               = "hola".!
        def parser[_: P]            = Within(outer = Pass, inner = foo(_))
        val Parsed.Failure(_, 0, _) = parse("hola", parser(_), verboseFailures = true)
      }

      test("can be used recursively") - {
        val input      = "HELL YES EHM, GOD NO"
        def word[_: P] = CharIn("A-Z").rep.!
        def parser[_: P] =
          Within(Until("EHM"), word(_)).rep(sep = " ")
        val Parsed.Success(res, _) = parse(input, parser(_), verboseFailures = true)
        assert(res == Seq("HELL", "YES"))
      }
    }

    test("NotWithin") - {
      test("succeeds only when inner is not present inside outer") - {
        def bar[_: P]    = "bar"
        def parser[_: P] = NotWithin(Until("baz").!, bar(_))

        val result1 = parse("barbaz", parser(_), verboseFailures = true)
        assert(!result1.isSuccess)

        val result2 = parse("foobaz", parser(_), verboseFailures = true)
        assert(result2.isSuccess, result2.get.value == "foo")
      }
    }

  }

}
