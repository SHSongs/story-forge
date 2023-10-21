//addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.3.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
//addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.4")
libraryDependencies += "org.vafer" % "jdeb" % "1.10" artifacts (Artifact("jdeb", "jar", "jar"))
//addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")

addDependencyTreePlugin

