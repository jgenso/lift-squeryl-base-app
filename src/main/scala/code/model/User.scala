package code.model

/**
 * Created with IntelliJ IDEA.
 * User: j2
 * Date: 10-06-13
 * Time: 04:31 PM
 * To change this template use File | Settings | File Templates.
 */
import net.liftweb.squerylrecord.KeyedRecord
import net.liftweb.record.field._
import org.squeryl.annotations.Column
import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.squerylrecord.RecordTypeMode._
import net.liftweb.util.{CssSel, CssBind, FieldError}
import net.liftweb.common._
import net.liftweb.util.Helpers._
import net.liftweb.http.{SHtml, S, RedirectResponse}
import net.liftweb.common._
import xml.{NodeSeq, Node, Elem}
import net.liftweb.http.js.JsCmds.FocusOnLoad
import net.liftweb.sitemap.Menu
import code.lib.proto.{MetaMegaProtoUser, MegaProtoUser}
import net.liftweb.http.S.LFuncHolder
import net.liftweb.sitemap.Loc.If

class User private() extends MegaProtoUser[User] with Record[User] with KeyedRecord[Long] {

  override def meta: MetaRecord[User] = User

  lazy val schema = DbSchema.users

  @Column(name="id")
  override lazy val idField = new LongField(this, 0)

  //verify if the email is unique
  override protected def valUnique(errorMsg: => String)(email: String): List[FieldError] = {
    from(DbSchema.users)(u => where(u.email === email and u.id <> id) select(u)).headOption match {
      case None => Nil
      case Some(u: User) => List(FieldError(this.email, errorMsg))
    }
  }

  //for insert or update schema
  override def saveTheRecord(): Box[User] = {
    tryo(DbSchema.users.insertOrUpdate(this))
  }


  def page(curPage: Int, itemsPerPage: Int): List[User] =
    from(DbSchema.users)(u => select(u)).page(curPage, itemsPerPage).toList

  def count: Int = DbSchema.users.size

  def delete: Int = schema.deleteWhere(user => user.id === id)

}

object User extends User with MetaRecord[User] with MetaMegaProtoUser[User] {

  override def screenWrap: Box[Elem] = Full(<lift:surround with="default" at="content">
    <lift:bind /></lift:surround>)

  override protected def userFromStringId(id: String): Box[TheUserType] = {
    tryo(DbSchema.users.lookup(id.toLong)) match {
      case Full(Some(u: User)) => Full(u)
      case _ => Empty
    }
  }

  def findById(id: String): Box[User] = {
    asLong(id).dmap(Empty: Box[User])(DbSchema.users.lookup(_).map(Full(_)).getOrElse(Empty))
  }

  def findUserByUniqueId(id: String): Box[TheUserType] = {
    from(DbSchema.users)(u => where(u.uniqueId === id) select(u)).headOption match {
      case None => Empty
      case Some(u: User) => Full(u)
    }
  }

  //ToDo Should be findByEmail  used in ProtoUser.login ---search by email
  def findUserByUserName(email: String): Box[TheUserType] = {
    from(DbSchema.users)(u => where(u.getEmail === email) select(u)).headOption match {
      case None =>
        Empty
      case Some(u: User) =>
        Full(u)
    }
  }


  def notLoggedIn: If = If(() => ! User.loggedIn_?, () => RedirectResponse("/index"))

  //template for login
  override def loginXhtml: Elem = {
    S.runTemplate(List("templates-hidden", "_signinform")).dmap(<span></span>)(s => <div>{loginCss(s)}</div>)
  }

  //combinator for log and action form
  def loginCss: CssSel = {
    "data-lift-id=title *" #> S.?("log.in") &
      "form [action]" #> S.uri
  }

  //template for change the password
  override def changePasswordXhtml: Elem = {
    S.runTemplate(List("templates-hidden", "_changepasswordform")).
      dmap(<span></span>)(s => <div>{changePasswordCss(s)}</div>)
  }

  //for change the password
  def changePasswordCss: CssSel = {
    val user = currentUser openOr User.createRecord
    var oldPassword = ""
    var newPassword: List[String] = Nil

    def testAndSet() {
      if (!user.testPassword(Full(oldPassword))) S.error(S.?("wrong.old.password"))
      else {
        user.setPasswordFromListString(newPassword)
        user.validate match {
          case Nil => user.saveTheRecord(); S.notice(S.?("password.changed")); S.redirectTo(homePage)
          case xs => S.error(xs)
        }
      }
    }
    "data-lift-id=title *" #> S.?("change.password") &
      "form [action]" #> S.uri &
      "@old-password" #> SHtml.password("", s => oldPassword = s) &
      "@new-password" #> SHtml.password_*("", LFuncHolder(s => newPassword = newPassword ++ s)) &
      "type=submit" #> changePasswordSubmitButton(S.?("change"), testAndSet _)
  }

  //template for search the password forgotten
  override def lostPasswordXhtml: Elem = {
    S.runTemplate(List("templates-hidden", "_lostpasswordform")).
      dmap(<span></span>)(s => <div>{lostPasswordCss(s)}</div>)
  }
  //send an email for reset the pasword
  def lostPasswordCss: CssSel = {
    "data-lift-id=title *" #> S.?("reset.your.password") &
      "form [action]" #> S.uri &
      "@username" #> SHtml.email("", sendPasswordReset _) &
      "type=submit" #> lostPasswordSubmitButton(S.?("send.it"))
  }

  //template for reset the password
  def passwordResetXhtml(user: User): Elem = {
    S.runTemplate(List("templates-hidden", "_passwordresetform")).
      dmap(<span></span>)(s => <div>{passwordResetCss(user)(s)}</div>)
  }
  //change the password
  def passwordResetCss(user: User): CssSel = {
    var newPassword: List[String] = Nil

    def testAndSet() {
      user.setPasswordFromListString(newPassword)
      user.validate match {
        case Nil =>
          DbSchema.users.update(user.resetUniqueId())
          S.redirectTo(homePage, () => S.notice(S.?("password.changed")))
        case xs => S.error(xs)
      }
    }
    "form [action]" #> S.uri &
      "data-lift-id=title *" #> S.?("reset.your.password") &
      ".password *" #> S.?("enter.your.new.password") &
      ".password2 *" #> S.?("repeat.your.new.password") &
      "@password" #> SHtml.password("", s => newPassword = List(s)) &
      "@password2" #> SHtml.password("", s => newPassword = newPassword ++ List(s)) &
      "type=submit" #> changePasswordSubmitButton(S.?("change"), testAndSet _)
  }

  //if the id user not be found not permit change the password
  override def passwordReset(id: String): Elem = {
    findUserByUniqueId(id) match {
      case Full(u: User) =>
        passwordResetXhtml(u)
      case _ =>
        S.redirectTo(homePage, () => S.error(S.?("password.link.invalid")))
    }
  }
}