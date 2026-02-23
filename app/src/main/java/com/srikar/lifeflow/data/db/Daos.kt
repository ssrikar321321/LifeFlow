package com.srikar.lifeflow.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.srikar.lifeflow.data.model.*

// ─── TASK DAO ──────────────────────────────────────────────────────────────────

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY deadline ASC, priority DESC")
    fun getActiveTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC LIMIT 30")
    fun getCompletedTasks(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE deadline BETWEEN :startOfDay AND :endOfDay AND isCompleted = 0")
    fun getTasksDueToday(startOfDay: Long, endOfDay: Long): LiveData<List<Task>>

    @Query("SELECT SUM(postponeCount) FROM tasks")
    suspend fun getTotalPostponeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :time WHERE id = :id")
    suspend fun markComplete(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET postponeCount = postponeCount + 1, deadline = :newDeadline WHERE id = :id")
    suspend fun postpone(id: Long, newDeadline: Long)
}

// ─── HABIT DAO ─────────────────────────────────────────────────────────────────

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveHabits(): LiveData<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC LIMIT 30")
    fun getRecentLogs(habitId: Long): LiveData<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLogForDate(habitId: Long, date: String): HabitLog?

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND completed = 1 AND date >= :since")
    suspend fun getCompletionCountSince(habitId: Long, since: String): Int
}

// ─── CHORE DAO ─────────────────────────────────────────────────────────────────

@Dao
interface ChoreDao {
    @Query("SELECT * FROM chores WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getAllChores(): LiveData<List<Chore>>

    @Query("SELECT * FROM chores WHERE nextDueDate <= :today AND isActive = 1")
    fun getOverdueChores(today: String): LiveData<List<Chore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chore: Chore): Long

    @Update
    suspend fun update(chore: Chore)

    @Delete
    suspend fun delete(chore: Chore)
}

// ─── GROCERY DAO ───────────────────────────────────────────────────────────────

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_items ORDER BY isPurchased ASC, category ASC")
    fun getAllItems(): LiveData<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE isPurchased = 0")
    fun getPendingItems(): LiveData<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GroceryItem): Long

    @Update
    suspend fun update(item: GroceryItem)

    @Delete
    suspend fun delete(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE isPurchased = 1")
    suspend fun clearPurchased()

    @Query("UPDATE grocery_items SET isPurchased = :purchased WHERE id = :id")
    suspend fun markPurchased(id: Long, purchased: Boolean)
}

// ─── TRANSACTION DAO ───────────────────────────────────────────────────────────

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date LIKE :month || '%' ORDER BY date DESC")
    fun getTransactionsByMonth(month: String): LiveData<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND date LIKE :month || '%'")
    suspend fun getMonthlyIncome(month: String): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND date LIKE :month || '%'")
    suspend fun getMonthlyExpenses(month: String): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND category = :category AND date LIKE :month || '%'")
    suspend fun getExpenseByCategory(category: String, month: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Delete
    suspend fun delete(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(goal: BudgetGoal): Long

    @Query("SELECT * FROM budget_goals WHERE month = :month")
    fun getBudgetGoals(month: String): LiveData<List<BudgetGoal>>
}
