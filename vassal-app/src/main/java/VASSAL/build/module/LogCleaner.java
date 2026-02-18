/*
 *
 * Copyright (c) 2000-2009 by Rodney Kinney, Brent Easton
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

package VASSAL.build.module;

import VASSAL.build.GameModule;
import VASSAL.build.module.metadata.MetaDataFactory;
import VASSAL.build.module.metadata.SaveMetaData;
import VASSAL.command.Command;
import VASSAL.command.NullCommand;
import VASSAL.tools.io.DeobfuscatingInputStream;
import VASSAL.tools.io.ObfuscatingOutputStream;
import VASSAL.tools.io.ZipWriter;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * LogCleaner provides utilities for cleaning VASSAL log files (.vlog) by
 * removing undo operations and the commands they undid, producing a clean
 * record of only the final sequence of moves.
 */
public class LogCleaner {

  /**
   * Clean a log file by removing undo operations and undone commands.
   *
   * @param inputFile  The input .vlog file to clean
   * @param outputFile The output .vlog file to write the cleaned log to
   * @throws IOException If an I/O error occurs during cleaning
   */
  public static void cleanLogFile(File inputFile, File outputFile) throws IOException {
    // Load the log file
    Command logCommand = loadLogFile(inputFile);
    
    // Extract metadata - cast to SaveMetaData as that's what .vlog files use
    SaveMetaData metadata = (SaveMetaData) MetaDataFactory.buildMetaData(inputFile);
    
    // Clean the log
    Command cleanedCommand = cleanLog(logCommand);
    
    // Save the cleaned log
    saveCleanedLog(cleanedCommand, metadata, outputFile);
  }

  /**
   * Load a log file and decode it into a Command tree.
   *
   * @param logFile The .vlog file to load
   * @return The decoded Command tree
   * @throws IOException If an I/O error occurs during loading
   */
  private static Command loadLogFile(File logFile) throws IOException {
    try (InputStream in = new BufferedInputStream(Files.newInputStream(logFile.toPath()));
         ZipInputStream zipInput = new ZipInputStream(in)) {
      for (ZipEntry entry = zipInput.getNextEntry(); entry != null;
           entry = zipInput.getNextEntry()) {
        if (GameState.SAVEFILE_ZIP_ENTRY.equals(entry.getName())) {
          try (InputStream din = new DeobfuscatingInputStream(zipInput)) {
            String commandString = IOUtils.toString(din, StandardCharsets.UTF_8);
            return GameModule.getGameModule().decode(commandString);
          }
        }
      }
    }
    
    throw new IOException("Invalid log file format");
  }

  /**
   * Clean a command tree by removing undo operations and the commands they undid.
   * 
   * The algorithm:
   * 1. Flatten the command tree into a list
   * 2. Identify undo pairs (UndoCommand(true) followed by UndoCommand(false))
   * 3. Mark the undo commands and the commands between them for removal
   * 4. Also mark the command immediately before the undo start (the original undone command)
   * 5. Rebuild the command tree without the marked commands
   *
   * @param logCommand The command tree to clean
   * @return A new command tree with undo operations removed
   */
  private static Command cleanLog(Command logCommand) {
    // Flatten the command tree to a list
    List<Command> allCommands = flattenCommands(logCommand);
    
    // Track which commands to remove
    boolean[] toRemove = new boolean[allCommands.size()];
    
    // Find and mark undo operations
    for (int i = 0; i < allCommands.size(); i++) {
      Command cmd = allCommands.get(i);
      
      // Check if this is an UndoCommand(true) - start of undo
      if (cmd instanceof BasicLogger.UndoCommand) {
        BasicLogger.UndoCommand undoCmd = (BasicLogger.UndoCommand) cmd;
        if (undoCmd.isInProgress()) {
          // Found start of undo - mark it for removal
          toRemove[i] = true;
          
          // Find the matching UndoCommand(false)
          int undoEnd = -1;
          for (int j = i + 1; j < allCommands.size(); j++) {
            Command endCmd = allCommands.get(j);
            if (endCmd instanceof BasicLogger.UndoCommand) {
              BasicLogger.UndoCommand endUndoCmd = (BasicLogger.UndoCommand) endCmd;
              if (!endUndoCmd.isInProgress()) {
                undoEnd = j;
                break;
              }
            }
          }
          
          if (undoEnd != -1) {
            // Mark the end undo command for removal
            toRemove[undoEnd] = true;
            
            // Mark all commands between start and end for removal
            for (int j = i + 1; j < undoEnd; j++) {
              toRemove[j] = true;
            }
            
            // Mark the command before the undo start (the original command being undone)
            // Need to find the previous LogCommand
            for (int j = i - 1; j >= 0; j--) {
              if (allCommands.get(j) instanceof BasicLogger.LogCommand) {
                toRemove[j] = true;
                break;
              }
            }
          }
        }
      }
    }
    
    // Rebuild command tree without removed commands
    return rebuildCommands(allCommands, toRemove);
  }

  /**
   * Flatten a command tree into a list, preserving order.
   *
   * @param command The root command
   * @return A list of all commands in the tree
   */
  private static List<Command> flattenCommands(Command command) {
    List<Command> result = new ArrayList<>();
    flattenCommandsHelper(command, result);
    return result;
  }

  /**
   * Helper method to recursively flatten commands.
   *
   * @param command The current command
   * @param result  The list to add commands to
   */
  private static void flattenCommandsHelper(Command command, List<Command> result) {
    if (command == null) {
      return;
    }
    
    result.add(command);
    
    // Process subcommands
    for (Command sub : command.getSubCommands()) {
      flattenCommandsHelper(sub, result);
    }
  }

  /**
   * Rebuild the command tree from a list, excluding marked commands.
   * 
   * This creates a new command structure that preserves the hierarchical
   * nature of the original commands.
   *
   * @param allCommands The list of all commands
   * @param toRemove    Boolean array indicating which commands to remove
   * @return The rebuilt command tree
   */
  private static Command rebuildCommands(List<Command> allCommands, boolean[] toRemove) {
    if (allCommands.isEmpty()) {
      return new NullCommand();
    }
    
    // Start with a NullCommand as the root
    Command root = new NullCommand();
    
    for (int i = 0; i < allCommands.size(); i++) {
      if (!toRemove[i]) {
        Command cmd = allCommands.get(i);
        root.append(cmd);
      }
    }
    
    return root;
  }

  /**
   * Save a cleaned command tree to a log file.
   *
   * @param cleanedCommand The cleaned command tree
   * @param metadata       The metadata to include in the log file
   * @param outputFile     The output .vlog file
   * @throws IOException If an I/O error occurs during saving
   */
  private static void saveCleanedLog(Command cleanedCommand, SaveMetaData metadata, File outputFile) throws IOException {
    String logString = GameModule.getGameModule().encode(cleanedCommand);
    
    try (ZipWriter zw = new ZipWriter(outputFile)) {
      try (OutputStream out = new ObfuscatingOutputStream(
          new BufferedOutputStream(zw.write(GameState.SAVEFILE_ZIP_ENTRY)))) {
        out.write(logString.getBytes(StandardCharsets.UTF_8));
      }
      metadata.save(zw);
    }
  }
}
