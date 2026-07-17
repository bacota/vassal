package vlog.command

import org.scalatest.funsuite.AnyFunSuite
import vlog.format.SequenceCodec

class CommandTreeTest extends AnyFunSuite:

  private val Esc: Char = CommandTree.CommandSeparator

  test("a single leaf command with no ESC delimiter decodes directly") {
    val token = CommandCodec.encode(RemovePieceCmd("p1"))
    assert(CommandTree.flatten(token) == Vector(RemovePieceCmd("p1")))
  }

  test("sibling commands joined by one level of ESC all decode as leaves") {
    val a = CommandCodec.encode(RemovePieceCmd("p1"))
    val b = CommandCodec.encode(UndoCmd(true))
    val joined = SequenceCodec.encode(Seq(a, b), Esc)
    assert(CommandTree.flatten(joined) == Vector(RemovePieceCmd("p1"), UndoCmd(true)))
  }

  test("a nested subtree, re-escaped by its parent level, is recursively unwrapped") {
    // Simulate GameModule.encode's recursion: a child subtree with two
    // grandchildren gets ESC-joined first, then that whole string is
    // appended (and therefore re-escaped) at the parent level alongside a
    // second, unrelated sibling leaf.
    val grandchild1 = CommandCodec.encode(RemovePieceCmd("gc1"))
    val grandchild2 = CommandCodec.encode(UndoCmd(false))
    val nestedSubtree = SequenceCodec.encode(Seq(grandchild1, grandchild2), Esc)

    val sibling = CommandCodec.encode(RemovePieceCmd("sibling"))
    val whole = SequenceCodec.encode(Seq(nestedSubtree, sibling), Esc)

    assert(
      CommandTree.flatten(whole) ==
        Vector(RemovePieceCmd("gc1"), UndoCmd(false), RemovePieceCmd("sibling"))
    )
  }

  test("null commandString yields no commands") {
    assert(CommandTree.flatten(null) == Vector.empty)
  }
