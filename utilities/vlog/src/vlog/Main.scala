package vlog

import vlog.command.CommandTree
import vlog.format.VLogFile
import vlog.module.{GridValidation, LocationResolver}
import vlog.moves.MoveExtractor
import vlog.prune.LogPruner
import java.nio.file.Paths

/** Standalone CLI for working with .vlog files.
  *
  *   vlog dump   <file.vlog>                  print metadata and decoded commands
  *   vlog strip  <in.vlog> <out.vlog>         copy the log with undone commands removed
  *   vlog moves  <file.vlog> [module.vmod]    list each move as piece / From / To
  *   vlog locate <module.vmod> <map> <x> <y>  name the grid location of a point
  */
@main def run(args: String*): Unit =
  args.toList match
    case "dump" :: path :: Nil => dump(path)
    case "strip" :: in :: out :: Nil => strip(in, out)
    case "moves" :: path :: Nil => listMoves(path, None)
    case "moves" :: path :: module :: Nil => listMoves(path, Some(module))
    case "locate" :: module :: map :: x :: y :: rest =>
      (scala.util.Try(x.toInt).toOption, scala.util.Try(y.toInt).toOption) match
        case (Some(xi), Some(yi)) => locate(module, map, xi, yi, rest.headOption)
        case _ =>
          System.err.println("Usage: vlog locate <module.vmod> <map> <x> <y> [board]")
          sys.exit(2)
    case "verify-grid" :: module :: vlog :: map :: Nil => verifyGrid(module, vlog, map)
    case _ =>
      System.err.println(
        """Usage:
          |  vlog dump   <file.vlog>                  Print metadata and decoded commands
          |  vlog strip  <in.vlog> <out.vlog>         Copy the log with undone commands removed
          |  vlog moves  <file.vlog> [module.vmod]    List each move as piece / From / To
          |                                           (a module fills in names the log didn't record)
          |  vlog locate <module.vmod> <map> <x> <y> [board]
          |                                           Name the grid location of a map point
          |  vlog verify-grid <module.vmod> <file.vlog> <map>
          |                                           Check computed names against the log's own record
          |""".stripMargin
      )
      sys.exit(2)

private def verifyGrid(moduleArg: String, vlogArg: String, map: String): Unit =
  val resolver = LocationResolver.read(Paths.get(moduleArg))
  val r = GridValidation.validate(resolver, Paths.get(vlogArg), map)
  println(s"samples: ${r.samples}  matched: ${r.matched}  mismatched: ${r.mismatched}  unresolved: ${r.unresolved}")
  r.examples.foreach(e => println(s"  MISMATCH $e"))
  if r.samples == 0 then println("(no ground-truth samples recorded for this map)")
  else if r.ok then println("PASS") else println("FAIL")

private def locate(moduleArg: String, map: String, x: Int, y: Int, board: Option[String]): Unit =
  val resolver = LocationResolver.read(Paths.get(moduleArg))
  resolver.locationName(map, (x, y), board) match
    case Right(name) => println(name)
    case Left(reason) =>
      System.err.println(s"could not locate ($x,$y) on '$map': $reason")
      sys.exit(1)

private def listMoves(pathArg: String, moduleArg: Option[String]): Unit =
  val file = VLogFile.read(Paths.get(pathArg))
  // Prune first so undone false-starts are not listed as moves that happened.
  val pruned = LogPruner.prune(file.commandString).commandString
  val extracted = MoveExtractor.extract(pruned)

  // If a module is given, compute names for moves whose report didn't record them.
  val resolver = moduleArg.map(m => LocationResolver.read(Paths.get(m)))
  def geom(map: Option[String], xy: (Int, Int)): Option[String] =
    for r <- resolver; mp <- map; name <- r.locationName(mp, xy).toOption yield name
  val moveList = extracted.map { m =>
    m.copy(
      from = m.from.orElse(geom(m.fromMap, m.fromXY)),
      to = m.to.orElse(geom(m.toMap, m.toXY))
    )
  }

  if moveList.isEmpty then
    println("No moves found.")
  else
    val loc = (o: Option[String]) => o.getOrElse("?")
    val pieceW = moveList.map(_.piece.length).max.max(5).min(50)
    val fromW = moveList.map(m => loc(m.from).length).max.max(4)
    val toW = moveList.map(m => loc(m.to).length).max.max(2)
    println(s"${"Piece".padTo(pieceW, ' ')}  ${"From".padTo(fromW, ' ')}  ${"To".padTo(toW, ' ')}")
    moveList.foreach { m =>
      val piece = if m.piece.length > pieceW then m.piece.take(pieceW - 1) + "…" else m.piece
      val crossMap = if m.fromMap != m.toMap then s"  [${loc(m.fromMap)} -> ${loc(m.toMap)}]" else ""
      println(
        s"${piece.padTo(pieceW, ' ')}  ${loc(m.from).padTo(fromW, ' ')}  ${loc(m.to).padTo(toW, ' ')}$crossMap"
      )
    }
    println(s"\n${moveList.size} move(s).")

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
