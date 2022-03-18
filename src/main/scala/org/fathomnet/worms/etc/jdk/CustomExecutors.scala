/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2021
 *
 * worms-server code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.fathomnet.worms.etc.jdk

import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

object CustomExecutors:

  private val ThreadCount = java.lang.Runtime.getRuntime().availableProcessors()

  /**
   * Best for IO bound tasks.
   * @param n
   *   Mu
   */
  def newFixedThreadPoolExecutor(n: Int = ThreadCount): ExecutorService =
    Executors.newFixedThreadPool(n)

  /**
   * Best for CPU bound tasks
   */
  def newForkJoinPool(): ExecutorService = new ForkJoinPool(ThreadCount)

  extension (executorService: ExecutorService)
    def asScala: ExecutionContext = ExecutionContext.fromExecutorService(executorService)
