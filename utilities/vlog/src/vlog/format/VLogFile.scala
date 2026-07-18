package vlog.format

import java.io.BufferedOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}
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

  /** Write a copy of `source` to `dest`, replacing only the 'savedGame' entry
    * with `newCommandString` (re-obfuscated with a fresh random key, exactly
    * as VASSAL's ObfuscatingOutputStream would). Every other entry — the
    * 'savedata' metadata, 'moduledata', and anything else — is copied through
    * byte-for-byte, so the result is a valid .vlog for the same module.
    */
  def rewriteSavedGame(source: Path, dest: Path, newCommandString: String): Unit =
    val key = new java.util.Random().nextInt(256).toByte
    val newBytes =
      Obfuscation.encode(newCommandString.getBytes(StandardCharsets.UTF_8), key)

    Using.resource(new ZipFile(source.toFile)) { zip =>
      Using.resource(
        new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(dest)))
      ) { out =>
        val entries = zip.entries()
        while entries.hasMoreElements do
          val entry = entries.nextElement()
          out.putNextEntry(new ZipEntry(entry.getName))
          if entry.getName == SavedGameEntry then out.write(newBytes)
          else
            Using.resource(zip.getInputStream(entry)) { in => in.transferTo(out) }
          out.closeEntry()
      }
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
