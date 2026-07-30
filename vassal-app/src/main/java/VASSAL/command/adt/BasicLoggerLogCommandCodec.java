/*
 * Copyright (c) 2000-2024 by The VASSAL Development Team
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License (LGPL).
 */
package VASSAL.command.adt;

/**
 * {@link CommandCodec} for {@link BasicLoggerLogCommandADT}.
 *
 * <p>Wire format: the fully encoded form of the wrapped {@link CommandADT}
 * (produced by the interpreter).
 */
public class BasicLoggerLogCommandCodec implements CommandCodec {

  private final CommandInterpreter interpreter;

  public BasicLoggerLogCommandCodec(CommandInterpreter interpreter) {
    this.interpreter = interpreter;
  }

  @Override
  public String getCommandType() { return BasicLoggerLogCommandADT.COMMAND_TYPE; }

  @Override
  public String encode(CommandADT command) {
    return interpreter.encode(((BasicLoggerLogCommandADT) command).getLoggedCommand());
  }

  @Override
  public CommandADT decode(String encoded) {
    return new BasicLoggerLogCommandADT(interpreter.decode(encoded));
  }
}
