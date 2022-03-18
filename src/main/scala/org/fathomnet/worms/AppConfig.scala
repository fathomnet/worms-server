/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms

import com.typesafe.config.ConfigFactory
import scala.util.Try

object AppConfig:

  val Config = ConfigFactory.load()

  val Name: String = "worms-server"

  val Version: String =
    Try(getClass.getPackage.getImplementationVersion).getOrElse("0.0.0-SNAPSHOT")
