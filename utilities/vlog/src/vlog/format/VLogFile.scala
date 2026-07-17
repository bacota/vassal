package vlog.format

import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipFile}
import scala.jdk.CollectionConverters.*
import scala.util.Using

/** The two well-known zip entry names inside a .vlog file, per
  * VASSAL.build.GameState.SAVEFILE_ZIP_ENTRY and
  * VASSAL.build.module.metadata.SaveMetaData.ZIP_ENTRY_NAME.
  */
object VLogFile:
  val SavedGameEntry = "savedGame"
  val MetadataEntry = "savedata"

  /** The ASCII ESC character (0x1B) VASSAL uses to separate top-level
    * commands in the flattened log/command string (GameModule.COMMAND_SEPARATOR).
    */
  val CommandSeparator: Char = 0x1B.toChar

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

      val commands = SequenceCodec.decode(commandString, CommandSeparator).filter(_.nonEmpty)

      VLogFile(commands, metadataXml)
    }

case class VLogFile(
    commands: Vector[String],
    metadataXml: Option[String]
)
