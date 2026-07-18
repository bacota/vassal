package vlog.module

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import scala.util.{Try, Using}

/** Reads the parts of a .vmod (a zip) that the location resolver needs: the
  * `buildFile.xml` module definition and, on demand, board image dimensions
  * (used only by grids with descending numbering, which reference the map
  * size). Image size is read from the header alone, not by decoding the pixels.
  */
final class ModuleFile private (path: Path):
  import ModuleFile.*

  val buildFileXml: String =
    Using.resource(new ZipFile(path.toFile)) { zip =>
      val entry = Option(zip.getEntry(BuildFile))
        .orElse(Option(zip.getEntry(BuildFileLegacy)))
        .getOrElse(throw new IllegalArgumentException(s"$path has no $BuildFile or $BuildFileLegacy"))
      Using.resource(zip.getInputStream(entry)) { in =>
        new String(in.readAllBytes(), StandardCharsets.UTF_8)
      }
    }

  /** (width, height) of a board image stored under images/ in the module. */
  def imageSize(imageName: String): Option[(Int, Int)] =
    Try {
      Using.resource(new ZipFile(path.toFile)) { zip =>
        val entry = Option(zip.getEntry(s"images/$imageName"))
          .orElse(Option(zip.getEntry(imageName)))
        entry.flatMap { e =>
          Using.resource(zip.getInputStream(e)) { in =>
            Using.resource(ImageIO.createImageInputStream(in)) { iis =>
              val readers = ImageIO.getImageReaders(iis)
              if readers.hasNext then
                val r = readers.next()
                try
                  r.setInput(iis)
                  Some((r.getWidth(0), r.getHeight(0)))
                finally r.dispose()
              else None
            }
          }
        }
      }
    }.toOption.flatten

object ModuleFile:
  private val BuildFile = "buildFile.xml"
  private val BuildFileLegacy = "buildFile"

  def read(path: Path): ModuleFile = new ModuleFile(path)
