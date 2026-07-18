package vlog

import vlog.command.CommandTree
import vlog.format.VLogFile
import vlog.prune.LogPruner
import java.nio.file.Paths

/** Standalone CLI for working with .vlog files.
  *
  *   vlog dump  <file.vlog>            print metadata and decoded commands
  *   vlog strip <in.vlog> <out.vlog>   copy the log with undone commands removed
  */
@main def run(args: String*): Unit =
  args.toList match
    case "dump" :: path :: Nil => dump(path)
    case "strip" :: in :: out :: Nil => strip(in, out)
    case _ =>
      System.err.println(
        """Usage:
          |  vlog dump  <file.vlog>            Print metadata and decoded commands
          |  vlog strip <in.vlog> <out.vlog>   Copy the log with undone commands removed
          |""".stripMargin
      )
      sys.exit(2)

private def dump(pathArg: String): Unit =
  val file = VLogFile.read(Paths.get(pathArg))

  file.metadataXml.foreach { xml =>
    println("=== metadata (savedata) ===")
    println(xml)
    println()
  }

  val commands = CommandTree.flatten(file.commandString)
  println(s"=== ${commands.size} command(s) ===")
  commands.zipWithIndex.foreach { case (cmd, i) =>
    println(s"[$i] $cmd")
  }

private def strip(inArg: String, outArg: String): Unit =
  val inPath = Paths.get(inArg)
  val outPath = Paths.get(outArg)

  val file = VLogFile.read(inPath)
  val result = LogPruner.prune(file.commandString)

  if !result.changed then
    println("No undone commands found; nothing to remove.")
  else
    println(
      s"Removed ${result.removedUndoBlocks} undo action(s) and " +
        s"${result.removedUndoneCommands} undone command(s)."
    )
  if result.unmatchedUndoBlocks > 0 then
    println(
      s"Warning: ${result.unmatchedUndoBlocks} undo action(s) had nothing left " +
        "to undo and were dropped."
    )

  VLogFile.rewriteSavedGame(inPath, outPath, result.commandString)
  println(s"Wrote $outArg")
