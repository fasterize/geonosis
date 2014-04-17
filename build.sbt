import NativePackagerKeys._
import com.typesafe.sbt.packager.archetypes.ServerLoader

name := "geonosis"

version := "0.1"

packageArchetype.java_server

serverLoading in Debian := ServerLoader.SystemV

maintainer := "David Rousselie <dax@happycoders.org>"

packageSummary := "A daemon to synchronize Zookeeper to the file system"

packageDescription := "Synchronize Zookeeper nodes to the file system"

daemonUser in Linux := normalizedName.value

daemonGroup in Linux := normalizedName.value

debianPackageDependencies in Debian ++= Seq("java6-runtime")

version in Debian := "0.1-" + (System.currentTimeMillis / 1000)

mappings in Universal <+= (packageBin in Compile, sourceDirectory ) map { (_, src) =>
  val conf = src / "main" / "resources" / "application.conf"
  conf -> "conf/application.conf"
}

seq(lsSettings :_*)
