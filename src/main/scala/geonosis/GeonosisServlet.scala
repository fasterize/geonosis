package geonosis

import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json._

class GeonosisServlet extends GeonosisStack with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/zk/*") {
    ZookeeperSync().get(s"/${multiParams("splat").head}")
  }

  get("/zk/*/") {
    ZookeeperSync().list(s"/${multiParams("splat").head}")
  }

  get("/zk/?") {
    ZookeeperSync().zkNodes
  }

}
