package com.rcalc.resourcecalculator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rcalc.resourcecalculator.R
import com.rcalc.resourcecalculator.db.AppDatabase
import com.rcalc.resourcecalculator.db.ScanResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnDeleteAll: MaterialButton

    private val adapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)

        rvHistory.adapter = adapter

        val dao = AppDatabase.getInstance(this).scanResultDao()

        lifecycleScope.launch {
            dao.getAllFlow().collectLatest { list ->
                adapter.submitList(list)
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        btnDeleteAll.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                dao.deleteAll()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        private var items: List<ScanResultEntity> = emptyList()

        fun submitList(list: List<ScanResultEntity>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            holder.text1.text = "From Items: ${item.fromItemsFormatted}  |  Total: ${item.totalResourcesFormatted}"
            holder.text2.text = dateFormat.format(Date(item.timestamp))
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }
    }
}
