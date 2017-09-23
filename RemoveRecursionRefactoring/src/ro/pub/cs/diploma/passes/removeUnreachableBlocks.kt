package ro.pub.cs.diploma.passes

import ro.pub.cs.diploma.ir.Block
import java.util.*

private fun getReachableBlocks(root: Block): Set<Block> {
  val reachableBlocks = HashSet<Block>()
  reachableBlocks.add(root)

  val queue = ArrayDeque<Block>()
  queue.add(root)

  while (!queue.isEmpty()) {
    val current = queue.remove()
    for (ref in current.outBlocks) {
      val child = ref.get()
      if (!reachableBlocks.contains(child)) {
        reachableBlocks.add(child)
        queue.add(child)
      }
    }
  }

  return reachableBlocks
}

fun removeUnreachableBlocks(blocks: List<Block>): List<Block> {
  val reachableBlocks = getReachableBlocks(blocks[0])
  return blocks.filter { reachableBlocks.contains(it) }.toList()
}
