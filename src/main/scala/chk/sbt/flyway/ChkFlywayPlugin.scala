package chk.sbt.flyway

import com.typesafe.config.{ConfigFactory, Config}
import org.flywaydb.core.Flyway
import org.flywaydb.sbt.FlywayPlugin
import FlywayPlugin.autoImport._
import sbt._
import Keys._

import scala.util.Try


object ChkFlywayPlugin extends AutoPlugin {
  override def requires = FlywayPlugin
  override def trigger = allRequirements

  object autoImport {
    val flywayConfigDirectory = settingKey[File]("The default configuration directory to search resource")
    val flywayConfigResource = settingKey[String]("The default configuration resource to load database configuration")
    val flywayMigrateScriptLocation = settingKey[File]("Location to store migration scripts")

    val flywayConfigPrefix = settingKey[String]("Default database config prefix")
    val flywayConfigDriverKey = settingKey[String]("Database Driver key in config")
    val flywayConfigUrlKey = settingKey[String]("Database Url key in config")
    val flywayConfigUserKey = settingKey[String]("Database User key in config")
    val flywayConfigPasswordKey = settingKey[String]("Database Password key in config")
    val flywayConfigTableKey = settingKey[String]("Database Schema Table key in config")
    val flywayConfigMigrationLocationKey = settingKey[String]("Database Migration Location key in config")
  }

  private case class DatabaseConfig(driver: String, url: String, user: String, password: String, schemaTable: String, migrateLocation: File)

  override def projectSettings: Seq[Setting[_]] = configSettings(Runtime) ++ inConfig(Test)(configSettings(Test))

  implicit class ConfigOps(config: Config) {
    def getString(path: String, default: => String): String = {
      Try(config.getString(path)).toOption.flatMap(Option(_)).getOrElse(default)
    }
  }

  def configSettings(conf: Configuration): Seq[Setting[_]] = {
    import autoImport._
    Seq[Setting[_]](
      flywayConfigDirectory <<= baseDirectory( _ / "conf" ),
      flywayConfigResource := "application",
      flywayConfigPrefix := "db",
      flywayConfigDriverKey := "driver",
      flywayConfigUrlKey := "url",
      flywayConfigUserKey := "user",
      flywayConfigPasswordKey := "password",
      flywayConfigTableKey := "schema.table",
      flywayConfigMigrationLocationKey := "migration",
      flywayMigrateScriptLocation <<= baseDirectory( _ / "db" / "migration" ),
      commands += flywayLoadConfig
    )
  }

  private def getDatabaseConfig(state: State, res: Option[String], prefixIn: Option[String]): Either[String, DatabaseConfig] = {
    val defaults = new Flyway()
    val resOpt = res.flatMap { e =>
      if(e.isEmpty) None
      else Option(e)
    }

    val extracted = Project.extract(state)
    val dir = extracted.get(autoImport.flywayConfigDirectory)
    val defaultRes = extracted.get(autoImport.flywayConfigResource)
    val prefix = prefixIn.getOrElse(extracted.get(autoImport.flywayConfigPrefix))
    val driverKey = extracted.get(autoImport.flywayConfigDriverKey)
    val urlKey = extracted.get(autoImport.flywayConfigUrlKey)
    val userKey = extracted.get(autoImport.flywayConfigUserKey)
    val pwdKey = extracted.get(autoImport.flywayConfigPasswordKey)
    val tableKey = extracted.get(autoImport.flywayConfigTableKey)
    val migrationLocationKey = extracted.get(autoImport.flywayConfigMigrationLocationKey)
    val migrateLocation = extracted.get(autoImport.flywayMigrateScriptLocation)

    val resPath = resOpt.getOrElse(defaultRes)
    val confRes = if(resPath.endsWith(".conf")) resPath
    else s"$resPath.conf"

    val configFile = dir / confRes
    if(!configFile.exists()) {
      Left(s"Config File [${configFile.getAbsolutePath}] Not Exists")

    } else {
      println(s"Loading Database Config from file [${configFile.getAbsolutePath}]")
      val config = ConfigFactory.parseFile(configFile)

      Try(config.getConfig(prefix)).toOption match {
        case None => Left(s"Cannot find the db with prefix [$prefix] in config")
        case Some(dbConfig) =>
          val driver = dbConfig.getString(s"$driverKey", "")
          val url = dbConfig.getString(s"$urlKey", "")
          val user = dbConfig.getString(s"$userKey", "")
          val password = dbConfig.getString(s"$pwdKey", "")
          val schemaTable = dbConfig.getString(s"$tableKey", defaults.getTable)
          val location = dbConfig.getString(s"$migrationLocationKey", migrateLocation.absolutePath)
          Right(DatabaseConfig(driver, url, user, password, schemaTable, file(location)))
      }
    }
  }

  private def _flywayLoadConfig(state: State, config: DatabaseConfig): State = {
    println(s"Setting Db Config $config")
    s"""set ${flywayDriver.key.label} := "${config.driver}"""" ::
    s"""set ${flywayUrl.key.label} := "${config.url}"""" ::
    s"""set ${flywayUser.key.label} := "${config.user}"""" ::
    s"""set ${flywayPassword.key.label} := "${config.password}"""" ::
    s"""set ${flywayTable.key.label} := "${config.schemaTable}"""" ::
    s"""set ${flywayLocations.key.label} := List("filesystem:${config.migrateLocation.getAbsolutePath}")""" ::
    state
  }

  lazy val flywayLoadConfig = Command.args("flywayLoadConfig", "<resource> <db>") { (state, args) =>
    val res = args.headOption
    val dbName = Try(args.apply(1)).toOption
    getDatabaseConfig(state, res, dbName) match {
      case Left(e) =>
        println(s"[error] $e")
        state

      case Right(config) =>
        _flywayLoadConfig(state, config)
    }
  }

}
