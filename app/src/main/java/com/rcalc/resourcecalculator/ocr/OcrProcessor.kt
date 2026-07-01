package com.rcalc.resourcecalculator.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine

data class OcrBlock(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = bottom - top
}

data class OcrResult(
    val rawText: String,
    val blocks: List<OcrBlock>
)

object OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text) { }
                }
                .addOnFailureListener { e ->
                    Log.e("OcrProcessor", "OCR gagal", e)
                    cont.resume("") { }
                }
        }
    }

    suspend fun extractBlocks(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks.flatMap { block ->
                        block.lines.map { line ->
                            val box = line.boundingBox ?: Rect(0, 0, 0, 0)
                            OcrBlock(
                                text = line.text.trim(),
                                left = box.left,
                                top = box.top,
                                right = box.right,
                                bottom = box.bottom
                            )
                        }
                    }
                    cont.resume(OcrResult(rawText = visionText.text, blocks = blocks)) { }
                }
                .addOnFailureListener { e ->
                    Log.e("OcrProcessor", "OCR gagal", e)
                    cont.resume(OcrResult(rawText = "", blocks = emptyList())) { }
                }
        }
    }
}
