package com.example.homework2.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUtils {

    /**
     * 创建相机拍照保存的图片临时 Uri (基于 FileProvider)
     */
    fun createCameraImageUri(context: Context): Uri {
        val storageDir = File(context.cacheDir, "camera_images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(storageDir, "camera_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * 异步压缩图片 (长边不超过 1080px, 质量约 80%) 并复制到应用私有目录，防止原图缺失
     */
    suspend fun compressAndSaveImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. 获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val maxDimension = 1080
            var sampleSize = 1
            val origWidth = options.outWidth
            val origHeight = options.outHeight

            if (origWidth > maxDimension || origHeight > maxDimension) {
                val halfWidth = origWidth / 2
                val halfHeight = origHeight / 2
                while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            // 2. 解码缩小后的 Bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            var bitmap: Bitmap? = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } ?: return@withContext null

            // 3. 读取 EXIF 旋转角度并纠正方向
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val degree = getExifOrientationDegree(inputStream)
                if (degree != 0 && bitmap != null) {
                    val currentBm = bitmap!!
                    val matrix = Matrix().apply { postRotate(degree.toFloat()) }
                    bitmap = Bitmap.createBitmap(currentBm, 0, 0, currentBm.width, currentBm.height, matrix, true)
                }
            }

            // 4. 保存到应用私有目录 files/item_images
            val targetDir = File(context.filesDir, "item_images").apply {
                if (!exists()) mkdirs()
            }
            val outputFile = File(targetDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")

            FileOutputStream(outputFile).use { outStream ->
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, outStream)
            }

            bitmap?.recycle()
            return@withContext outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun getExifOrientationDegree(inputStream: InputStream): Int {
        return try {
            val exif = ExifInterface(inputStream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}
