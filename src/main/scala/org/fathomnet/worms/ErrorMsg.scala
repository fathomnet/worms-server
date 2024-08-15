/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
sealed trait ErrorMsg:
    def message: String
    def code: Int

case class NotFound(message: String, code: Int = 404)    extends ErrorMsg
case class ServerError(message: String, code: Int = 500) extends ErrorMsg
