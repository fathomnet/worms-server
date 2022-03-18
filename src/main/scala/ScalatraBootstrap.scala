/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

import org.scalatra.LifeCycle
import javax.servlet.ServletContext

import scala.concurrent.ExecutionContext
import org.fathomnet.worms.etc.jdk.CustomExecutors
import org.fathomnet.worms.etc.jdk.CustomExecutors.asScala
import org.fathomnet.worms.api.PhylogenyApi

class ScalatraBootstrap extends LifeCycle:

  override def init(context: ServletContext): Unit =

    println("STARTING UP NOW")
    // Optional because * is the default
    context.setInitParameter("org.scalatra.cors.allowedOrigins", "*")
    // Disables cookies, but required because browsers will not allow passing credentials to wildcard domains
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")
    context.setInitParameter(
      "org.scalatra.cors.allowedMethods",
      "GET, POST, ORIGIN, HEAD, OPTIONS, PUT, DELETE, TRACE, CONNECT"
    )

    val executionContext = CustomExecutors.newFixedThreadPoolExecutor().asScala

// context.mount(MediaApi(executionContext), "/media/demo")
    context.mount(PhylogenyApi(), "/")
