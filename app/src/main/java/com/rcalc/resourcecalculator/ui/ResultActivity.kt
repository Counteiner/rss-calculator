package com.rcalc.resourcecalculator.ui

import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.rcalc.resourcecalculator.R
import com.rcalc.resourcecalculator.db.AppDatabase
import com.rcalc.resourcecalculator.db.ScanResultEntity
import com.rcalc.resourcecalculator.model.ResourceEntry
import com.rcalc.resourcecalculator.model.ScanResult
import com.rcalc.resourcecalculator.ocr.ValueParser
import com.rcalc.resourcecalculator.rendering.ResultPanelRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ResultActivity : AppCompatActivity() {

    private lateinit var ivResult: ImageView
    private lateinit var tvFood: TextView
    private lateinit var tvWood: TextView
    private lateinit var tvStone: TextView
    private lateinit var tvGold: TextView
    private lateinit var tvTotalFromItems: TextView
    private lateinit var tvTotalResources: TextView
    private lateinit var tvRawOcr: TextView

    private var resultBitmap: Bitmap? = null
    private lateinit var scanResult: ScanResult
    private var resultFormat: String = "A"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        ivResult = findViewById(R.id.ivResult)
        tvFood = findViewById(R.id.tvFood)
        tvWood = findViewById(R.id.tvWood)
        tvStone = findViewById(R.id.tvStone)
        tvGold = findViewById(R.id.tvGold)
        tvTotalFromItems = findViewById(R.id.tvTotalFromItems)
        tvTotalResources = findViewById(R.id.tvTotalResources)
        tvRawOcr = findViewById(R.id.tvRawOcr)

        resultFormat = intent.getStringExtra("result_format") ?: "A"
        val rowsRaw = intent.getStringExtra("result_raw_json") ?: ""
        val totalFromItems = intent.getDoubleExtra("result_total_from_items", 0.0)
        val totalResources = intent.getDoubleExtra("result_total_resources", 0.0)
        val imageUri = intent.getStringExtra("image_uri") ?: ""
        val rawOcr = intent.getStringExtra("raw_ocr_text") ?: ""

        val rows = deserializeRows(rowsRaw)
        scanResult = ScanResult(rows, totalFromItems, totalResources)

        displayResult(scanResult)
        if (rawOcr.isNotBlank()) {
            tvRawOcr.text = "OCR:\n$rawOcr"
            tvRawOcr.setOnLongClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OCR", rawOcr))
                Toast.makeText(this, "Teks OCR disalin", Toast.LENGTH_SHORT).show()
                true
            }
            lifecycleScope.launch(Dispatchers.IO) {
                File(filesDir, "ocr_debug.txt").writeText(rawOcr)
            }
        }
        loadAndProcessImage(imageUri)

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveImage() }
        findViewById<MaterialButton>(R.id.btnShare).setOnClickListener { shareImage() }

        saveToHistory()
    }

    private fun loadAndProcessImage(uri: String) {
        lifecycleScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(Uri.parse(uri))
                val original = BitmapFactory.decodeStream(inputStream)
                if (original == null) return@launch

                resultBitmap = withContext(Dispatchers.IO) {
                    ResultPanelRenderer.appendResultPanel(original, scanResult, resultFormat)
                }
                ivResult.setImageBitmap(resultBitmap)
            } catch (e: Exception) {
                Toast.makeText(this@ResultActivity, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayResult(result: ScanResult) {
        if (resultFormat == "B") {
            for (row in result.rows) {
                val text = "${row.name}: ${ValueParser.formatCompact(row.fromItems)}"
                when (row.name.lowercase()) {
                    "food" -> tvFood.text = text
                    "wood" -> tvWood.text = text
                    "stone" -> tvStone.text = text
                    "gold" -> tvGold.text = text
                }
            }
            tvTotalFromItems.text = "${getString(R.string.result_grand_total)}: ${ValueParser.formatCompact(result.totalFromItems)}"
            tvTotalResources.text = getString(R.string.result_format_b)
        } else {
            for (row in result.rows) {
                val text = "${row.name}: ${ValueParser.formatCompact(row.fromItems)} / ${ValueParser.formatCompact(row.total)}"
                when (row.name.lowercase()) {
                    "food" -> tvFood.text = text
                    "wood" -> tvWood.text = text
                    "stone" -> tvStone.text = text
                    "gold" -> tvGold.text = text
                }
            }
            tvTotalFromItems.text = "${getString(R.string.result_from_items)}: ${ValueParser.formatCompact(result.totalFromItems)}"
            tvTotalResources.text = "${getString(R.string.result_total_resources)}: ${ValueParser.formatCompact(result.totalResources)}"
        }
    }

    private fun saveImage() {
        val bitmap = resultBitmap ?: return
        lifecycleScope.launch {
            try {
                val filename = "RCalc_${System.currentTimeMillis()}.jpg"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RCalc")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                )
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(this@ResultActivity, "Tersimpan di album RCalc", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ResultActivity, "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareImage() {
        val bitmap = resultBitmap ?: return
        lifecycleScope.launch {
            try {
                val cacheDir = cacheDir
                val file = File(cacheDir, "RCalc_share_${System.currentTimeMillis()}.jpg")
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                }
                val uri = FileProvider.getUriForFile(
                    this@ResultActivity,
                    "${packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Bagikan hasil"))
            } catch (e: Exception) {
                Toast.makeText(this@ResultActivity, "Gagal membagikan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveToHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@ResultActivity).scanResultDao()
            dao.insert(
                ScanResultEntity(
                    fromItemsFormatted = ValueParser.formatCompact(scanResult.totalFromItems),
                    totalResourcesFormatted = if (resultFormat == "B") "Grid" else ValueParser.formatCompact(scanResult.totalResources),
                    resultImagePath = null,
                    rawJson = serializeRows(scanResult.rows)
                )
            )
        }
    }

    private fun serializeRows(rows: List<ResourceEntry>): String {
        return rows.joinToString("|") { "${it.name}:${it.fromItems}:${it.total}" }
    }

    private fun deserializeRows(raw: String): List<ResourceEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { part ->
            val s = part.split(":")
            ResourceEntry(name = s[0], fromItems = s[1].toDoubleOrNull() ?: 0.0, total = s[2].toDoubleOrNull() ?: 0.0)
        }
    }
}
