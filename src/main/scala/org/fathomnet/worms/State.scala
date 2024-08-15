/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

import scala.concurrent.ExecutionContext

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
object State:

    var data: Option[Data] = None
