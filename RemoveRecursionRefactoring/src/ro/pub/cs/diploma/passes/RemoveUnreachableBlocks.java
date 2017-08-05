package ro.pub.cs.diploma.passes;

import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.ir.Block;

import java.util.List;
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

  @Override
  @NotNull
  public List<Block> transform(@NotNull final List<Block> blocks) {
    List<Block> before;
    List<Block> after = blocks;
    do {
      before = after;
      after = before.stream().filter(Block::removeIfUnreachable).collect(Collectors.toList());
    }
    while (after.size() != before.size());
    return after;
  }
}
