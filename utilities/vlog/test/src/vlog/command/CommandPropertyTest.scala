package vlog.command

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import vlog.Generators
import vlog.format.SequenceCodec

class CommandPropertyTest extends AnyFunSuite with ScalaCheckPropertyChecks:

  test("CommandCodec.decode is the inverse of encode for every leaf command") {
    forAll(Generators.leafCommand) { cmd =>
      assert(CommandCodec.decode(CommandCodec.encode(cmd)) == cmd)
    }
  }

  test("flatten recovers the leaves of a one-level command tree") {
    // Mirror GameModule.encode of a node whose subcommands are these leaves:
    // ESC-join their encodings, then flatten back to the original list.
    forAll(Generators.leafCommands) { cmds =>
      whenever(cmds.nonEmpty) {
        val encoded = SequenceCodec.encode(cmds.map(CommandCodec.encode), CommandTree.CommandSeparator)
        assert(CommandTree.flatten(encoded).toList == cmds)
      }
    }
  }

  test("flatten recovers the leaves of a two-level (re-escaped) command tree") {
    // Each inner group is encoded, then the groups are encoded again (which
    // re-escapes the inner ESC delimiters) — the case that forces flatten to
    // recurse. flatten must still yield the leaves in order.
    val groups = org.scalacheck.Gen.choose(1, 3)
      .flatMap(k => org.scalacheck.Gen.listOfN(k, Generators.leafCommands))
    forAll(groups) { gs =>
      whenever(gs.nonEmpty && gs.forall(_.nonEmpty)) {
        val esc = CommandTree.CommandSeparator
        val encoded = SequenceCodec.encode(
          gs.map(group => SequenceCodec.encode(group.map(CommandCodec.encode), esc)),
          esc
        )
        assert(CommandTree.flatten(encoded).toList == gs.flatten)
      }
    }
  }
