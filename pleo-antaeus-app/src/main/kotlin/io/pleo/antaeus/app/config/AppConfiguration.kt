package io.pleo.antaeus.app.config

object AppConfiguration {
    val databaseUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:sqlite:/tmp/data.db"
    val databaseUser: String = System.getenv("DATABASE_USERNAME") ?: ""
    val databasePassword: String = System.getenv("DATABASE_PASSWORD") ?: ""
    val databaseDriver: String = System.getenv("DATABASE_DRIVER") ?: "org.sqlite.JDBC"
}
