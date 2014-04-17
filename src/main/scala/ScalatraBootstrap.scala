import geonosis._
import org.scalatra._
import scala.collection.JavaConverters._
import javax.servlet.ServletContext
import com.typesafe.config.ConfigFactory

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    val config = ConfigFactory.load

    ZookeeperSync(config.getStringList("zookeeper.servers").asScala.toSeq.mkString(","),
                  config.getStringList("zookeeper.paths").asScala.toSeq,
                  config.getString("syncdir"))

    context.mount(new GeonosisServlet, "/*")
  }
}
