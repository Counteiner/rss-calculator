package com.rcalc.resourcecalculator.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rcalc.resourcecalculator.R
import com.rcalc.resourcecalculator.model.ResourceEntry
import com.rcalc.resourcecalculator.model.ScanResult
import com.rcalc.resourcecalculator.ocr.OcrProcessor
import com.rcalc.resourcecalculator.ocr.ResourceTableParser
import com.rcalc.resourcecalculator.ocr.ValueParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class ScanActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvLoading: TextView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        ivPreview = findViewById(R.id.ivPreview)
        progressBar = findViewById(R.id.progressBar)
        tvLoading = findViewById(R.id.tvLoading)

        pickImageLauncher.launch("image/*")
    }

    private fun processImage(uri: Uri) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    showError("Gagal membaca gambar")
                    return@launch
                }
                ivPreview.setImageBitmap(bitmap)

                val rawText = withContext(Dispatchers.IO) {
                    OcrProcessor.extractText(bitmap)
                }

                val rows = ResourceTableParser.parse(rawText)

                if (rows.isEmpty()) {
                    Toast.makeText(
                        this@ScanActivity,
                        getString(R.string.error_no_table),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                val totalFromItems = rows.sumOf { it.fromItems }
                val totalResources = rows.sumOf { it.total }
                val result = ScanResult(rows, totalFromItems, totalResources)

                val intent = Intent(this@ScanActivity, ResultActivity::class.java).apply {
                    putExtra("result_raw_json", serializeRows(rows))
                    putExtra("result_total_from_items", totalFromItems)
                    putExtra("result_total_resources", totalResources)
                    putExtra("image_uri", uri.toString())
                }
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                showError("Terjadi kesalahan: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun serializeRows(rows: List<ResourceEntry>): String {
        return rows.joinToString("|") { "${it.name}:${it.fromItems}:${it.total}" }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        tvLoading.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        finish()
    }
}
