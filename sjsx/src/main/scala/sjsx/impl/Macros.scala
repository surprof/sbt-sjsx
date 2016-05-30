//     Project: sjsx
//      Module:
// Description:
package sjsx.impl
/*
import de.surfice.smacrotools.{JsCommonMacroTools, JsBlackboxMacroTools}
import scala.reflect.macros.blackbox
import scala.scalajs.js

private[sjsx] class Macros(val c: blackbox.Context) extends JsBlackboxMacroTools {
  import c.universe._

  private val jsDynamic = weakTypeOf[js.Dynamic]

  def require(global: c.Expr[String]) = q""


  def requireType[T: c.WeakTypeTag] = {
    val t = weakTypeOf[T]

    val (global,id) = analyzeSJSXModule(t)

    val tree = requireImpl(id,global,t)
    printTree(tree)
    tree
  }

  def requireInstance[T: c.WeakTypeTag](args: c.Expr[Any]*) = {
    val t = weakTypeOf[T]

    val (global,id) = analyzeSJSXModule(t)

    val tree = q"new scalajs.js.Dynamic.newInstance(${requireImpl(id,global,jsDynamic)})(..$args).asInstanceOf[$t]"
    printTree(tree)
    tree
  }

//}

//trait RequireMacros extends JsCommonMacroTools {
//  import c.universe._

  private val requireJS = setting("sjsx.require","require")

  protected[this] def analyzeSJSXModule(t: Type): (Option[String],Option[String]) = {
     val params = extractAnnotationParameters(t.typeSymbol,"sjsx.SJSXModule",Seq("id","global")) getOrElse {
      error(s"Cannot require $t without @sjsx.SJSXModule annotation")
      ???
    }
    val globalTree = params("global")
    val idTree = params("id")

    (globalTree,idTree) match {
      case (q"scala.None",q"$expr(..$id)") => (None,extractStringConstant(id.head))
      case (q"$expr(..$global)",q"scala.None") => (extractStringConstant(global.head),None)
      case (q"$e1(..$global)",q"$e2(..$id)") => (extractStringConstant(global.head),extractStringConstant(id.head))
      case _ =>
        error(s"@SJSXModule annotation on $t requires at least one argument != None")
        ???
    }
  }

  protected[this] def requireImpl(id: Option[String], global: Option[String], tpe: Type) = (id,global) match {
    case (None,Some(global)) => q"${selectGlobalDynamic(global)}.asInstanceOf[$tpe]"
    case (Some(id),None) => q"${selectGlobalDynamic(requireJS)}.apply($id).asInstanceOf[$tpe]"
    case _ => ???
  }
}
*/