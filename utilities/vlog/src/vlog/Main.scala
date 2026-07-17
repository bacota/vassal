package vlog

import vlog.command.CommandTree
import vlog.format.VLogFile
import java.nio.file.Paths

/** Standalone CLI for inspecting .vlog files. Usage:
  *   vlog <path-to-file.vlog>
  */
@main def run(pathArg: String): Unit =
  val path = Paths.get(pathArg)
  val file = VLogFile.read(path)

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
