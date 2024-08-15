/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.etc.zio

import org.fathomnet.worms.etc.jdk.Logging.given
import zio.*
import zio.Cause.Die

object ZioUtil:

    private val log = java.lang.System.getLogger(getClass.getName)

    def unsafeRun[E, A](app: ZIO[Any, E, A]): A =
        Unsafe.unsafe { implicit unsafe =>
            Runtime.default.unsafe.run(app).getOrThrowFiberFailure()
        }

    def safeRun[E, A](app: ZIO[Any, E, A]): Option[A] =
        Unsafe.unsafe { implicit unsafe =>
            Runtime.default.unsafe.run(app) match
                case Exit.Success(a) => Some(a)
                case Exit.Failure(e) =>
                    e match
                        case d: Die => log.atError.withCause(d.value).log(e.toString)
                        case _      => log.atError.log(e.toString)
                    None
        }
