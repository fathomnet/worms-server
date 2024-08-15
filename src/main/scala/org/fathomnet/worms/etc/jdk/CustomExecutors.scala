/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.etc.jdk

import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import scala.concurrent.ExecutionContext

/**
 * @author
 *   Brian Schlining
 * @since 2022-03-17
 */
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
