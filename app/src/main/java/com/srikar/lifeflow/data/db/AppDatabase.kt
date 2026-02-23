package com.srikar.lifeflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.srikar.lifeflow.data.model.*

class Converters {
    @TypeConverter fun fromTaskPriority(v: TaskPriority) = v.name
    @TypeConverter fun toTaskPriority(v: String) = TaskPriority.valueOf(v)

    @TypeConverter fun fromTaskCategory(v: TaskCategory) = v.name
    @TypeConverter fun toTaskCategory(v: String) = TaskCategory.valueOf(v)

    @TypeConverter fun fromHabitFrequency(v: HabitFrequency) = v.name
    @TypeConverter fun toHabitFrequency(v: String) = HabitFrequency.valueOf(v)

    @TypeConverter fun fromChoreRoom(v: ChoreRoom) = v.name
    @TypeConverter fun toChoreRoom(v: String) = ChoreRoom.valueOf(v)

    @TypeConverter fun fromChoreFrequency(v: ChoreFrequency) = v.name
    @TypeConverter fun toChoreFrequency(v: String) = ChoreFrequency.valueOf(v)

    @TypeConverter fun fromTransactionType(v: TransactionType) = v.name
    @TypeConverter fun toTransactionType(v: String) = TransactionType.valueOf(v)

    @TypeConverter fun fromExpenseCategory(v: ExpenseCategory) = v.name
    @TypeConverter fun toExpenseCategory(v: String) = ExpenseCategory.valueOf(v)
}

@Database(
    entities = [
        Task::class,
        Habit::class, HabitLog::class,
        Chore::class,
        GroceryItem::class,
        Transaction::class, BudgetGoal::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun choreDao(): ChoreDao
    abstract fun groceryDao(): GroceryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifeflow_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
