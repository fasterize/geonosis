package geonosis

import scala.collection.JavaConverters._
import scala.util.Try

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.framework.recipes.cache.{PathChildrenCache, PathChildrenCacheListener, PathChildrenCacheEvent, ChildData}
import org.apache.curator.utils.{CloseableUtils, ZKPaths}

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import grizzled.slf4j.Logging
import scalax.file.Path
import scalax.file.ImplicitConversions._

class ZookeeperSync(val zookeeperServers: String, val zkNodes: Seq[String], val dumpBasename: String) extends Logging {
  val client: CuratorFramework = CuratorFrameworkFactory.newClient(zookeeperServers,
      new ExponentialBackoffRetry(1000, 3))
  val dumpBasePath = Path.fromString(dumpBasename)

  if (zkNodes.isEmpty) {
    warn("No Zookeeper node to synchronize")
  }

  var caches: Map[String, PathChildrenCache] = zkNodes.map { zkNode =>
      (zkNode -> new PathChildrenCache(client, zkNode, true))
    }.toMap

  def start(): Unit = {
    info(s"Starting Zookeeper synchronization to ${dumpBasename}")
    try {
      client.start()
      caches.foreach { case (zkNode, cache) =>
        info(s"Loading Zookeeper cache for $zkNode")
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE)
        addListener(cache)
        dump(getCacheData(cache))
      }
    }
  }

  def dump(actions: Map[Path, Array[Byte]]): Unit = {
    actions.foreach { case (path, data) =>
      debug(s"Writing $path")
      path.write(data)
    }
  }

  def getCacheData(cache: PathChildrenCache): Map[Path, Array[Byte]] = {
    cache.getCurrentData.asScala.toSet.
      map(getChildFilesToDump).
      foldLeft(Map[Path, Array[Byte]]())(_ ++ _)
  }

  def getChildFilesToDump(child: ChildData): Map[Path, Array[Byte]] = {
    List(
      getChildData(child),
      getChildMetadata(child)
    ).flatten.toMap
  }

  def getChildData(child: ChildData): Option[(Path, Array[Byte])] = {
    if (child.getData == null) {
      None
    } else {
      val childPath = Path.fromString(child.getPath.substring(1))
      Some((dumpBasePath / childPath / "__data__") -> child.getData)
    }
  }

  def getChildMetadata(child: ChildData): Option[(Path, Array[Byte])] = {
    val childPath = Path.fromString(child.getPath.substring(1))
    Some((dumpBasePath / childPath / "__metadata__") -> compact(render(
      ("dataVersion" -> child.getStat.getVersion) ~
        ("cversion" -> child.getStat.getCversion) ~
        ("cZxid" -> child.getStat.getCzxid) ~
        ("ctime" -> child.getStat.getCtime) ~
        ("mZxid" -> child.getStat.getMzxid) ~
        ("mtime" -> child.getStat.getMtime) ~
        ("pZxid" -> child.getStat.getPzxid) ~
        ("aclVersion" -> child.getStat.getAversion) ~
        ("ephemeralOwner" -> child.getStat.getEphemeralOwner) ~
        ("dataLength" -> child.getStat.getDataLength) ~
        ("numChildren" -> child.getStat.getNumChildren)
    )).getBytes)
  }

  def remove(path: String): Unit = {
    (dumpBasePath / Path.fromString(path.substring(1))).deleteRecursively(true, true)
  }

  def addListener(cache: PathChildrenCache): PathChildrenCacheListener = {
    val listener = new PathChildrenCacheListener {
        def childEvent(client: CuratorFramework, event: PathChildrenCacheEvent): Unit = {
          event.getType match {
            case PathChildrenCacheEvent.Type.CHILD_ADDED => {
              debug(s"Node added: ${event.getData.getPath}")
              dump(getChildFilesToDump(event.getData))
            }
            case PathChildrenCacheEvent.Type.CHILD_UPDATED => {
              debug(s"Node changed: ${event.getData.getPath}")
              dump(getChildFilesToDump(event.getData))
            }
            case PathChildrenCacheEvent.Type.CHILD_REMOVED => {
              debug(s"Node removed: ${event.getData.getPath}")
              remove(event.getData.getPath)
            }
            case _ =>
          }
        }
      }

    cache.getListenable().addListener(listener)
    listener
  }

  def list(zkNode: String) = for {
      cache <- caches.get(zkNode).toSeq
      child: ChildData <- cache.getCurrentData.asScala.toSet
    } yield child.getPath

  def get(zkNode: String) = {
    val Pattern = "(.*)/([^/]*)".r
    zkNode match {
      case Pattern(dirname, basename) => (
        for {
          cache <- caches.get(dirname).toSeq

          child: ChildData <- cache.getCurrentData.asScala.toSet

          result <- Try(
            parse(new String(child.getData))
          ).recover {
            case _ => new String(child.getData)
          }.toOption.toSeq if child.getPath == zkNode

        } yield result
      ).headOption
      case _ => None
    }
  }

}

object ZookeeperSync {
  var sync: ZookeeperSync = null

  def apply(zookeeperServers: String = "localhost:2181", zkNodes: Seq[String] = Seq(), dumpBasename: String = "/tmp"): ZookeeperSync = {
    if (sync == null) {
      sync = new ZookeeperSync(zookeeperServers, zkNodes, dumpBasename)
      sync.start
    }
    sync
  }
}
