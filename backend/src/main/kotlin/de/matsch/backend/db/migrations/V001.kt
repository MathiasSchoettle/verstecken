package de.matsch.backend.db.migrations

import de.matsch.backend.db.DBMigration
import de.matsch.backend.db.DatabaseMigration

@DBMigration
class V001 : DatabaseMigration("001", "Initialize DB") {
    @Override
    override fun script() = """
        CREATE TABLE messages (
            id BIGSERIAL PRIMARY KEY,
            text VARCHAR(50) NOT NULL
        )
    """
}