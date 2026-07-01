package com.rcalc.resourcecalculator.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text) { }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }
}
