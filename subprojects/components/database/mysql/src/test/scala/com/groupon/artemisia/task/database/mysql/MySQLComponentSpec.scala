/*
 * Copyright (c) 2016, Groupon, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of GROUPON nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.groupon.artemisia.task.database.mysql

import java.nio.file.Paths
import com.typesafe.config.ConfigFactory
import com.groupon.artemisia.TestSpec
import com.groupon.artemisia.core.Keywords

/**
  * Created by chlr on 6/6/16.
  */
class MySQLComponentSpec extends TestSpec {

  val component = new MySQLComponent("MySQL")

  "MySQLComponent" must "dispatch SQLRead when requested" in {
    val config = ConfigFactory parseString
      s"""
        |{
        |  dsn = {
        |     ${Keywords.Connection.HOSTNAME} = dummy_host
        |     ${Keywords.Connection.USERNAME} = user
        |     ${Keywords.Connection.PASSWORD} = pass
        |     ${Keywords.Connection.DATABASE} = db
        |  }
        |  sql = "SELECT * FROM table"
        |}
      """.stripMargin

    val task = component.dispatchTask("SQLRead", "sql_read", config).asInstanceOf[SQLRead]
    task.sql must be ("SELECT * FROM table")
    task.connectionProfile.port must be (3306)
    task.connectionProfile.default_database must be ("db")
  }

  it must "dispatch SQLExecute when request" in {

    val config = ConfigFactory parseString
      s"""
         |{
         |  ${MySQLComponentSpec.getDSN(pass = "pass")}
         |  sql = "SELECT * FROM table"
         |}
      """.stripMargin

    val task: SQLExecute = component.dispatchTask("SQLExecute", "sql_read", config).asInstanceOf[SQLExecute]
    task.sql must be ("SELECT * FROM table")
    task.connectionProfile.username must be ("user")
    task.connectionProfile.password must be ("pass")
  }


  it must "dispatch LoadToTable when request" in {


    val config = ConfigFactory parseString
      s"""
         |{
         |  ${MySQLComponentSpec.getDSN()}
         |  destination-table = test_table
         |       load = {
         |         header =  yes
         |         delimiter = "\\u0001"
         |         quoting = no,
         |       }
         |  location = ${this.getClass.getResource("/dummy_load_file.txt").getFile}
         |}
      """.stripMargin

    val task = component.dispatchTask("SQLLoad", "sql_read", config).asInstanceOf[LoadFromFile]
    task.tableName must be ("test_table")
    task.loadSetting.delimiter must be ('\u0001')
    Paths.get(task.location.toString).getFileName.toString must be ("dummy_load_file.txt")
  }


  it must "dispatch ExportToFile when request" in {
    val config = ConfigFactory parseString
      s"""
        | {
        |  ${MySQLComponentSpec.getDSN()}
        |    export = {
        |      delimiter = "\\t"
        |      header = yes
        |    }
        |    location = target/output.txt
        |    sql = "select * from dual"
        |  }
      """.stripMargin
    val task = component.dispatchTask("SQLExport", "sql_read", config).asInstanceOf[ExportToFile]
    task.sql must be ("select * from dual")
    task.exportSetting.delimiter must be ('\t')
    task.exportSetting.header must be (true)
  }


  it must "spew out doc for all tasks" in {
    for (task <- component.tasks) {
      component.taskDoc(task.taskName).trim.length must be > 1
    }
  }


}


object MySQLComponentSpec {

    def getDSN(host: String = "dummy_host", user: String = "user", pass: String = "password", db: String = "db", port: Int = -1) = {
      s"""
          |dsn = {
          | ${Keywords.Connection.HOSTNAME} = $host
          | ${Keywords.Connection.USERNAME} = $user
          | ${Keywords.Connection.PASSWORD} = $pass
          | ${Keywords.Connection.DATABASE} = $db
          | ${Keywords.Connection.PORT} = $port
          |}
          |
     """.stripMargin
   }
}