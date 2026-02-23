package com.srikar.lifeflow.ui.work

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.srikar.lifeflow.R
import com.srikar.lifeflow.data.model.*
import java.text.SimpleDateFormat
import java.util.*

class WorkFragment : Fragment() {

    private val viewModel: WorkViewModel by viewModels()
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_work, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TaskAdapter(
            onComplete = { viewModel.completeTask(it.id) },
            onPostpone = { task ->
                // Show postpone dialog - adds 1 day and logs it
                AlertDialog.Builder(requireContext())
                    .setTitle("Postpone Task")
                    .setMessage("Postpone '${task.title}' by how many days?")
                    .setItems(arrayOf("1 day", "2 days", "3 days", "1 week")) { _, which ->
                        val days = when (which) { 0 -> 1; 1 -> 2; 2 -> 3; else -> 7 }
                        viewModel.postponeTask(task.id, days)
                        Snackbar.make(view,
                            "Postponed by $days day(s). Total postpones: ${task.postponeCount + 1} 🤦",
                            Snackbar.LENGTH_LONG).show()
                    }
                    .show()
            },
            onDelete = { viewModel.deleteTask(it) }
        )

        view.findViewById<RecyclerView>(R.id.rv_tasks).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WorkFragment.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        viewModel.activeTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
            view.findViewById<TextView>(R.id.tv_task_count).text =
                "${tasks.size} pending tasks"
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_task).setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etDesc = dialogView.findViewById<EditText>(R.id.et_description)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinner_priority)
        val tvDeadline = dialogView.findViewById<TextView>(R.id.tv_deadline)

        var selectedDeadline: Long? = null

        // Populate spinners
        ArrayAdapter.createFromResource(requireContext(), R.array.task_categories,
            android.R.layout.simple_spinner_item).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = it }

        ArrayAdapter.createFromResource(requireContext(), R.array.task_priorities,
            android.R.layout.simple_spinner_item).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPriority.adapter = it }

        tvDeadline.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d, 23, 59)
                selectedDeadline = cal.timeInMillis
                tvDeadline.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Work Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    val category = TaskCategory.values()[spinnerCategory.selectedItemPosition]
                    val priority = TaskPriority.values()[spinnerPriority.selectedItemPosition]
                    viewModel.addTask(Task(
                        title = title,
                        description = etDesc.text.toString().trim(),
                        category = category,
                        priority = priority,
                        deadline = selectedDeadline
                    ))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ─── ADAPTER ──────────────────────────────────────────────────────────────────

class TaskAdapter(
    private val onComplete: (Task) -> Unit,
    private val onPostpone: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DIFF) {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_task_title)
        val tvCategory: TextView = view.findViewById(R.id.tv_category)
        val tvDeadline: TextView = view.findViewById(R.id.tv_deadline)
        val tvPostpone: TextView = view.findViewById(R.id.tv_postpone_count)
        val btnComplete: View = view.findViewById(R.id.btn_complete)
        val btnPostpone: View = view.findViewById(R.id.btn_postpone)
        val btnDelete: View = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TaskViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
    )

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.tvTitle.text = task.title
        holder.tvCategory.text = task.category.name.replace("_", " ")
        holder.tvDeadline.text = task.deadline?.let {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it))
        } ?: "No deadline"

        // Show postpone count — a little guilt trip
        holder.tvPostpone.text = if (task.postponeCount > 0)
            "⏰ Postponed ${task.postponeCount}×" else ""
        holder.tvPostpone.visibility = if (task.postponeCount > 0) View.VISIBLE else View.GONE

        // Priority color
        val color = when (task.priority) {
            TaskPriority.CRITICAL -> 0xFFB71C1C.toInt()
            TaskPriority.HIGH -> 0xFFE65100.toInt()
            TaskPriority.MEDIUM -> 0xFF1565C0.toInt()
            TaskPriority.LOW -> 0xFF388E3C.toInt()
        }
        holder.tvCategory.setTextColor(color)

        holder.btnComplete.setOnClickListener { onComplete(task) }
        holder.btnPostpone.setOnClickListener { onPostpone(task) }
        holder.btnDelete.setOnClickListener { onDelete(task) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(a: Task, b: Task) = a.id == b.id
            override fun areContentsTheSame(a: Task, b: Task) = a == b
        }
    }
}
