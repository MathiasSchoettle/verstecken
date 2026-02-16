package de.matsch.backend.db

abstract class DatabaseMigration(val version: String, val description: String) {
    abstract fun script(): String
}