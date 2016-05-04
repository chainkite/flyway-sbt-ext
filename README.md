# flyway-sbt-ext

Load datasource values from config file for flyway-sbt.

The artifact version will be always the same as flyway-sbt version from 4.0

Usage:
```
// project/plugin.sbt
addSbtPlugin("chainkite" % "flyway-sbt-ext" % "4.0")
```
```
// conf/application.conf
db {
  url = "jdbc:postgresql://localhost:5432/test"
  user = "test"
  password = "test"
  migration= path/to/flyway_migration_scripts_dir
}
```
```
sbt> flywayLoadConfig application db
```
Then you can continue using flyway sbt commands with configured datasource values.

FYI: [flyway-sbt commands](https://flywaydb.org/documentation/sbt/)
