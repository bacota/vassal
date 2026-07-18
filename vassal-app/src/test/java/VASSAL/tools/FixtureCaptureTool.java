/*
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASSAL.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.w3c.dom.Document;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import VASSAL.build.Builder;
import VASSAL.build.GameModule;
import VASSAL.build.module.GameState;
import VASSAL.command.Command;
import VASSAL.tools.menu.MenuBarProxy;
import VASSAL.tools.menu.MenuManager;

/**
 * Phase-0 compatibility-corpus fixture capture tool.
 *
 * Loads one module (.vmod/.vmdx) via the real (legacy) VASSAL code, dumps its
 * re-serialized buildFile.xml, then decodes any number of save/log files
 * (.vsav/.vlog) belonging to that module and dumps each decoded Command tree
 * (both a human-readable form and the re-encoded wire-format string).
 *
 * These fixtures are the "golden" output the Scala.js reimplementation is
 * graded against for format compatibility.
 *
 * A real (or virtual, e.g. Xvfb) X display is required: GameModule
 * unconditionally constructs a JFrame, so this cannot run under plain
 * java.awt.headless=true.
 *
 * Usage:
 *   FixtureCaptureTool &lt;module.vmod&gt; &lt;outputDir&gt; [save-or-log-file ...]
 */
public final class FixtureCaptureTool {

  private FixtureCaptureTool() { }

  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: FixtureCaptureTool <module.vmod> <outputDir> [save-or-log-file ...]");
      System.exit(1);
    }

    final File moduleFile = new File(args[0]);
    final Path outDir = Path.of(args[1]);
    Files.createDirectories(outDir);

    final String moduleBase = stripExtension(moduleFile.getName());

    new ToolMenuManager();
    GameModule.init(new GameModule(new DataArchive(moduleFile.getPath())));
    final GameModule module = GameModule.getGameModule();

    // 1. Re-serialized buildFile.xml
    final Document doc = Builder.createNewDocument();
    doc.appendChild(module.getBuildElement(doc));
    final StringWriter xmlOut = new StringWriter();
    Builder.writeDocument(doc, xmlOut);
    writeFile(outDir.resolve(moduleBase + ".buildFile.xml"), xmlOut.toString());

    System.out.println("Captured module tree: " + moduleBase);

    // 2. Each save/log file, decoded via the same GameState the module just built.
    final GameState state = module.getGameState();
    for (int i = 2; i < args.length; i++) {
      final File saveFile = new File(args[i]);
      final String base = stripExtension(saveFile.getName());

      final Command decoded = state.decodeSavedGame(saveFile);

      // Human-readable recursive dump (Command.toString() already recurses
      // into subcommands via '+').
      writeFile(outDir.resolve(base + ".commands.txt"), decoded.toString());

      // Re-encoded wire-format string -- the byte-fidelity fixture.
      final String reencoded = module.encode(decoded);
      writeFile(outDir.resolve(base + ".reencoded.txt"), reencoded);

      System.out.println("Captured save/log: " + base);
    }

    System.exit(0);
  }

  private static void writeFile(Path path, String content) throws IOException {
    try (FileWriter w = new FileWriter(path.toFile(), StandardCharsets.UTF_8)) {
      w.write(content);
    }
  }

  private static String stripExtension(String name) {
    final int dot = name.lastIndexOf('.');
    return dot < 0 ? name : name.substring(0, dot);
  }

  /** Minimal stand-in for the private VASSAL.launch.Player.PlayerMenuManager. */
  private static final class ToolMenuManager extends MenuManager {
    private final MenuBarProxy menuBar = new MenuBarProxy();

    @Override
    public JMenuBar getMenuBarFor(JFrame fc) {
      return menuBar.createPeer();
    }

    @Override
    public MenuBarProxy getMenuBarProxyFor(JFrame fc) {
      return menuBar;
    }
  }
}
