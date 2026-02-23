package com.srikar.lifeflow.ui.budget

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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BudgetFragment : Fragment() {

    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvIncome = view.findViewById<TextView>(R.id.tv_income)
        val tvExpenses = view.findViewById<TextView>(R.id.tv_expenses)
        val tvBalance = view.findViewById<TextView>(R.id.tv_balance)
        val tvMonth = view.findViewById<TextView>(R.id.tv_month)

        adapter = TransactionAdapter { viewModel.deleteTransaction(it) }

        view.findViewById<RecyclerView>(R.id.rv_transactions).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@BudgetFragment.adapter
        }

        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            tvMonth.text = LocalDate.parse("$month-01")
                .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }

        viewModel.transactions.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.monthlyIncome.observe(viewLifecycleOwner) { tvIncome.text = "Income: €%.2f".format(it) }
        viewModel.monthlyExpenses.observe(viewLifecycleOwner) { tvExpenses.text = "Spent: €%.2f".format(it) }
        viewModel.balance.observe(viewLifecycleOwner) {
            tvBalance.text = "Balance: €%.2f".format(it)
            tvBalance.setTextColor(if (it >= 0) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())
        }

        // Month navigation
        view.findViewById<View>(R.id.btn_prev_month).setOnClickListener {
            val current = LocalDate.parse("${viewModel.currentMonth.value}-01")
            viewModel.setMonth(current.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM")))
        }
        view.findViewById<View>(R.id.btn_next_month).setOnClickListener {
            val current = LocalDate.parse("${viewModel.currentMonth.value}-01")
            viewModel.setMonth(current.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM")))
        }

        view.findViewById<FloatingActionButton>(R.id.fab_add_transaction).setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_transaction_title)
        val etAmount = dialogView.findViewById<EditText>(R.id.et_amount)
        val radioIncome = dialogView.findViewById<RadioButton>(R.id.rb_income)
        val radioExpense = dialogView.findViewById<RadioButton>(R.id.rb_expense)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_expense_category)

        ArrayAdapter.createFromResource(requireContext(), R.array.expense_categories,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = it }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                val amount = etAmount.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                val type = if (radioIncome.isChecked) TransactionType.INCOME else TransactionType.EXPENSE
                val category = ExpenseCategory.values()[spinnerCategory.selectedItemPosition]
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

                viewModel.addTransaction(Transaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    date = today
                ))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

// ─── ADAPTER ──────────────────────────────────────────────────────────────────

class TransactionAdapter(
    private val onDelete: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TxViewHolder>(DIFF) {

    inner class TxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_tx_title)
        val tvAmount: TextView = view.findViewById(R.id.tv_tx_amount)
        val tvCategory: TextView = view.findViewById(R.id.tv_tx_category)
        val tvDate: TextView = view.findViewById(R.id.tv_tx_date)
        val btnDelete: View = view.findViewById(R.id.btn_tx_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TxViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(holder: TxViewHolder, position: Int) {
        val tx = getItem(position)
        holder.tvTitle.text = tx.title
        holder.tvCategory.text = tx.category.name.lowercase().replaceFirstChar { it.uppercase() }
        holder.tvDate.text = tx.date

        val isIncome = tx.type == TransactionType.INCOME
        holder.tvAmount.text = "${if (isIncome) "+" else "-"}€%.2f".format(tx.amount)
        holder.tvAmount.setTextColor(if (isIncome) 0xFF2E7D32.toInt() else 0xFFB71C1C.toInt())

        holder.btnDelete.setOnClickListener { onDelete(tx) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
            override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
        }
    }
}
