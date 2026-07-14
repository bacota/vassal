# VASSAL Architecture Notes

Notes on VASSAL's structure, intended as a reference for extending, rearchitecting,
or migrating the engine to another language.

## Project Structure

Multi-module Maven project:

- `vassal-app` — the engine itself (~1116 Java files under `vassal-app/src/main/java/VASSAL`)
- `vassal-deprecation`
- `vassal-doc`
- `release-prepare`

Top-level packages under `VASSAL`: `build`, `chat`, `command`, `configure`,
`counters`, `i18n`, `launch`, `preferences`, `property`, `script`, `search`,
`tools`. Most gameplay components live in `VASSAL.build.module` and
`VASSAL.build.widget`.

Tests mirror this layout under `vassal-app/src/test/java/VASSAL/...`. Coverage
is moderate/uneven, concentrated in counters/command/configure logic; UI and
chat/networking layers are thin on tests.

## Core Architecture

- **`VASSAL.build.GameModule`** (`vassal-app/src/main/java/VASSAL/build/GameModule.java`,
  ~2613 lines) — a God-object singleton (`GameModule.getGameModule()`)
  representing the whole loaded module: doc/component tree, toolbar, prefs,
  chat server, data archive, active game state.

- **`Configurable` / `AbstractConfigurable` / `Buildable` / `AbstractBuildable`**
  (`VASSAL.build`) — form a composite tree mirroring the module's
  `buildFile.xml`. Every element implements `build(Element)` /
  `getBuildElement(Document)` for XML (de)serialization.

- **`VASSAL.command.Command`** — implements the Command pattern for both
  undo AND network sync (`AddPiece`, `MovePiece`, `ChangePiece`,
  `RemovePiece`). `CommandEncoder` serializes commands to strings for
  logging/network transmission; `Logger` writes `.vlog` files;
  `ChangeTracker` / `MoveTracker` diff piece states into Commands.

- **Game pieces** use the Decorator pattern: `VASSAL.counters.GamePiece`
  (interface) and `VASSAL.counters.Decorator` (~1085 lines) wrap a
  `BasicPiece` with chained trait decorators (movement, markers, layers).
  `PieceCloner` / `PieceDefiner` handle reflective trait creation.

## UI Coupling

Swing is **not** separated from game logic. 263 files in `VASSAL.counters`
and 164 in `VASSAL.build.module` import/extend `javax.swing`. Pieces
implement their own `draw(Graphics)`; domain classes directly extend
`JComponent` / `JPanel`. There is no MVC boundary.

## Networking / Multiplayer

`VASSAL.chat` (plus `peer2peer`, `node`, `messageboard`, `ui` subpackages)
broadcasts `Command` objects between clients to keep game state in sync —
command broadcasting, not state diffing. Includes a module server/lobby
model and peer-to-peer support.

## Persistence

- Modules are ZIP-based `.vmod` files; saved games are also ZIP, handled via
  `VASSAL.tools.io.ZipArchive` / `ZipWriter` and `VASSAL.tools.DataArchive`.
- Module config is XML (`buildFile.xml`).
- Saved games/logs store sequences of encoded `Command` strings (a bespoke
  serialization format, not Java serialization).
- `.vmdx` extension files layer additional content onto a base module via
  `ExtensionTree`.

## Extensibility

Reflection-based (`Class.forName` / `newInstance`) in `VASSAL.build.Builder`,
`GameModule`, `PieceDefiner`, `AutoConfigurer`, `ConfigureTree`,
`ExtensionTree`, `DataArchive`, `WizardSupport`, `PieceMover`. Module XML
stores fully-qualified class names for components/traits — a de facto
plugin registry, instantiated reflectively at load time.

## Migration Pain Points

1. **`GameModule` God-class singleton** with global static access — hard to
   decompose cleanly.
2. **Swing baked into domain/model classes** (pieces, traits,
   configurables) — no headless core exists to port independently; UI and
   logic are entangled and would need to be untangled together.
3. **Reflection + string-encoded Java class names** embedded in saved
   games/module XML — renaming or migrating classes breaks save-file
   compatibility; any new language implementation needs a compatibility/
   registry shim to keep old saves loadable.
4. **Bespoke string-based Command encoding format** for network/log sync —
   undocumented in an obvious spec; must be reverse-engineered and
   reimplemented exactly for interop with existing modules and saved games.
