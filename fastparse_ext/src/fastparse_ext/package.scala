package object fastparse_ext extends fastparse_ext.Extensions {
  import fastparse.P

  implicit class ExtensionOps[A](val p: P[A]) extends AnyVal {
    def unit: P[Unit] = p.map(_ => ())
  }
}
