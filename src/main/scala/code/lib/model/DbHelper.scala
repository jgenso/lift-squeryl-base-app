/*
 * Copyright (C) 2013 Genso Iniciativas Web.
 */

package code.lib.model

import net.liftweb.squerylrecord.RecordTypeMode
import RecordTypeMode._
import code.model._

object DbHelper {

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
}
