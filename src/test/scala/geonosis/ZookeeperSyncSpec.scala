package geonosis

import scala.collection.JavaConverters._

import org.scalatra.test.scalatest._
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

import org.apache.curator.framework.listen.ListenerContainer
import org.apache.curator.framework.recipes.cache.{ChildDataWithC, ChildData, PathChildrenCacheListener, PathChildrenCacheEvent, PathChildrenCache}
import org.apache.zookeeper.data.Stat

import com.google.common.base.Function
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._

import scalax.file.Path
import scalax.file.PathMatcher._

class PathChildrenCacheWithDC extends PathChildrenCache(null, "/test1", true) {}

class ZookeeperSyncSpec extends ScalatraSuite with FunSpec with BeforeAndAfter with MockitoSugar {

  describe("a ZookeeperSync") {
    val zookeeperSync = new ZookeeperSync("localhost:2181", Seq("/test1", "/test2"), "/tmp/test")

    it("should dump a filled Zookeeper cache") {
      val cache = mock[PathChildrenCacheWithDC]
      when(cache.getCurrentData).thenReturn(
        Seq(
          new ChildDataWithC("/test1/test11",
            new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
            "content1".getBytes),
          new ChildDataWithC("/test1/test12",
            new Stat(211, 212, 213, 214, 21, 22, 23, 215, 8, 0, 216),
            "content2".getBytes),
          new ChildDataWithC("/test1/test13",
            new Stat(311, 312, 313, 314, 31, 32, 33, 315, 0, 0, 316),
            null)
        ).map(_.asInstanceOf[ChildData]).asJava)

      zookeeperSync.getCacheData(cache).
        mapValues {new String(_)} should equal(
          Map(
            Path.fromString("/tmp/test/test1/test11/__data__") -> "content1",
            Path.fromString("/tmp/test/test1/test11/__metadata__") -> compact(render(
              ("dataVersion" -> 11) ~
                ("cversion" -> 12) ~
                ("cZxid" -> 111) ~
                ("ctime" -> 113) ~
                ("mZxid" -> 112) ~
                ("mtime" -> 114) ~
                ("pZxid" -> 116) ~
                ("aclVersion" -> 13) ~
                ("ephemeralOwner" -> 115) ~
                ("dataLength" -> 8) ~
                ("numChildren" -> 0)
            )),
            Path.fromString("/tmp/test/test1/test12/__data__") -> "content2",
            Path.fromString("/tmp/test/test1/test12/__metadata__") -> compact(render(
              ("dataVersion" -> 21) ~
                ("cversion" -> 22) ~
                ("cZxid" -> 211) ~
                ("ctime" -> 213) ~
                ("mZxid" -> 212) ~
                ("mtime" -> 214) ~
                ("pZxid" -> 216) ~
                ("aclVersion" -> 23) ~
                ("ephemeralOwner" -> 215) ~
                ("dataLength" -> 8) ~
                ("numChildren" -> 0)
            )),
            Path.fromString("/tmp/test/test1/test13/__metadata__") -> compact(render(
              ("dataVersion" -> 31) ~
                ("cversion" -> 32) ~
                ("cZxid" -> 311) ~
                ("ctime" -> 313) ~
                ("mZxid" -> 312) ~
                ("mtime" -> 314) ~
                ("pZxid" -> 316) ~
                ("aclVersion" -> 33) ~
                ("ephemeralOwner" -> 315) ~
                ("dataLength" -> 0) ~
                ("numChildren" -> 0)
            ))
          ))
    }

    it("should dump an empty Zookeeper cache") {
      val cache = mock[PathChildrenCacheWithDC]
      when(cache.getCurrentData).thenReturn(Seq().map(_.asInstanceOf[ChildData]).asJava)

      zookeeperSync.getCacheData(cache) should equal(Map())
    }
  }

