/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

final case class Names(aphiaId: Long, name: String, acceptedName: String, alternateNames: Seq[String])
