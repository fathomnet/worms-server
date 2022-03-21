/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

final case class Page[A](items: Seq[A], limit: Int, offset: Int, total: Int)