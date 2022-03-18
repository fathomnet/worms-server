/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

import org.scalatra.LifeCycle
import javax.servlet.ServletContext

import scala.concurrent.ExecutionContext
import org.fathomnet.worms.etc.jdk.CustomExecutors
import org.fathomnet.worms.etc.jdk.CustomExecutors.asScala
import org.fathomnet.worms.api.PhylogenyApi
import org.fathomnet.worms.etc.jdk.Logging.{given}

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
class ScalatraBootstrap extends LifeCycle:

  override def init(context: ServletContext): Unit =

    System.getLogger(getClass.getName).atInfo.log("Bootstrapping...")
    // Optional because * is the default
    context.setInitParameter("org.scalatra.cors.allowedOrigins", "*")
    // Disables cookies, but required because browsers will not allow passing credentials to wildcard domains
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")
    context.setInitParameter(
      "org.scalatra.cors.allowedMethods",
      "GET, POST, ORIGIN, HEAD, OPTIONS, PUT, DELETE, TRACE, CONNECT"
    )

    val executionContext = CustomExecutors.newFixedThreadPoolExecutor().asScala

    context.mount(PhylogenyApi(), "/")
