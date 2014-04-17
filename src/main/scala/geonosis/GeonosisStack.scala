package geonosis

import org.scalatra._
import javax.servlet.http.HttpServletRequest
import collection.mutable

trait GeonosisStack extends ScalatraServlet {

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }
}
