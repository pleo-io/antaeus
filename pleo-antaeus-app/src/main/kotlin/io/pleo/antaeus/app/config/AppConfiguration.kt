package io.pleo.antaeus.app.config

/**
 * Environment driven application configuration
 */
object AppConfiguration {
    // Database
    val databaseUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:sqlite:/tmp/data.db"
    val databaseUser: String = System.getenv("DATABASE_USERNAME") ?: ""
    val databasePassword: String = System.getenv("DATABASE_PASSWORD") ?: ""
    val databaseDriver: String = System.getenv("DATABASE_DRIVER") ?: "org.sqlite.JDBC"

    // Task Schedule
    val defaultTaskDelayCron: String = System.getenv("DEFAULT_TASK_DELAY_CRON") ?: "0 0 0 1 * ?"

    // Billing Scheduler
    val billingSchedulingJobCron: String = System.getenv("BILLING_SCHEDULING_JOB_CRON") ?: "1 * * * * ?"
    val invoiceBillingQueue: String = System.getenv("INVOICE_BILLING_QUEUE")!!

    // Invoice billing workers
    val billingWorkerConcurrency: Int = System.getenv("BILLING_WORKER_CONCURRENCY")?.toInt() ?: 1
}
