/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
object AppConfig:

    val Config = ConfigFactory.load()

    val Name: String = "worms-server"

    val Version: String =
        Try(getClass.getPackage.getImplementationVersion).getOrElse("0.0.0-SNAPSHOT")
