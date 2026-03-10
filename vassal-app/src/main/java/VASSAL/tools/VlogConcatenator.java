/*
 *
 * Copyright (c) 2024 by VASSAL contributors
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

import VASSAL.build.GameModule;
import VASSAL.build.module.BasicLogger;
import VASSAL.build.module.GameState;
import VASSAL.build.module.metadata.SaveMetaData;
import VASSAL.command.Command;
import VASSAL.command.CommandFilter;
import VASSAL.preferences.Prefs;
import VASSAL.tools.io.ObfuscatingOutputStream;
import VASSAL.tools.io.ZipWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless command-line utility that concatenates two VASSAL log files (.vlog)
 * into a single new log file, using the same logic as
 * {@code GameState.loadAndAppend} (i.e. {@code loadFastForward(true)}).
 *
 * <p>Usage:
 * <pre>
 *   java VASSAL.tools.VlogConcatenator &lt;module.vmod&gt; &lt;first.vlog&gt; &lt;second.vlog&gt; &lt;output.vlog&gt;
 * </pre>
 */
public class VlogConcatenator {

  public static void main(String[] args) {
    // Must be set before any AWT/Swing classes are touched
    System.setProperty("java.awt.headless", "true"); //NON-NLS

    if (args.length != 4) {
      System.err.println("Usage: VASSAL.tools.VlogConcatenator <module.vmod> <first.vlog> <second.vlog> <output.vlog>"); //NON-NLS
      System.exit(1);
    }

    final File moduleFile  = new File(args[0]);
    final File firstVlog   = new File(args[1]);
    final File secondVlog  = new File(args[2]);
    final File outputFile  = new File(args[3]);

    if (!moduleFile.exists()) {
      System.err.println("Module file not found: " + moduleFile); //NON-NLS
      System.exit(1);
    }
    if (!firstVlog.exists()) {
      System.err.println("First vlog file not found: " + firstVlog); //NON-NLS
      System.exit(1);
    }
    if (!secondVlog.exists()) {
      System.err.println("Second vlog file not found: " + secondVlog); //NON-NLS
      System.exit(1);
    }
    if (outputFile.exists()) {
      System.err.println("Output file already exists: " + outputFile); //NON-NLS
      System.exit(1);
    }

    try {
      // Initialize the module so that CommandEncoder infrastructure is available
      GameModule.init(new GameModule(new DataArchive(moduleFile.getPath())));

      final GameModule gm = GameModule.getGameModule();
      final GameState  gs = gm.getGameState();

      // --- First vlog ---
      // Decode and execute: restores game state to the initial state of the
      // first vlog and populates BasicLogger.logInput via LogCommand.execute().
      final Command cmd1 = gs.decodeSavedGame(firstVlog);
      cmd1.execute();

      // Capture the beginning state (the initial game state of the first vlog)
      final Command beginningState = gs.getRestoreCommand();
      if (beginningState == null) {
        System.err.println("Unable to capture beginning state from first vlog"); //NON-NLS
        System.exit(1);
      }

      // Collect LogCommand entries from the decoded first-vlog command tree
      final List<BasicLogger.LogCommand> logCommands = new ArrayList<>();
      collectLogCommands(cmd1, logCommands);

      // --- Second vlog ---
      // Decode and filter for LogCommands only (same as loadContinuation)
      Command cmd2 = gs.decodeSavedGame(secondVlog);
      final CommandFilter logFilter = new CommandFilter() {
        @Override
        protected boolean accept(Command c) {
          return c instanceof BasicLogger.LogCommand;
        }
      };
      cmd2 = logFilter.apply(cmd2);
      collectLogCommands(cmd2, logCommands);

      // --- Build combined command tree ---
      // beginningState is the root; append all LogCommands from both vlogs
      for (final BasicLogger.LogCommand lc : logCommands) {
        beginningState.append(lc);
      }

      // Encode the combined command tree
      final String logString = gm.encode(beginningState);

      // Disable log-comment prompting so SaveMetaData() won't open a dialog
      final Prefs prefs = gm.getPrefs();
      prefs.setValue(SaveMetaData.PROMPT_LOG_COMMENT, false);
      final SaveMetaData metadata = new SaveMetaData();

      // Write the output vlog (same format as BasicLogger.write())
      try (ZipWriter zw = new ZipWriter(outputFile)) {
        try (OutputStream out = new ObfuscatingOutputStream(
            new BufferedOutputStream(zw.write(GameState.SAVEFILE_ZIP_ENTRY)))) {
          out.write(logString.getBytes(StandardCharsets.UTF_8));
        }
        metadata.save(zw);
      }
    }
    catch (IOException e) {
      System.err.println("Error concatenating vlogs: " + e.getMessage()); //NON-NLS
      e.printStackTrace(System.err);
      System.exit(1);
    }

    System.exit(0);
  }

  /**
   * Recursively walks {@code c} and collects all
   * {@link BasicLogger.LogCommand} instances into {@code result}.
   * Non-{@code LogCommand} nodes are descended into so that LogCommands
   * nested under NullCommands (as produced by {@link CommandFilter}) are
   * also found.
   */
  private static void collectLogCommands(Command c,
                                         List<BasicLogger.LogCommand> result) {
    if (c == null) {
      return;
    }
    if (c instanceof BasicLogger.LogCommand) {
      result.add((BasicLogger.LogCommand) c);
    }
    else {
      for (final Command sub : c.getSubCommands()) {
        collectLogCommands(sub, result);
      }
    }
  }
}
