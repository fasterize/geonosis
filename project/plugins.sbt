addSbtPlugin("org.scalatra.sbt" % "scalatra-sbt" % "0.3.2")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

resolvers += Resolver.url("dax", url("http://dax.github.io/ivy-repo")) ivys "http://dax.github.io/ivy-repo/com/typesafe/sbt/[module]_2.10_0.13/[revision]/[artifact]-[revision].[ext]"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7-2b09e1edf0f7aca736ea166abc7878d39fe78da9")
