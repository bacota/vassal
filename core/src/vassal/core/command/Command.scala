package vassal.core.command

/** Cross-platform port of `VASSAL.command.Command`: an action that can be
  * transmitted between clients (network play), written to a `.vlog`, or
  * embedded whole in a `.vsav`. Commands chain into compound trees via
  * [[append]]; each [[CommandEncoder]] only ever handles a single
  * (non-compound) command.
  *
  * Execution (`executeCommand`/`myUndoCommand` in the legacy class) mutates
  * live game state -- pieces, maps, GameState -- none of which exist in
  * vassal-core yet (that's Phase 1 item 4). This port is deliberately
  * limited to the pure-data half needed for encode/decode fidelity:
  * the subcommand tree, `isNull`, and `append`'s exact semantics, since
  * those are what the string wire format actually depends on.
  */
abstract class Command {
  private var seq: Vector[Command] = Vector.empty

  def subCommands: Vector[Command] = seq

  /** @return true if this command does nothing. */
  def isNull: Boolean = false

  def isLoggable: Boolean = !isNull

  /** True if this command has no subcommands other than null ones. */
  protected def isAtomic: Boolean = seq.forall(_.isNull)

  /** Detail string included in `toString`, e.g. for debugging/fixture dumps. */
  def details: Option[String] = None

  override def toString: String = {
    val sb = new StringBuilder(getClass.getSimpleName.stripSuffix("$"))
    details.foreach(d => sb.append('[').append(d).append(']'))
    for (c <- seq) sb.append('+').append(c.toString)
    sb.toString
  }

  /** Appends `c` as a subcommand. A no-op (nothing recorded, `this` returned
    * unchanged) if `c` is null or itself null-ish. Otherwise `c` is recorded
    * as a subcommand, and if this command was null-ish *before* the append,
    * `c` is returned instead of `this` -- callers that chain
    * `c = c.append(next)`, as the real decode dispatcher does, depend on
    * this "promotion" to skip past leading null commands.
    */
  def append(c: Command): Command = {
    if (c == null || c.isNull) this
    else {
      // isNull must be evaluated before mutating seq, since NullCommand's
      // isNull depends on seq's contents (isAtomic) -- matching the legacy
      // Java, which checks isNull() before seq.add(c).
      val wasNull = this.isNull
      seq = seq :+ c
      if (wasNull) c else this
    }
  }
}

/** `VASSAL.command.NullCommand`: a command that does nothing, encoded as the
  * empty string. Used as the root of an otherwise-empty compound command
  * tree and as the "no next command" sentinel.
  */
final class NullCommand extends Command {
  override def isNull: Boolean = isAtomic
}
