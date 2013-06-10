package bootstrap.liftweb

import net.liftweb._
import squerylrecord.SquerylRecord
import util._
import Helpers._

import common._
import http._
import sitemap._
import Loc._
import net.liftmodules.JQueryModule
import net.liftweb.http.js.jquery._
import org.squeryl.adapters.H2Adapter
import java.sql.DriverManager
import org.squeryl.Session
import net.liftweb.squerylrecord.RecordTypeMode._
import net.liftweb.http.S
import net.liftweb.util.LoanWrapper
import code.lib.model.DbHelper
import code.model.User


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("code")

    //Database connection
    DbHelper.initDB()

    S.addAround(new LoanWrapper {
      override def apply[T](f: => T): T = {
        val result = inTransaction {
          try {
            Right(f)
          } catch {
            case e: LiftFlowOfControlException => Left(e)
          }
        }

        result match {
          case Right(r) => r
          case Left(exception) => throw exception
        }
      }
    })

    // Uncomment the following line to create the database schema
    DbHelper.createSchema()

    // Build SiteMap
    val entries = List(
      Menu.i("Home") / "index", // the simple way to declare a menu

      // more complex because this menu allows anything in the
      // /static path to be visible
      Menu(Loc("Static", Link(List("static"), true, "/static/index"), 
	       "Static Content")))

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => User.sitemapMutator(SiteMap(entries: _*)))

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    //Init the jQuery module, see http://liftweb.net/jquery for more information.
    LiftRules.jsArtifacts = JQueryArtifacts
    JQueryModule.InitParam.JQuery=JQueryModule.JQuery172
    JQueryModule.init()

  }
}
