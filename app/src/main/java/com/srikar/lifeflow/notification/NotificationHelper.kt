package com.srikar.lifeflow.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

object NotificationHelper {
    const val CHANNEL_TASKS = "lifeflow_tasks"
    const val CHANNEL_HABITS = "lifeflow_habits"
    const val CHANNEL_CHORES = "lifeflow_chores"
    const val CHANNEL_BUDGET = "lifeflow_budget"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            Triple(CHANNEL_TASKS, "Work Tasks", "Reminders for research tasks and deadlines"),
            Triple(CHANNEL_HABITS, "Daily Habits", "Exercise, meditation, and routine reminders"),
            Triple(CHANNEL_CHORES, "Home Chores", "Cleaning, cooking, and shopping reminders"),
            Triple(CHANNEL_BUDGET, "Budget", "Budget alerts and reminders")
        ).forEach { (id, name, desc) ->
            nm.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = desc
                }
            )
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        message: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }
}

// ─── DAILY REMINDER WORKER ────────────────────────────────────────────────────
// Fires every morning to summarize today's tasks, habits due, and overdue chores

class DailyReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = com.srikar.lifeflow.data.db.AppDatabase.getInstance(context)
        val today = java.time.LocalDate.now().toString()

        // Count overdue chores
        // (In a full impl, query synchronously via suspending DAO)

        NotificationHelper.showNotification(
            context,
            id = 1001,
            channel = NotificationHelper.CHANNEL_TASKS,
            title = "☀️ Good morning, Srikar!",
            message = "Open LifeFlow to see today's tasks and habits."
        )
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            // Schedule for 8 AM every day
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }
    }
}

// ─── HABIT REMINDER WORKER ────────────────────────────────────────────────────

class HabitReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitName = inputData.getString("habit_name") ?: "habit"
        val streak = inputData.getInt("streak", 0)
        val streakMsg = if (streak > 0) " 🔥 $streak day streak!" else ""

        NotificationHelper.showNotification(
            context,
            id = (2000 + inputData.getLong("habit_id", 0)).toInt(),
            channel = NotificationHelper.CHANNEL_HABITS,
            title = "Time for: $habitName",
            message = "Don't break your routine.$streakMsg"
        )
        return Result.success()
    }

    companion object {
        fun scheduleForHabit(
            context: Context,
            habitId: Long,
            habitName: String,
            streak: Int,
            hour: Int,
            minute: Int
        ) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val data = workDataOf(
                "habit_id" to habitId,
                "habit_name" to habitName,
                "streak" to streak
            )

            val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "habit_$habitId",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelForHabit(context: Context, habitId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("habit_$habitId")
        }
    }
}

// ─── CHORE REMINDER WORKER ────────────────────────────────────────────────────

class ChoreReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val choreName = inputData.getString("chore_name") ?: "chore"
        val room = inputData.getString("room") ?: ""

        NotificationHelper.showNotification(
            context,
            id = (3000 + inputData.getLong("chore_id", 0)).toInt(),
            channel = NotificationHelper.CHANNEL_CHORES,
            title = "🏠 Chore due: $choreName",
            message = if (room.isNotEmpty()) "Room: $room — don't postpone it!" else "Don't postpone it!"
        )
        return Result.success()
    }
}

// ─── BOOT RECEIVER ────────────────────────────────────────────────────────────
// Re-schedules workers after phone restart

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyReminderWorker.schedule(context)
        }
    }
}
