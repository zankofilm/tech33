package ir.madreseyar.teacher

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val MAX_UPLOAD_IMAGE_BYTES = 900 * 1024
private const val MAX_RENDER_IMAGE_BYTES = 3 * 1024 * 1024

/** Decodes small inline images with downsampling. Invalid/oversized images never crash a screen. */
internal fun decodeSafeImageDataUrl(value: String): ImageBitmap? = runCatching {
    val comma = value.indexOf(',')
    if (comma < 0 || comma > 64 || value.length - comma > (MAX_RENDER_IMAGE_BYTES * 4 / 3 + 1024)) return@runCatching null
    val header = value.substring(0, comma).lowercase()
    if (header !in setOf("data:image/jpeg;base64", "data:image/png;base64", "data:image/webp;base64")) return@runCatching null
    val bytes = Base64.decode(value.substring(comma + 1), Base64.DEFAULT)
    if (bytes.isEmpty() || bytes.size > MAX_RENDER_IMAGE_BYTES) return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth < 1 || bounds.outHeight < 1) return@runCatching null
    var sample = 1
    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@runCatching null
    bitmap.asImageBitmap()
}.getOrNull()

/** Keeps image uploads within the host JSON API payload budget. */
internal fun encodeQuestionImage(context: Context, uri: Uri): String {
    val type = context.contentResolver.getType(uri)?.lowercase()
    require(type in setOf("image/jpeg", "image/png", "image/webp")) { "فرمت تصویر باید JPG، PNG یا WebP باشد." }
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_UPLOAD_IMAGE_BYTES) throw IOException("حجم تصویر باید کمتر از ۹۰۰ کیلوبایت باشد.")
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: throw IOException("فایل انتخاب‌شده خوانده نشد.")
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "فایل انتخاب‌شده تصویر معتبر نیست." }
    return "data:$type;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
}

@Composable
internal fun QuestionImagePreview(dataUrl: String, modifier: Modifier = Modifier) {
    val bitmap = remember(dataUrl) { decodeSafeImageDataUrl(dataUrl) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "تصویر سؤال",
            modifier = modifier.fillMaxWidth().heightIn(max = 220.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Text("تصویر این سؤال قابل نمایش نیست.", modifier = modifier, color = MaterialTheme.colorScheme.error)
    }
}
