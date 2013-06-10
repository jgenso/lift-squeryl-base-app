/*
 * Copyright (C) 2013 Genso Iniciativas Web.
 */

package code.lib.model

import net.liftweb.squerylrecord.RecordTypeMode
import RecordTypeMode._
import code.model._
import java.sql.DriverManager
import net.liftweb.http.{Req, LiftRules}
import net.liftweb.util.Props
import net.liftweb.common.Logger

object DbHelper extends Logger {

  def dropSchema() {
    inTransaction {
      try {
        DbSchema.drop
      } catch {
        case e => e.printStackTrace()
        throw e;
      }
    }
  }

  def createSchema() {
    inTransaction {
      try {
        DbSchema.create
      } catch {
        case e => e.printStackTrace()
        throw e;
      }
    }
  }

  def initDB() = if (Props.devMode) {
    info("Devel MODE")
    initH2()
  } else {
    initPostgresql() //ToDo Postgres
  }

  private def initH2() {

    Class.forName("org.h2.Driver")

    import org.squeryl.adapters.H2Adapter
    import net.liftweb.squerylrecord.SquerylRecord
    import org.squeryl.Session

    SquerylRecord.initWithSquerylSession(Session.create(
      DriverManager.getConnection("jdbc:h2:mem:dbname;DB_CLOSE_DELAY=-1", "sa", ""),
      new H2Adapter))

    LiftRules.liftRequest.append({
      case Req("console" ::_, _, _) => false
    })
  }

  private def initPostgresql() {

    Class.forName("org.postgresql.Driver")

    import org.squeryl.Session
    import org.squeryl.adapters._
    import net.liftweb.squerylrecord.SquerylRecord

    def connection = DriverManager.getConnection("jdbc:postgresql://localhost/liftbaseapp")

    SquerylRecord.initWithSquerylSession(Session.create(connection, new PostgreSqlAdapter))
  }
}
