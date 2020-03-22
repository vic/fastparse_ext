// -*- mode: scala -*-

// Dont use sonatype's maven-central as it timeouts in travis.
// interp.repositories() =
//  List(coursierapi.Repository("https://jcenter.bintray.com"))
// @

import mill._, scalalib._, publish._


object fastparse_ext extends ScalaModule with PublishModule {

  def publishVersion = os.read(os.pwd / "VERSION").trim

  // use versions installed from .tool-versions
  def scalaVersion = scala.util.Properties.versionNumberString
  def millVersion = System.getProperty("MILL_VERSION")

  def artifactName = "fastparse_ext"

  def m2 = T {
    val pa = publishArtifacts()
    val wd = T.ctx().dest
    val ad = pa.meta.group.split("\\.").foldLeft(wd)((a, b) => a / b) / pa.meta.id / pa.meta.version
    os.makeDir.all(ad)
    pa.payload.map { case (f,n) => os.copy(f.path, ad/n) }
  }

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

  override def ivyDeps = Agg(ivy"com.lihaoyi::fastparse:2.2.2")

  object test extends Tests {
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.2")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
