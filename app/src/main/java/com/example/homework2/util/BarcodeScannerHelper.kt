package com.example.homework2.util

import android.content.Context
import android.widget.Toast
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

object BarcodeScannerHelper {

    /**
     * 启动 Google Code Scanner (限制 EAN-13 格式，无需自己申请相机权限)
     */
    fun startIsbnScan(
        context: Context,
        onSuccess: (String) -> Unit,
        onCanceled: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13)
                .enableAutoZoom()
                .build()

            val scanner = GmsBarcodeScanning.getClient(context, options)

            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onSuccess(rawValue)
                    } else {
                        onFailure("无法识别条形码内容")
                    }
                }
                .addOnCanceledListener {
                    Toast.makeText(context, "已取消扫码", Toast.LENGTH_SHORT).show()
                    onCanceled()
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.message ?: "扫码失败"
                    Toast.makeText(context, "扫码不可用，请确保已安装 Google Play 服务", Toast.LENGTH_SHORT).show()
                    onFailure(errorMsg)
                }
        } catch (e: Exception) {
            Toast.makeText(context, "启动扫码失败: ${e.message}", Toast.LENGTH_SHORT).show()
            onFailure(e.message ?: "启动失败")
        }
    }
}
