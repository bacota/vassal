package vassal.core.build

/** Replaces `VASSAL.build.Builder.create`'s reflective component
  * instantiation (`Class.forName(name).getConstructor().newInstance()`) with
  * an explicit registry, since Scala.js has no runtime reflection.
  *
  * buildFile.xml stores a fully-qualified Java class name as each element's
  * tag name (e.g. `<VASSAL.build.module.Documentation>`); the legacy app
  * loads and instantiates that class by name. Every module and save file
  * ever produced already has those exact strings baked in, so this registry
  * must key on the same strings forever -- there is no opportunity to
  * rename them without breaking compatibility with existing files.
  *
  * Populated incrementally: each component ported to Scala (Phase 1 item 4
  * onward) registers itself under its legacy Java class name. There is
  * deliberately no attempt here to pre-register placeholder entries for
  * components that don't have a real Scala implementation yet -- an
  * unregistered class name should fail loudly (see `create`), not silently
  * produce a stub.
  */
object ComponentRegistry {
  private var factories: Map[String, () => Buildable] = Map.empty

  /** Registers `factory` under `className`, the legacy fully-qualified Java
    * class name a buildFile.xml element's tag name will contain. Re-registering
    * the same name replaces the previous factory (useful for tests).
    */
  def register(className: String, factory: () => Buildable): Unit =
    factories += className -> factory

  def isRegistered(className: String): Boolean = factories.contains(className)

  def registeredClassNames: Set[String] = factories.keySet

  /** Instantiates the component registered under `className`.
    * @throws NoSuchComponentException if nothing is registered under that name --
    *         either an unrecognized/malformed buildFile.xml, or (far more likely
    *         while this port is incomplete) a real component that hasn't been
    *         ported to Scala yet.
    */
  def create(className: String): Buildable =
    factories.get(className) match {
      case Some(factory) => factory()
      case None          => throw NoSuchComponentException(className)
    }
}

final case class NoSuchComponentException(className: String)
    extends RuntimeException(
      s"No component registered for '$className'. Either this buildFile.xml " +
        "element is unrecognized, or (more likely) this component hasn't been " +
        "ported from the legacy Java implementation to vassal-core yet."
    )
