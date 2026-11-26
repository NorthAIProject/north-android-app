package com.fernandocorreia.khepri.core.database

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider

/** An in-memory database with the production driver. */
internal fun testDatabase(): KhepriDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhepriDatabase::class.java)
        .setDriver(AndroidSQLiteDriver())
        .build()
