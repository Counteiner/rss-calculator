package com.rcalc.resourcecalculator.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.rcalc.resourcecalculator.R
import com.rcalc.resourcecalculator.model.ScanResult
import com.rcalc.resourcecalculator.ocr.FormatDetector
import com.rcalc.resourcecalculator.ocr.GridShopParser
import com.rcalc.resourcecalculator.ocr.OcrBlock
import com.rcalc.resourcecalculator.ocr.OcrFormat
import com.rcalc.resourcecalculator.ocr.OcrProcessor
import com.rcalc.resourcecalculator.ocr.OcrResult
import com.rcalc.resourcecalculator.ocr.ResourceTableParser
import com.rcalc.resourcecalculator.ocr.ValueParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val bitmap = withContext(Dispatchers.IO) {
                    loadRotatedBitmap(uri)
                }
                if (bitmap == null) {
                    showError("Gagal membaca gambar")
                    return@launch
                }

                val resized = resizeIfNeeded(bitmap, 2048)
                ivPreview.setImageBitmap(resized)

                val ocrResult = withContext(Dispatchers.IO) {
                    OcrProcessor.extractBlocks(resized)
                }

                val rawText = ocrResult.rawText
                val format = FormatDetector.detect(rawText)

                val result: ScanResult
                val formatLabel: String

                when (format) {
                    OcrFormat.FORMAT_A -> {
                        formatLabel = "A"
                        val rows = ResourceTableParser.parse(rawText)
                        if (rows.isEmpty()) {
                            val preview = rawText.take(300)
                            Toast.makeText(
                                this@ScanActivity,
                                "Format A: tabel tidak terdeteksi.\nTeks mentah:\n$preview",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                            return@launch
                        }
                        val totalFromItems = rows.sumOf { it.fromItems }
                        val totalResources = rows.sumOf { it.total }
                        result = ScanResult(rows, totalFromItems, totalResources)
                    }
                    OcrFormat.FORMAT_B -> {
                        formatLabel = "B"
                        val rows = GridShopParser.parse(
                            blocks = ocrResult.blocks,
                            imageWidth = resized.width,
                            imageHeight = resized.height
                        )
                        if (rows.isEmpty()) {
                            val preview = rawText.take(300)
                            Toast.makeText(
                                this@ScanActivity,
                                "Format B: grid tidak terdeteksi.\nTeks mentah:\n$preview",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                            return@launch
                        }
                        val grandTotal = rows.sumOf { it.fromItems }
                        result = ScanResult(rows, grandTotal, grandTotal)
                    }
                    OcrFormat.UNKNOWN -> {
                        showError("Format gambar tidak dikenali. Gunakan screenshot tabel resource atau grid shop.")
                        return@launch
                    }
                }

                val intent = Intent(this@ScanActivity, ResultActivity::class.java).apply {
                    putExtra("result_format", formatLabel)
                    putExtra("result_raw_json", serializeRows(result.rows))
                    putExtra("result_total_from_items", result.totalFromItems)
                    putExtra("result_total_resources", result.totalResources)
                    putExtra("image_uri", uri.toString())
                    putExtra("raw_ocr_text", rawText)
                }
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                showError("Gagal memproses: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadRotatedBitmap(uri: Uri): Bitmap? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null

        val orientation = try {
            val exif = ExifInterface(inputStream)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        inputStream.close()

        val rawBitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (rotation == 0f) return rawBitmap

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
    }

    private fun resizeIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun serializeRows(rows: List<com.rcalc.resourcecalculator.model.ResourceEntry>): String {
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
