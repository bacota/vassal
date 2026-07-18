package vassal.core.build

import vassal.core.xml.XmlElement

/** Cross-platform equivalent of `VASSAL.build.Buildable`: a component that
  * can be built from a buildFile.xml element and mirrors it back out.
  * Deliberately minimal for now -- grows as real components are ported
  * (Phase 1 item 4 onward); see [[ComponentRegistry]].
  */
trait Buildable {
  /** Builds this component's own state from `e` (its buildFile.xml element),
    * then constructs and adds any child components found among `e`'s child
    * elements via [[ComponentRegistry.create]] and `addTo`.
    */
  def build(e: XmlElement): Unit

  /** Adds this component to `parent`. As in the legacy interface, the child
    * is responsible for attaching itself, so component types can be added by
    * extensions without modifying the parent.
    */
  def addTo(parent: Buildable): Unit

  /** Adds `child` as a subcomponent. Called alongside `child.addTo(this)`. */
  def add(child: Buildable): Unit

  /** @return this component's buildFile.xml element, including recursively
    *         built child elements -- the inverse of `build`.
    */
  def buildElement: XmlElement
}
