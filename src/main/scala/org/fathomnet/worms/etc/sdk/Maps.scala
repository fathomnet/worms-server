/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * worms-server code is licensed under the MIT license.
 */

package org.fathomnet.worms.etc.sdk

object Maps:

    def findUniqueKey(baseKey: String, map: scala.collection.Map[String, ?]): String =
        def helper(attempt: Int): String =
            val newKey = if (attempt == 0) baseKey else s"$baseKey $attempt"
            if (!map.contains(newKey)) newKey else helper(attempt + 1)
        helper(0)
