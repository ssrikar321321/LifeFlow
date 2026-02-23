package com.srikar.lifeflow.ui.habits

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.srikar.lifeflow.R
import com.srikar.lifeflow.data.model.*

class HabitsFragment : Fragment() {

    private val viewModel: HabitsViewModel by viewModels()
    private lateinit var adapter: HabitAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HabitAdapter(
            onDone = { viewModel.markDone(it) },
            onDelete = { viewModel.deleteHabit(it) }
        )

        view.findViewById<RecyclerView>(R.id.rv_habits).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HabitsFragment.adapter
        }

        viewModel.habits.observe(viewLifecycleOwner) { habits ->
            adapter.submitList(habits)

            // Update summary
            val totalStreak = habits.sumOf { it.currentStreak }
            view.findViewById<TextView>(R.id.tv_total_streak).text =
                "Combined streak: $totalStreak days 🔥"
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_habit).setOnClickListener {
            showAddHabitDialog()
        }
    }

    private fun showAddHabitDialog() {
        val defaultHabits = listOf(
            "🏃 Morning Run" to 6,
            "🧘 Meditation" to 7,
            "💪 Workout" to 6,
            "📚 Reading" to 20,
            "🍳 Cook Dinner" to 18,
            "💊 Take Vitamins" to 8,
            "Custom..." to -1
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Add Habit")
            .setItems(defaultHabits.map { it.first }.toTypedArray()) { _, which ->
                val selected = defaultHabits[which]
                if (selected.second == -1) {
                    showCustomHabitDialog()
                } else {
                    viewModel.addHabit(Habit(
                        name = selected.first,
                        reminderHour = selected.second,
                        reminderMinute = 0
                    ))
                }
            }
            .show()
    }

    private fun showCustomHabitDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_habit, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_habit_name)
        val etIcon = dialogView.findViewById<EditText>(R.id.et_icon)
        val npHour = dialogView.findViewById<NumberPicker>(R.id.np_hour).apply {
            minValue = 0; maxValue = 23; value = 7
        }
        val npMinute = dialogView.findViewById<NumberPicker>(R.id.np_minute).apply {
            minValue = 0; maxValue = 59; value = 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Custom Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addHabit(Habit(
                        name = name,
                        icon = etIcon.text.toString().ifEmpty { "⭐" },
                        reminderHour = npHour.value,
                        reminderMinute = npMinute.value
                    ))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ─── ADAPTER ──────────────────────────────────────────────────────────────────

class HabitAdapter(
    private val onDone: (Habit) -> Unit,
    private val onDelete: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.HabitViewHolder>(DIFF) {

    inner class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_habit_name)
        val tvStreak: TextView = view.findViewById(R.id.tv_streak)
        val tvLongest: TextView = view.findViewById(R.id.tv_longest_streak)
        val btnDone: View = view.findViewById(R.id.btn_mark_done)
        val btnDelete: View = view.findViewById(R.id.btn_delete_habit)
        val cardView: View = view.findViewById(R.id.card_habit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HabitViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
    )

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = getItem(position)
        holder.tvName.text = habit.name
        holder.tvStreak.text = "🔥 ${habit.currentStreak} day streak"
        holder.tvLongest.text = "Best: ${habit.longestStreak} days"

        // Check if completed today
        val today = java.time.LocalDate.now().toString()
        val isDoneToday = habit.lastCompletedDate == today
        holder.btnDone.alpha = if (isDoneToday) 0.4f else 1.0f
        (holder.btnDone as? Button)?.text = if (isDoneToday) "✅ Done!" else "Mark Done"

        holder.btnDone.isEnabled = !isDoneToday
        holder.btnDone.setOnClickListener { if (!isDoneToday) onDone(habit) }
        holder.btnDelete.setOnClickListener { onDelete(habit) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Habit>() {
            override fun areItemsTheSame(a: Habit, b: Habit) = a.id == b.id
            override fun areContentsTheSame(a: Habit, b: Habit) = a == b
        }
    }
}
