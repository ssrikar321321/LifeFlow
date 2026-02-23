package com.srikar.lifeflow.ui.work

import android.app.Application
import androidx.lifecycle.*
import com.srikar.lifeflow.data.db.AppDatabase
import com.srikar.lifeflow.data.model.*
import com.srikar.lifeflow.data.repository.TaskRepository
import kotlinx.coroutines.launch

class WorkViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TaskRepository(AppDatabase.getInstance(app).taskDao())

    val activeTasks = repo.activeTasks
    val completedTasks = repo.completedTasks
    val todayTasks = repo.getTasksDueToday()

    fun addTask(task: Task) = viewModelScope.launch { repo.addTask(task) }
    fun completeTask(id: Long) = viewModelScope.launch { repo.completeTask(id) }
    fun deleteTask(task: Task) = viewModelScope.launch { repo.deleteTask(task) }

    fun postponeTask(id: Long, daysAhead: Int = 1) = viewModelScope.launch {
        val newDeadline = System.currentTimeMillis() + daysAhead * 86_400_000L
        repo.postponeTask(id, newDeadline)
    }
}

// ─────────────────────────────────────────────────────────────────────────────

package com.srikar.lifeflow.ui.habits

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.srikar.lifeflow.data.db.AppDatabase
import com.srikar.lifeflow.data.model.Habit
import com.srikar.lifeflow.data.repository.HabitRepository
import com.srikar.lifeflow.notification.HabitReminderWorker
import kotlinx.coroutines.launch

class HabitsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = HabitRepository(AppDatabase.getInstance(app).habitDao())
    private val ctx: Context = app.applicationContext

    val habits = repo.activeHabits

    fun addHabit(habit: Habit) = viewModelScope.launch {
        val id = repo.addHabit(habit)
        HabitReminderWorker.scheduleForHabit(
            ctx, id, habit.name, habit.currentStreak,
            habit.reminderHour, habit.reminderMinute
        )
    }

    fun markDone(habit: Habit) = viewModelScope.launch {
        repo.markHabitDone(habit)
    }

    fun deleteHabit(habit: Habit) = viewModelScope.launch {
        HabitReminderWorker.cancelForHabit(ctx, habit.id)
        repo.deleteHabit(habit)
    }
}

// ─────────────────────────────────────────────────────────────────────────────

package com.srikar.lifeflow.ui.home

import android.app.Application
import androidx.lifecycle.*
import com.srikar.lifeflow.data.db.AppDatabase
import com.srikar.lifeflow.data.model.*
import com.srikar.lifeflow.data.repository.*
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val choreRepo = ChoreRepository(AppDatabase.getInstance(app).choreDao())
    private val groceryRepo = GroceryRepository(AppDatabase.getInstance(app).groceryDao())

    val allChores = choreRepo.allChores
    val overdueChores = choreRepo.getOverdueChores()
    val groceryItems = groceryRepo.allItems

    fun addChore(chore: Chore) = viewModelScope.launch { choreRepo.addChore(chore) }
    fun markChoreDone(chore: Chore) = viewModelScope.launch { choreRepo.markChoreDone(chore) }
    fun deleteChore(chore: Chore) = viewModelScope.launch { choreRepo.deleteChore(chore) }

    fun addGroceryItem(item: GroceryItem) = viewModelScope.launch { groceryRepo.addItem(item) }
    fun togglePurchased(id: Long, purchased: Boolean) = viewModelScope.launch { groceryRepo.markPurchased(id, purchased) }
    fun clearPurchasedItems() = viewModelScope.launch { groceryRepo.clearPurchased() }
    fun deleteGroceryItem(item: GroceryItem) = viewModelScope.launch { groceryRepo.deleteItem(item) }
}

// ─────────────────────────────────────────────────────────────────────────────

package com.srikar.lifeflow.ui.budget

import android.app.Application
import androidx.lifecycle.*
import com.srikar.lifeflow.data.db.AppDatabase
import com.srikar.lifeflow.data.model.Transaction
import com.srikar.lifeflow.data.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TransactionRepository(AppDatabase.getInstance(app).transactionDao())

    private val _currentMonth = MutableLiveData(
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    )
    val currentMonth: LiveData<String> = _currentMonth

    val transactions = _currentMonth.switchMap { repo.getTransactionsByMonth(it) }
    val budgetGoals = _currentMonth.switchMap { repo.getBudgetGoals(it) }

    private val _monthlyIncome = MutableLiveData(0.0)
    val monthlyIncome: LiveData<Double> = _monthlyIncome

    private val _monthlyExpenses = MutableLiveData(0.0)
    val monthlyExpenses: LiveData<Double> = _monthlyExpenses

    init { refreshSummary() }

    fun setMonth(month: String) {
        _currentMonth.value = month
        refreshSummary()
    }

    fun addTransaction(t: Transaction) = viewModelScope.launch {
        repo.addTransaction(t)
        refreshSummary()
    }

    fun deleteTransaction(t: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(t)
        refreshSummary()
    }

    private fun refreshSummary() = viewModelScope.launch {
        val month = _currentMonth.value ?: return@launch
        _monthlyIncome.value = repo.getMonthlyIncome(month)
        _monthlyExpenses.value = repo.getMonthlyExpenses(month)
    }

    val balance: LiveData<Double> = MediatorLiveData<Double>().apply {
        fun update() { value = (_monthlyIncome.value ?: 0.0) - (_monthlyExpenses.value ?: 0.0) }
        addSource(_monthlyIncome) { update() }
        addSource(_monthlyExpenses) { update() }
    }
}
