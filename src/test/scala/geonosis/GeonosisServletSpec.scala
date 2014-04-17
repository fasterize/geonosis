package geonosis

import scala.collection.JavaConverters._

import org.scalatra.test.scalatest._
import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

import org.apache.curator.framework.recipes.cache.{ChildDataWithC, ChildData, PathChildrenCache}
import org.apache.zookeeper.data.Stat

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import scalax.file.Path

class GeonosisServletSpec extends ScalatraSuite with FlatSpec with MockitoSugar {
  addServlet(classOf[GeonosisServlet], "/*")
  behavior of "a GeonosisServlet"

  trait ZKSync {
    val zookeeperSync = new ZookeeperSync("localhost:2181",
        Seq("/test1", "/test2"),
        "/tmp/test")
    ZookeeperSync.sync = zookeeperSync

    val cache = mock[PathChildrenCacheWithDC]
    when(cache.getCurrentData).thenReturn(
      Seq(
        new ChildDataWithC("/test1/test11", new Stat(), "content1".getBytes),
        new ChildDataWithC("/test1/test12", new Stat(), compact(render(("attribute" -> "value"))).getBytes)
      ).map(_.asInstanceOf[ChildData]).asJava)
    zookeeperSync.caches = Map("/test1" -> cache)
  }

  "a GET on /zk" should "list cached Zookeeper paths" in new ZKSync {
    get("/zk") {
      status should equal (200)
      header("Content-Type") should startWith ("application/json")
      body should equal (compact(render(Seq("/test1", "/test2"))))
    }
  }

  "a GET on /zk/test1/" should "list cached children of /test1" in new ZKSync {
    get("/zk/test1/") {
      status should equal (200)
      header("Content-Type") should startWith ("application/json")
      body should equal (compact(render(Seq("/test1/test11", "/test1/test12"))))
    }
  }

  "a GET on /zk/test1/test11" should "return the data content of /test1/test11" in new ZKSync {
    get("/zk/test1/test11") {
      status should equal (200)
      header("Content-Type") should startWith ("application/json")
      body should equal ("\"content1\"")
    }
  }

  "a GET on /zk/test1/test12" should "return the json content of /test1/test12" in new ZKSync {
    get("/zk/test1/test12") {
      status should equal (200)
      header("Content-Type") should startWith ("application/json")
      body should equal (compact(render(("attribute" -> "value"))))
    }
  }
}
