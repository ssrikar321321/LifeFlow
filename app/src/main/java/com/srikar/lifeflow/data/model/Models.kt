package com.srikar.lifeflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

// ─── WORK ──────────────────────────────────────────────────────────────────────

enum class TaskPriority { LOW, MEDIUM, HIGH, CRITICAL }
enum class TaskCategory { RESEARCH, PAPER_WRITING, MEETING, ADMIN, UKAEA_PREP, OTHER }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.OTHER,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadline: Long? = null,           // epoch millis
    val reminderTime: Long? = null,       // epoch millis
    val isCompleted: Boolean = false,
    val postponeCount: Int = 0,           // tracks laziness 😄
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

// ─── HABITS ────────────────────────────────────────────────────────────────────

enum class HabitFrequency { DAILY, WEEKDAYS, WEEKENDS, CUSTOM }

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "⭐",               // emoji icon
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val reminderHour: Int = 7,             // 24h
    val reminderMinute: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val lastCompletedDate: String? = null, // ISO date string yyyy-MM-dd
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String,      // yyyy-MM-dd
    val completed: Boolean
)

// ─── HOME CHORES ───────────────────────────────────────────────────────────────

enum class ChoreRoom { KITCHEN, WASHROOM, BEDROOM, LIVING_ROOM, GENERAL }
enum class ChoreFrequency { DAILY, EVERY_2_DAYS, WEEKLY, BIWEEKLY, MONTHLY }

@Entity(tableName = "chores")
data class Chore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val room: ChoreRoom = ChoreRoom.GENERAL,
    val frequency: ChoreFrequency = ChoreFrequency.WEEKLY,
    val reminderHour: Int = 9,
    val lastDoneDate: String? = null,      // yyyy-MM-dd
    val nextDueDate: String? = null,       // yyyy-MM-dd
    val isActive: Boolean = true
)

// ─── GROCERY ───────────────────────────────────────────────────────────────────

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String = "",
    val category: String = "General",
    val isPurchased: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

// ─── BUDGET ────────────────────────────────────────────────────────────────────

enum class TransactionType { INCOME, EXPENSE }
enum class ExpenseCategory {
    GROCERIES, RENT, UTILITIES, TRANSPORT, HEALTH,
    DINING, CLOTHING, ENTERTAINMENT, RESEARCH, OTHER
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val date: String,      // yyyy-MM-dd
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: ExpenseCategory,
    val monthlyLimit: Double,
    val month: String  // yyyy-MM
)
