// -*- mode: scala -*-

import mill._, scalalib._, publish._

val crossVersions = Seq("2.13.2", "2.12.11")

object fastparse_ext extends Cross[FastparseExt](crossVersions: _*)
class FastparseExt(val crossScalaVersion: String) extends CrossScalaModule with PublishModule {
  def publishVersion = os.read(os.pwd / "VERSION").trim
  def artifactName   = "fastparse_ext"

  def pomSettings = PomSettings(
    description = "Utility extensions for fastparse",
    organization = "com.github.vic",
    url = "https://github.com/vic/fastparse_ext",
    licenses = Seq(License.`Apache-2.0`),
    versionControl = VersionControl.github("vic", "fastparse_ext"),
    developers = Seq(
      Developer("vic", "Victor Borja", "https://github.com/vic")
    )
  )

  override def ivyDeps = Agg(ivy"com.lihaoyi::fastparse:2.3.0")

  object test extends Tests {
    def ivyDeps        = Agg(ivy"com.lihaoyi::utest::0.7.2")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
