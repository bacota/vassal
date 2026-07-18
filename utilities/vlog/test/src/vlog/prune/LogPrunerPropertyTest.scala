package vlog.prune

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen
import vlog.command.*
import vlog.format.SequenceCodec
import vlog.moves.MoveExtractor

class LogPrunerPropertyTest extends AnyFunSuite with ScalaCheckPropertyChecks:

  private val Esc = CommandTree.CommandSeparator
  private def logged(c: Command): String = "LOG\t" + CommandCodec.encode(c)

  private def moveToken(i: Int): String =
    SequenceCodec.encode(
      Seq(
        logged(MovePieceCmd(s"p$i", Some("M"), i, i, None, Some("M"), 0, 0, None, Some("u"))),
        logged(DisplayTextCmd(s"* p$i moves A -> B *"))
      ),
      Esc
    )
  private def chatToken(i: Int): String = logged(DisplayTextCmd(s"* chat $i *"))
  private val undoToken: String =
    SequenceCodec.encode(Seq("LOG\tUNDO\ttrue", "LOG\tUNDO\tfalse"), Esc)
  private def stream(tokens: Seq[String]): String =
    SequenceCodec.encode(CommandCodec.encode(AddPieceCmd("setup", "s", "s")) +: tokens, Esc)

  private enum Ev:
    case Move, Chat, Undo

  /** A random action sequence in which every undo has a preceding, still-live
    * undoable move to take back (undoStack never goes negative) — the only kind
    * of sequence VASSAL can actually produce. */
  private val events: Gen[List[Ev]] =
    Gen.sized { size =>
      def go(n: Int, stack: Int, acc: List[Ev]): Gen[List[Ev]] =
        if n == 0 then Gen.const(acc.reverse)
        else
          val choice =
            if stack > 0 then Gen.oneOf(Ev.Move, Ev.Chat, Ev.Undo)
            else Gen.oneOf(Ev.Move, Ev.Chat)
          choice.flatMap { e =>
            val ns = e match
              case Ev.Move => stack + 1
              case Ev.Undo => stack - 1
              case Ev.Chat => stack
            go(n - 1, ns, e :: acc)
          }
      go(size % 25, 0, Nil)
    }

  private def build(evs: List[Ev]): (String, Int, Int) =
    var i = 0
    val tokens = evs.map {
      case Ev.Move => i += 1; moveToken(i)
      case Ev.Chat => i += 1; chatToken(i)
      case Ev.Undo => undoToken
    }
    (stream(tokens), evs.count(_ == Ev.Move), evs.count(_ == Ev.Undo))

  private def undoLeaves(s: String): Int =
    CommandTree.flatten(s).count { case LogCmd(_: UndoCmd) => true; case _ => false }

  test("every undo is matched: removes one undo block and one undone move") {
    forAll(events) { evs =>
      val (input, _, undos) = build(evs)
      val r = LogPruner.prune(input)
      assert(r.removedUndoBlocks == undos)
      assert(r.removedUndoneCommands == undos)
      assert(r.unmatchedUndoBlocks == 0)
    }
  }

  test("the pruned log contains no undo commands") {
    forAll(events) { evs =>
      val (input, _, _) = build(evs)
      assert(undoLeaves(LogPruner.prune(input).commandString) == 0)
    }
  }

  test("surviving move count equals moves minus undos") {
    forAll(events) { evs =>
      val (input, moves, undos) = build(evs)
      val pruned = LogPruner.prune(input).commandString
      assert(MoveExtractor.extract(pruned).size == moves - undos)
    }
  }

  test("pruning is idempotent") {
    forAll(events) { evs =>
      val (input, _, _) = build(evs)
      val once = LogPruner.prune(input).commandString
      assert(LogPruner.prune(once).commandString == once)
    }
  }

  test("a log with no undos is returned byte-for-byte unchanged") {
    forAll(events) { evs =>
      whenever(!evs.contains(Ev.Undo)) {
        val (input, _, _) = build(evs)
        val r = LogPruner.prune(input)
        assert(!r.changed && r.commandString == input)
      }
    }
  }