  describe("a ZookeeperSync with JSON validation") {
    val zookeeperSync = new ZookeeperSync("localhost:2181", Seq("/test1", "/test2"), "/tmp/test", true)

    it("should dump a filled Zookeeper cache with valid JSON") {
      val cache = mock[PathChildrenCacheWithDC]
      when(cache.getCurrentData).thenReturn(
        Seq(new ChildDataWithC("/test1/test11",
                               new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
                               """{"test": 1}""".getBytes)
        ).map(_.asInstanceOf[ChildData]).asJava)

      zookeeperSync.getCacheData(cache).
        mapValues {new String(_)} should equal(
          Map(
            Path.fromString("/tmp/test/test1/test11/__data__") -> """{"test": 1}""",
            Path.fromString("/tmp/test/test1/test11/__metadata__") -> compact(render(
              ("dataVersion" -> 11) ~
                ("cversion" -> 12) ~
                ("cZxid" -> 111) ~
                ("ctime" -> 113) ~
                ("mZxid" -> 112) ~
                ("mtime" -> 114) ~
                ("pZxid" -> 116) ~
                ("aclVersion" -> 13) ~
                ("ephemeralOwner" -> 115) ~
                ("dataLength" -> 8) ~
                ("numChildren" -> 0)
            ))
          ))
    }

    it("should dump a filled Zookeeper cache with invalid JSON") {
      val cache = mock[PathChildrenCacheWithDC]
      when(cache.getCurrentData).thenReturn(
        Seq(new ChildDataWithC("/test1/test11",
                               new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
                               """{"test": 1}""".getBytes),
            new ChildDataWithC("/test1/test12",
                               new Stat(211, 212, 213, 214, 21, 22, 23, 215, 8, 0, 216),
                               """{"test": invalid}""".getBytes)
        ).map(_.asInstanceOf[ChildData]).asJava)

      zookeeperSync.getCacheData(cache).
        mapValues {new String(_)} should equal(
          Map(
            Path.fromString("/tmp/test/test1/test11/__data__") -> """{"test": 1}""",
            Path.fromString("/tmp/test/test1/test11/__metadata__") -> compact(render(
              ("dataVersion" -> 11) ~
                ("cversion" -> 12) ~
                ("cZxid" -> 111) ~
                ("ctime" -> 113) ~
                ("mZxid" -> 112) ~
                ("mtime" -> 114) ~
                ("pZxid" -> 116) ~
                ("aclVersion" -> 13) ~
                ("ephemeralOwner" -> 115) ~
                ("dataLength" -> 8) ~
                ("numChildren" -> 0)
            ))
          ))
    }
  }


