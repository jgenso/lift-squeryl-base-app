package code.model

/**
 * Created with IntelliJ IDEA.
 * User: j2
 * Date: 10-06-13
 * Time: 04:36 PM
 * To change this template use File | Settings | File Templates.
 */
import net.liftweb.squerylrecord.RecordTypeMode._
import org.squeryl.Schema

object DbSchema extends Schema {

  val users = table[User]("user")

}
