package com.fernandocorreia.khepri.core.database.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.database.DatabaseCleaner
import com.fernandocorreia.khepri.core.database.KhepriDatabase
import com.fernandocorreia.khepri.core.database.snapshot.SnapshotDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DatabaseModule {
    @Binds @IntoSet internal abstract fun cleaner(cleaner: DatabaseCleaner): SignOutCleaner

    internal companion object {
        /** Version 1 has nothing to migrate from; a later version adds migrations and a test for each. */
        @Provides
        @Singleton
        internal fun database(@ApplicationContext context: Context): KhepriDatabase =
            Room.databaseBuilder(context, KhepriDatabase::class.java, KhepriDatabase.FILE_NAME)
                .setDriver(AndroidSQLiteDriver())
                .build()

        @Provides internal fun snapshots(database: KhepriDatabase): SnapshotDao = database.snapshots()
    }
}
