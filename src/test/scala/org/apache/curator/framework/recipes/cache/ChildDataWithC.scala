package org.apache.curator.framework.recipes.cache

import org.apache.curator.framework.recipes.cache.ChildData
import org.apache.zookeeper.data.Stat

// To remove for curator > 2.4.1
class ChildDataWithC(path: String, stat: Stat, content: Array[Byte]) extends ChildData(path, stat, content) {}
