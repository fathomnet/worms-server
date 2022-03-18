/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.etc.jdk

import java.lang.System.Logger
import java.lang.System.Logger.Level
import java.util.function.Supplier

/**
 * Add fluent logging to System.Logger. Usage:
 * {{{
 * import org.fathomnet.support.etc.jdk.Logging.{given, *}
 * given log: Logger = System.getLogger("my.logger")
 *
 * log.atInfo.log("Hello World")
 * log.atInfo.withCause(new RuntimeException("Oops")).log("Hello World")
 *
 * }}}
 * * @author Brian Schlining
 */
object Logging:

  trait Builder:
    def logger: Logger
    def level: Level
    def throwable: Option[Throwable]

  case class LoggerBuilder(
      logger: Logger,
      level: Level = Level.OFF,
      throwable: Option[Throwable] = None
  ):

    def atTrace: LoggerBuilder = copy(level = Level.TRACE)
    def atDebug: LoggerBuilder = copy(level = Level.DEBUG)
    def atInfo: LoggerBuilder  = copy(level = Level.INFO)
    def atWarn: LoggerBuilder  = copy(level = Level.WARNING)
    def atError: LoggerBuilder = copy(level = Level.ERROR)

    def withCause(cause: Throwable): LoggerBuilder = copy(throwable = Some(cause))

    def log(msg: String): Unit =
      if (logger.isLoggable(level))
        throwable match
          case Some(e) => logger.log(level, msg, e)
          case None    => logger.log(level, msg)

    def log(fn: Supplier[String]): Unit =
      if (logger.isLoggable(level))
        throwable match
          case Some(e) => logger.log(level, fn, e)
          case None    => logger.log(level, fn)

  given Conversion[Logger, LoggerBuilder] with
    def apply(logger: Logger): LoggerBuilder = LoggerBuilder(logger)
