package vlog.format

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.zip.ZipFile
import scala.util.Using

/** The two well-known zip entry names inside a .vlog file, per
  * VASSAL.build.GameState.SAVEFILE_ZIP_ENTRY and
  * VASSAL.build.module.metadata.SaveMetaData.ZIP_ENTRY_NAME.
  */
object VLogFile:
  val SavedGameEntry = "savedGame"
  val MetadataEntry = "savedata"

  def read(path: Path): VLogFile =
    Using.resource(new ZipFile(path.toFile)) { zip =>
      def entryBytes(name: String): Option[Array[Byte]] =
        Option(zip.getEntry(name)).map { entry =>
          Using.resource(zip.getInputStream(entry)) { in =>
            in.readAllBytes()
          }
        }

      val metadataXml = entryBytes(MetadataEntry).map { bytes =>
        new String(Obfuscation.decode(bytes), StandardCharsets.UTF_8)
      }

      val commandString = entryBytes(SavedGameEntry).map { bytes =>
        new String(Obfuscation.decode(bytes), StandardCharsets.UTF_8)
      }.getOrElse(
        throw new IllegalArgumentException(s"$path does not contain a '$SavedGameEntry' entry")
      )

      VLogFile(commandString, metadataXml)
    }

/** @param commandString the deobfuscated, still-encoded contents of the
  *   'savedGame' zip entry: a recursively ESC-joined Command tree, per
  *   VASSAL.build.GameModule.encode/decode. Use vlog.command.CommandTree.flatten
  *   to turn this into an ordered list of leaf Commands.
  */
case class VLogFile(
    commandString: String,
    metadataXml: Option[String]
)
