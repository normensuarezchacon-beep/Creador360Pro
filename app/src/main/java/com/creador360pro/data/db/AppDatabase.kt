package com.creador360pro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.creador360pro.data.dao.*
import com.creador360pro.data.model.*

@Database(
    entities = [
        IdeaItem::class,
        CalendarEvent::class,
        IncomeRecord::class,
        Contact::class,
        CollaborationHistory::class,
        DesignProject::class,
        VideoProject::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ideaDao(): IdeaDao
    abstract fun calendarDao(): CalendarDao
    abstract fun incomeDao(): IncomeDao
    abstract fun contactDao(): ContactDao
    abstract fun collaborationDao(): CollaborationDao
    abstract fun designProjectDao(): DesignProjectDao
    abstract fun videoProjectDao(): VideoProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "creador360_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
