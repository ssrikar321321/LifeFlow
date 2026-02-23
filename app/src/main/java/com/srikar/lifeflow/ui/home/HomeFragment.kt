package com.srikar.lifeflow.ui.home

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.srikar.lifeflow.R
import com.srikar.lifeflow.data.model.*

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var choreAdapter: ChoreAdapter
    private lateinit var groceryAdapter: GroceryAdapter
    private lateinit var rvChores: RecyclerView
    private lateinit var rvGrocery: RecyclerView
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tab_layout_home)
        rvChores = view.findViewById(R.id.rv_chores)
        rvGrocery = view.findViewById(R.id.rv_grocery)

        setupChores(view)
        setupGrocery(view)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { rvChores.visibility = View.VISIBLE; rvGrocery.visibility = View.GONE }
                    1 -> { rvChores.visibility = View.GONE; rvGrocery.visibility = View.VISIBLE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        view.findViewById<FloatingActionButton>(R.id.fab_home).setOnClickListener {
            when (tabLayout.selectedTabPosition) {
                0 -> showAddChoreDialog()
                1 -> showAddGroceryDialog()
            }
        }
    }

    private fun setupChores(view: View) {
        choreAdapter = ChoreAdapter(
            onDone = { viewModel.markChoreDone(it) },
            onDelete = { viewModel.deleteChore(it) }
        )
        rvChores.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = choreAdapter
        }
        viewModel.allChores.observe(viewLifecycleOwner) { choreAdapter.submitList(it) }
    }

    private fun setupGrocery(view: View) {
        groceryAdapter = GroceryAdapter(
            onToggle = { item -> viewModel.togglePurchased(item.id, !item.isPurchased) },
            onDelete = { viewModel.deleteGroceryItem(it) }
        )
        rvGrocery.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groceryAdapter
            visibility = View.GONE
        }
        viewModel.groceryItems.observe(viewLifecycleOwner) { groceryAdapter.submitList(it) }
    }

    private fun showAddChoreDialog() {
        val quickChores = listOf(
            "🍳 Wash dishes" to ChoreRoom.KITCHEN,
            "🧹 Sweep floor" to ChoreRoom.GENERAL,
            "🚿 Clean washroom" to ChoreRoom.WASHROOM,
            "🧺 Do laundry" to ChoreRoom.GENERAL,
            "🛒 Go grocery shopping" to ChoreRoom.GENERAL,
            "🗑️ Take out trash" to ChoreRoom.GENERAL,
            "Custom chore..." to null
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Add Chore")
            .setItems(quickChores.map { it.first }.toTypedArray()) { _, which ->
                val (name, room) = quickChores[which]
                if (room == null) {
                    showCustomChoreDialog()
                } else {
                    viewModel.addChore(Chore(
                        name = name,
                        room = room,
                        frequency = ChoreFrequency.WEEKLY
                    ))
                }
            }
            .show()
    }

    private fun showCustomChoreDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_chore, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_chore_name)
        val spinnerRoom = dialogView.findViewById<Spinner>(R.id.spinner_room)
        val spinnerFreq = dialogView.findViewById<Spinner>(R.id.spinner_frequency)

        ArrayAdapter.createFromResource(requireContext(), R.array.chore_rooms,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRoom.adapter = it }

        ArrayAdapter.createFromResource(requireContext(), R.array.chore_frequencies,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFreq.adapter = it }

        AlertDialog.Builder(requireContext())
            .setTitle("Custom Chore")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addChore(Chore(
                        name = name,
                        room = ChoreRoom.values()[spinnerRoom.selectedItemPosition],
                        frequency = ChoreFrequency.values()[spinnerFreq.selectedItemPosition]
                    ))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddGroceryDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_grocery, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_item_name)
        val etQty = dialogView.findViewById<EditText>(R.id.et_quantity)
        val spinnerCat = dialogView.findViewById<Spinner>(R.id.spinner_grocery_category)

        ArrayAdapter.createFromResource(requireContext(), R.array.grocery_categories,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCat.adapter = it }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Grocery Item")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addGroceryItem(GroceryItem(
                        name = name,
                        quantity = etQty.text.toString().trim(),
                        category = spinnerCat.selectedItem.toString()
                    ))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ─── CHORE ADAPTER ────────────────────────────────────────────────────────────

class ChoreAdapter(
    private val onDone: (Chore) -> Unit,
    private val onDelete: (Chore) -> Unit
) : ListAdapter<Chore, ChoreAdapter.ChoreViewHolder>(DIFF) {

    inner class ChoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_chore_name)
        val tvRoom: TextView = view.findViewById(R.id.tv_room)
        val tvNextDue: TextView = view.findViewById(R.id.tv_next_due)
        val btnDone: View = view.findViewById(R.id.btn_chore_done)
        val btnDelete: View = view.findViewById(R.id.btn_chore_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChoreViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_chore, parent, false)
    )

    override fun onBindViewHolder(holder: ChoreViewHolder, position: Int) {
        val chore = getItem(position)
        holder.tvName.text = chore.name
        holder.tvRoom.text = chore.room.name.replace("_", " ")
        holder.tvNextDue.text = chore.nextDueDate?.let { "Due: $it" } ?: "Not scheduled yet"

        // Highlight overdue
        val today = java.time.LocalDate.now().toString()
        if (chore.nextDueDate != null && chore.nextDueDate < today) {
            holder.tvNextDue.setTextColor(0xFFB71C1C.toInt())
            holder.tvNextDue.text = "⚠️ OVERDUE since ${chore.nextDueDate}"
        }

        holder.btnDone.setOnClickListener { onDone(chore) }
        holder.btnDelete.setOnClickListener { onDelete(chore) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Chore>() {
            override fun areItemsTheSame(a: Chore, b: Chore) = a.id == b.id
            override fun areContentsTheSame(a: Chore, b: Chore) = a == b
        }
    }
}

// ─── GROCERY ADAPTER ──────────────────────────────────────────────────────────

class GroceryAdapter(
    private val onToggle: (GroceryItem) -> Unit,
    private val onDelete: (GroceryItem) -> Unit
) : ListAdapter<GroceryItem, GroceryAdapter.GroceryViewHolder>(DIFF) {

    inner class GroceryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_item_name)
        val tvQty: TextView = view.findViewById(R.id.tv_quantity)
        val tvCat: TextView = view.findViewById(R.id.tv_grocery_category)
        val checkBox: CheckBox = view.findViewById(R.id.cb_purchased)
        val btnDelete: View = view.findViewById(R.id.btn_grocery_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = GroceryViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_grocery, parent, false)
    )

    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvName.text = item.name
        holder.tvQty.text = item.quantity
        holder.tvCat.text = item.category
        holder.checkBox.isChecked = item.isPurchased
        holder.tvName.alpha = if (item.isPurchased) 0.4f else 1.0f

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.setOnCheckedChangeListener { _, _ -> onToggle(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GroceryItem>() {
            override fun areItemsTheSame(a: GroceryItem, b: GroceryItem) = a.id == b.id
            override fun areContentsTheSame(a: GroceryItem, b: GroceryItem) = a == b
        }
    }
}
