package com.srikar.lifeflow.data.repository

import com.srikar.lifeflow.data.db.*
import com.srikar.lifeflow.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── TASK REPOSITORY ──────────────────────────────────────────────────────────

class TaskRepository(private val dao: TaskDao) {
    val activeTasks = dao.getActiveTasks()
    val completedTasks = dao.getCompletedTasks()

    fun getTasksDueToday(): androidx.lifecycle.LiveData<List<Task>> {
        val start = todayStartMillis()
        return dao.getTasksDueToday(start, start + 86_400_000L - 1)
    }

    suspend fun addTask(task: Task) = dao.insert(task)
    suspend fun updateTask(task: Task) = dao.update(task)
    suspend fun deleteTask(task: Task) = dao.delete(task)
    suspend fun completeTask(id: Long) = dao.markComplete(id)
    suspend fun postponeTask(id: Long, newDeadline: Long) = dao.postpone(id, newDeadline)
    suspend fun getTotalPostpones() = dao.getTotalPostponeCount()

    private fun todayStartMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

// ─── HABIT REPOSITORY ─────────────────────────────────────────────────────────

class HabitRepository(private val dao: HabitDao) {
    val activeHabits = dao.getActiveHabits()

    suspend fun addHabit(habit: Habit) = dao.insert(habit)
    suspend fun updateHabit(habit: Habit) = dao.update(habit)
    suspend fun deleteHabit(habit: Habit) = dao.delete(habit)
    fun getLogs(habitId: Long) = dao.getRecentLogs(habitId)

    suspend fun markHabitDone(habit: Habit) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = dao.getLogForDate(habit.id, today)
        if (existing?.completed == true) return  // already done

        dao.insertLog(HabitLog(habitId = habit.id, date = today, completed = true))

        // Recalculate streak
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val newStreak = if (habit.lastCompletedDate == yesterday || habit.lastCompletedDate == today) {
            habit.currentStreak + 1
        } else {
            1
        }
        val longestStreak = maxOf(habit.longestStreak, newStreak)
        dao.update(
            habit.copy(
                currentStreak = newStreak,
                longestStreak = longestStreak,
                totalCompletions = habit.totalCompletions + 1,
                lastCompletedDate = today
            )
        )
    }

    suspend fun isCompletedToday(habitId: Long): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return dao.getLogForDate(habitId, today)?.completed == true
    }
}

// ─── CHORE REPOSITORY ─────────────────────────────────────────────────────────

class ChoreRepository(private val dao: ChoreDao) {
    val allChores = dao.getAllChores()

    fun getOverdueChores(): androidx.lifecycle.LiveData<List<Chore>> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return dao.getOverdueChores(today)
    }

    suspend fun addChore(chore: Chore) = dao.insert(chore)
    suspend fun deleteChore(chore: Chore) = dao.delete(chore)

    suspend fun markChoreDone(chore: Chore) {
        val today = LocalDate.now()
        val nextDue = when (chore.frequency) {
            ChoreFrequency.DAILY -> today.plusDays(1)
            ChoreFrequency.EVERY_2_DAYS -> today.plusDays(2)
            ChoreFrequency.WEEKLY -> today.plusWeeks(1)
            ChoreFrequency.BIWEEKLY -> today.plusWeeks(2)
            ChoreFrequency.MONTHLY -> today.plusMonths(1)
        }
        dao.update(
            chore.copy(
                lastDoneDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                nextDueDate = nextDue.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        )
    }
}

// ─── GROCERY REPOSITORY ───────────────────────────────────────────────────────

class GroceryRepository(private val dao: GroceryDao) {
    val allItems = dao.getAllItems()
    val pendingItems = dao.getPendingItems()

    suspend fun addItem(item: GroceryItem) = dao.insert(item)
    suspend fun deleteItem(item: GroceryItem) = dao.delete(item)
    suspend fun markPurchased(id: Long, purchased: Boolean) = dao.markPurchased(id, purchased)
    suspend fun clearPurchased() = dao.clearPurchased()
}

// ─── TRANSACTION REPOSITORY ───────────────────────────────────────────────────

class TransactionRepository(private val dao: TransactionDao) {
    fun getTransactionsByMonth(month: String) = dao.getTransactionsByMonth(month)
    fun getBudgetGoals(month: String) = dao.getBudgetGoals(month)

    suspend fun addTransaction(t: Transaction) = dao.insert(t)
    suspend fun deleteTransaction(t: Transaction) = dao.delete(t)
    suspend fun getMonthlyIncome(month: String) = dao.getMonthlyIncome(month) ?: 0.0
    suspend fun getMonthlyExpenses(month: String) = dao.getMonthlyExpenses(month) ?: 0.0
    suspend fun setBudgetGoal(goal: BudgetGoal) = dao.insertBudgetGoal(goal)
}
