package de.matsch.backend.db

import org.slf4j.Logger
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.stereotype.Service

@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
class DatabaseVersioningService(val db: JdbcTemplate, val migrations: List<DatabaseMigration>, val logger: Logger) : ApplicationRunner {

    @Override
    override fun run(args: ApplicationArguments) {
        initMigrationTable()

        val executedMigrations = loadExecutedMigrationVersions()

        val pendingMigrations = migrations
            .filter { it.version !in executedMigrations }
            .sortedBy { it.version }

        if (pendingMigrations.isEmpty()) {
            logger.info("No pending migrations. Finished with migration!")
            return
        }

        logger.info("Found ${pendingMigrations.size} pending migrations")

        migrations.forEach { executeMigration(it) }
    }

    private fun executeMigration(migration: DatabaseMigration) {

        logger.info("Executing migration V${migration.version}: ${migration.description}")

        val startTime = System.currentTimeMillis()

        try {
            val script = migration.script()
            db.execute(script)

            val executionTime = System.currentTimeMillis() - startTime

            recordMigration(migration, executionTime, true)
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            val errorMessage = "${e::class.simpleName}: ${e.message}"

            recordMigration(migration, executionTime, false, errorMessage)

            logger.error("Failed to execute migration V${migration.version}", e)
            throw RuntimeException("Migration failed: V${migration.version}", e)
        }
    }

    private fun recordMigration(migration: DatabaseMigration, executionTime: Long, successful: Boolean, errorMessage: String? = null) {
        // Delete any existing record for this version
        db.update(
            "DELETE FROM schema_version WHERE version = ?",
            migration.version
        )

        db.update(
            """
            INSERT INTO schema_version 
            (version, description, execution_time_ms, success, error_message) 
            VALUES (?, ?, ?, ?, ?)
            """,
            migration.version, migration.description, executionTime, successful, errorMessage
        )
    }

    private fun initMigrationTable() {
        db.execute("""
        CREATE TABLE IF NOT EXISTS schema_version (
            id BIGSERIAL PRIMARY KEY,
            version VARCHAR(50) NOT NULL UNIQUE,
            description VARCHAR(255),
            executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            execution_time_ms BIGINT,
            success BOOLEAN NOT NULL,
            error_message TEXT
        )
    """)
    }

    private fun loadExecutedMigrationVersions(): Set<String?> {
        return db.queryForList<String>(
            "SELECT version FROM schema_version WHERE success = true"
        ).toSet()
    }
}