  describe("a ZookeeperSync with a temporary dump directory") {
    var zookeeperSync: ZookeeperSync = new ZookeeperSync("localhost:2181",
        Seq("/test1", "/test2"),
        "/tmp/test")
    var tempdir: Path = Path.fromString("/tmp/test")

    before {
      tempdir = Path.createTempDirectory()
      zookeeperSync = new ZookeeperSync("localhost:2181",
          Seq("/test1", "/test2"),
          tempdir.path)

    }

    after {
      tempdir.deleteRecursively(true, true)
    }

    it("should write files from Zookeeper nodes cache") {
      val cache = mock[PathChildrenCacheWithDC]
      when(cache.getCurrentData).thenReturn(
        Seq(
          new ChildDataWithC("/test1/test11",
            new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
            "content1".getBytes),
          new ChildDataWithC("/test1/test12",
            new Stat(211, 212, 213, 214, 21, 22, 23, 215, 8, 0, 216),
            "content2".getBytes),
          new ChildDataWithC("/test1/test13",
            new Stat(311, 312, 313, 314, 31, 32, 33, 315, 0, 0, 316),
            null)
        ).map(_.asInstanceOf[ChildData]).asJava)

      zookeeperSync.dump(zookeeperSync.getCacheData(cache))

      listDir(tempdir) should equal (
        List(
          "test1/test11/__data__",
          "test1/test11/__metadata__",
          "test1/test12/__data__",
          "test1/test12/__metadata__",
          "test1/test13/__metadata__"
        ))

      (tempdir / "test1" / "test11" / "__data__").string should equal ("content1")
        (tempdir / "test1" / "test11" / "__metadata__").string should equal (compact(render(
          ("dataVersion" -> 11) ~
            ("cversion" -> 12) ~
            ("cZxid" -> 111) ~
            ("ctime" -> 113) ~
            ("mZxid" -> 112) ~
            ("mtime" -> 114) ~
            ("pZxid" -> 116) ~
            ("aclVersion" -> 13) ~
            ("ephemeralOwner" -> 115) ~
            ("dataLength" -> 8) ~
            ("numChildren" -> 0)
        )))

      (tempdir / "test1" / "test12" / "__data__").string should equal ("content2")
        (tempdir / "test1" / "test12" / "__metadata__").string should equal (compact(render(
          ("dataVersion" -> 21) ~
            ("cversion" -> 22) ~
            ("cZxid" -> 211) ~
            ("ctime" -> 213) ~
            ("mZxid" -> 212) ~
            ("mtime" -> 214) ~
            ("pZxid" -> 216) ~
            ("aclVersion" -> 23) ~
            ("ephemeralOwner" -> 215) ~
            ("dataLength" -> 8) ~
            ("numChildren" -> 0)
        )))

      (tempdir / "test1" / "test13" / "__metadata__").string should equal (compact(render(
        ("dataVersion" -> 31) ~
          ("cversion" -> 32) ~
          ("cZxid" -> 311) ~
          ("ctime" -> 313) ~
          ("mZxid" -> 312) ~
          ("mtime" -> 314) ~
          ("pZxid" -> 316) ~
          ("aclVersion" -> 33) ~
          ("ephemeralOwner" -> 315) ~
          ("dataLength" -> 0) ~
          ("numChildren" -> 0)
      )))
    }

    it("should add a new file when a Zookeeper node is added") {
      val cache = mock[PathChildrenCacheWithDC]
      val listenerContainer = new ListenerContainer[PathChildrenCacheListener]()
      when(cache.getListenable).thenReturn(listenerContainer)
      val listener = zookeeperSync.addListener(cache)
      listDir(tempdir) should equal (List())

      listener.childEvent(zookeeperSync.client, new PathChildrenCacheEvent(
        PathChildrenCacheEvent.Type.CHILD_ADDED,
        new ChildDataWithC("/test1/test11",
          new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
          "content1".getBytes)
      ))

      listDir(tempdir) should equal (
        List(
          "test1/test11/__data__",
          "test1/test11/__metadata__"
        ))

      (tempdir / "test1" / "test11" / "__data__").string should equal ("content1")
        (tempdir / "test1" / "test11" / "__metadata__").string should equal (compact(render(
          ("dataVersion" -> 11) ~
            ("cversion" -> 12) ~
            ("cZxid" -> 111) ~
            ("ctime" -> 113) ~
            ("mZxid" -> 112) ~
            ("mtime" -> 114) ~
            ("pZxid" -> 116) ~
            ("aclVersion" -> 13) ~
            ("ephemeralOwner" -> 115) ~
            ("dataLength" -> 8) ~
            ("numChildren" -> 0)
        )))
    }

    it("should update a file when a Zookeeper node is updated") {
      val cache = mock[PathChildrenCacheWithDC]
      val listenerContainer = new ListenerContainer[PathChildrenCacheListener]()

      when(cache.getCurrentData).thenReturn(
        Seq(
          new ChildDataWithC("/test1/test11",
            new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
            "content1".getBytes)
        ).map(_.asInstanceOf[ChildData]).asJava)

      zookeeperSync.dump(zookeeperSync.getCacheData(cache))
      listDir(tempdir) should equal (
        List(
          "test1/test11/__data__",
          "test1/test11/__metadata__"
        ))

      when(cache.getListenable).thenReturn(listenerContainer)
      val listener = zookeeperSync.addListener(cache)

      listener.childEvent(zookeeperSync.client, new PathChildrenCacheEvent(
        PathChildrenCacheEvent.Type.CHILD_UPDATED,
        new ChildDataWithC("/test1/test11",
          new Stat(111, 112, 113, 114, 111, 112, 113, 115, 9, 0, 116),
          "content11".getBytes)
      ))

      listDir(tempdir) should equal (
        List(
          "test1/test11/__data__",
          "test1/test11/__metadata__"
        ))

      (tempdir / "test1" / "test11" / "__data__").string should equal ("content11")
        (tempdir / "test1" / "test11" / "__metadata__").string should equal (compact(render(
          ("dataVersion" -> 111) ~
            ("cversion" -> 112) ~
            ("cZxid" -> 111) ~
            ("ctime" -> 113) ~
            ("mZxid" -> 112) ~
            ("mtime" -> 114) ~
            ("pZxid" -> 116) ~
            ("aclVersion" -> 113) ~
            ("ephemeralOwner" -> 115) ~
            ("dataLength" -> 9) ~
            ("numChildren" -> 0)
        )))
    }

    it("should remove a file when a Zookeeper node is removed") {
      val cache = mock[PathChildrenCacheWithDC]
      val listenerContainer = new ListenerContainer[PathChildrenCacheListener]()

      when(cache.getCurrentData).thenReturn(
        Seq(
          new ChildDataWithC("/test1/test11",
            new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
            "content1".getBytes)
        ).map(_.asInstanceOf[ChildData]).asJava)

      zookeeperSync.dump(zookeeperSync.getCacheData(cache))
      listDir(tempdir) should equal (
        List(
          "test1/test11/__data__",
          "test1/test11/__metadata__"
        ))

      when(cache.getListenable).thenReturn(listenerContainer)
      val listener = zookeeperSync.addListener(cache)

      listener.childEvent(zookeeperSync.client, new PathChildrenCacheEvent(
        PathChildrenCacheEvent.Type.CHILD_REMOVED,
        new ChildDataWithC("/test1/test11",
          new Stat(111, 112, 113, 114, 11, 12, 13, 115, 8, 0, 116),
          "content1".getBytes)
      ))

      listDir(tempdir) should equal (List())
    }

  }

  def listDir(dir: Path): Seq[String] = {
    dir.descendants(filter = IsFile).map { p => Path(p.segments.drop(3): _*).path }.toList.map(_.toString).sorted
  }
}
