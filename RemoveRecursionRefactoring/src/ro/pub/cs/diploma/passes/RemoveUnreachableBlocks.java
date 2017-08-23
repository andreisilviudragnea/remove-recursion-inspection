package ro.pub.cs.diploma.passes;

import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.ir.Block;

import java.util.*;
import java.util.stream.Collectors;

public class RemoveUnreachableBlocks implements Pass<List<Block>, List<Block>, List<Block>> {
  private RemoveUnreachableBlocks() {

  }

  @NotNull
  public static RemoveUnreachableBlocks getInstance() {
    return new RemoveUnreachableBlocks();
  }

  @Override
  @NotNull
  public List<Block> collect(@NotNull final List<Block> blocks) {
    return blocks;
  }

  @NotNull
  private static Set<Block> getReachableBlocks(@NotNull final Block root) {
    final Set<Block> reachableBlocks = new HashSet<>();
    reachableBlocks.add(root);

    final Queue<Block> queue = new ArrayDeque<>();
    queue.add(root);

    while (!queue.isEmpty()) {
      final Block current = queue.remove();
      for (Ref<Block> ref : current.getOutBlocks()) {
        final Block child = ref.get();
        if (!reachableBlocks.contains(child)) {
          reachableBlocks.add(child);
          queue.add(child);
        }
      }
    }

    return reachableBlocks;
  }

  @Override
  @NotNull
  public List<Block> transform(@NotNull final List<Block> blocks) {
    final Set<Block> reachableBlocks = getReachableBlocks(blocks.get(0));
    return blocks.stream().filter(reachableBlocks::contains).collect(Collectors.toList());
  }
}
