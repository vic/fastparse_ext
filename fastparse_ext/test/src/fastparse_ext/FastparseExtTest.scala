package fastparse_ext
import utest._

object FastparseExtTest extends TestSuite {
  import fastparse._
  import fastparse.NoWhitespace._

  override def tests = Tests {

    test("SetIndex") - {
      test("changes the parser index") - {
        def parser[_: P] = P("A") ~ SetIndex(5) ~ P("B").!
        val result       = parse("AholaB", parser(_))
        assert(result == Parsed.Success("B", 6))
      }
    }

    test("Until") - {
      test("consumes until given parser is found") - {
        def b[_: P]      = P("B")
        def parser[_: P] = P("A") ~ Until(b) ~ b.!
        val result       = parse("AholaB", parser(_))
        assert(result == Parsed.Success("B", 6))
      }

      test("does not consume if given parser is not found") - {
        def parser[_: P] = P("A") ~ (Until("B") | "C").!
        val result       = parse("AC", parser(_))
        assert(result == Parsed.Success("C", 2))
      }
    }

    test("UpTo") - {
      test("consumes up to the given parser") - {
        def parser[_: P] = P("A") ~ UpTo("B").!
        val result       = parse("AholaB", parser(_))
        assert(result == Parsed.Success("holaB", 6))
      }

      test("does not consumes if the given parser fails") - {
        def parser[_: P] = P("A") ~ (UpTo("B") | "C").!
        val result       = parse("AC", parser(_))
        assert(result == Parsed.Success("C", 2))
      }
    }

    test("Within") - {
      test("scopes an inner parser so that it cannot consume past the end of its outer parser") - {
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
        def word[_: P]: P[String]       = (!(" " | "\n") ~ AnyChar).rep(1).!
        def words[_: P]: P[Seq[String]] = word.rep(sep = ws, min = 1)

        def section[_: P]: P[Section] =
          ("SECTION " ~ CharIn("0-9").!.map(Integer.parseInt(_)) ~ ws ~
            Within(Until("SECTION" | End), words(_), endAtOuter = true)).map(Section.tupled)

        def sections[_: P]: P[Seq[Section]] = section.rep(1)

        def area[_: P]: P[Area] =
          ("AREA " ~ CharIn("A-Z").! ~ ws ~
            Within(Until("AREA" | End), sections(_), endAtOuter = true)).map(Area.tupled)

        def parser[_: P]: P[Seq[Area]] =
          ws ~ area.rep ~ End

        val expected = Seq(
          Area("A", Seq(Section(1, Seq("One", "Two")), Section(2, Seq("Three")))),
          Area("B", Seq(Section(1, Seq("Uno", "Dos"))))
        )

        val result = parse(input, parser(_))
        assert(result.isSuccess, result.get.value == expected)
      }

      test("endAtOuter=false makes end position that of inner") - {
        def parser[_: P]           = Within(Until("B"), Pass(_) ~ "A".!, endAtOuter = false)
        val Parsed.Success("A", 1) = parse("ACDBE", parser(_))
      }

      test("endAtOuter=true makes end position that of outer") - {
        def parser[_: P]           = Within(Until("B"), Pass(_) ~ "A".!, endAtOuter = true)
        val Parsed.Success("A", 3) = parse("ACDBE", parser(_))
      }
    }

    test("NotWithin") - {
      test("succeeds only when inner is not present inside outer") - {
        def parser[_: P] = NotWithin(Until("baz").!, Pass(_) ~ "bar")

        val result1 = parse("barbaz", parser(_))
        assert(!result1.isSuccess)

        val result2 = parse("foobaz", parser(_))
        assert(result2.isSuccess, result2.get.value == "foo")
      }
    }

  }

}
