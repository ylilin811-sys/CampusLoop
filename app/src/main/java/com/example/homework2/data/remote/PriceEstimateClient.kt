package com.example.homework2.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 估价客户端。
 *
 * 后端就是项目根目录下的 price-server（Node 服务），接口：
 *   POST /api/price/estimate
 * 返回建议价区间、建议价、理由和风险提示。
 *
 * 网络地址说明：
 *  - Android 模拟器访问「电脑本机」必须用 10.0.2.2，不能用 127.0.0.1
 *  - 真机调试时改成电脑的局域网 IP（例如 http://192.168.1.5:8787），
 *    并用 HOST=0.0.0.0 启动服务
 */
data class ComparableItem(
    val title: String,
    val price: Double,
    val status: String = "listing"
)

data class EstimateRequest(
    val title: String,
    val description: String,
    val condition: String,
    val category: String,
    val originalPrice: Double?,
    val groupSize: Int = 1,
    val comparables: List<ComparableItem> = emptyList()
)

data class PriceEstimate(
    val low: Int,
    val high: Int,
    val suggested: Int,
    val confidence: String,
    val reasons: List<String>,
    val risk: String,
    val source: String
)

sealed class EstimateResult {
    data class Success(val estimate: PriceEstimate) : EstimateResult()
    data class Failure(val code: String, val message: String) : EstimateResult()
}

object PriceEstimateClient {

    // 模拟器 → 电脑本机。真机改成电脑局域网 IP。
    private const val BASE_URL = "http://10.0.2.2:8787"

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 25_000

    suspend fun estimate(request: EstimateRequest): EstimateResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("$BASE_URL/api/price/estimate").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val body = buildBody(request).toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(body) }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
            parseResponse(statusCode, text)
        } catch (e: IOException) {
            EstimateResult.Failure(
                "NO_SERVER",
                "连不上估价服务，请先在电脑上启动 price-server"
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildBody(request: EstimateRequest): JSONObject {
        val body = JSONObject()
        body.put("title", request.title)
        body.put("desc", request.description)
        body.put("condition", request.condition)
        body.put("category", request.category)
        request.originalPrice?.let { body.put("originalPrice", it) }
        if (request.groupSize > 1) body.put("groupSize", request.groupSize)

        val comparables = JSONArray()
        request.comparables.take(10).forEach { item ->
            comparables.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("price", item.price)
                    put("status", item.status)
                }
            )
        }
        body.put("comparables", comparables)
        return body
    }

    private fun parseResponse(statusCode: Int, text: String): EstimateResult {
        val json = runCatching { JSONObject(text) }.getOrNull()
            ?: return EstimateResult.Failure("BAD_RESPONSE", "估价服务返回了无法解析的内容")

        if (!json.optBoolean("ok", false)) {
            return EstimateResult.Failure(
                json.optString("code", "ERROR"),
                json.optString("message", "估价失败，请稍后重试")
            )
        }

        val data = json.optJSONObject("data")
            ?: return EstimateResult.Failure("BAD_RESPONSE", "估价结果为空")

        val suggested = data.optInt("suggested")
        if (suggested <= 0) {
            return EstimateResult.Failure("BAD_RESPONSE", "估价结果无效")
        }

        val reasons = mutableListOf<String>()
        data.optJSONArray("reasons")?.let { array ->
            for (index in 0 until array.length()) {
                reasons.add(array.optString(index))
            }
        }

        return EstimateResult.Success(
            PriceEstimate(
                low = data.optInt("low", suggested),
                high = data.optInt("high", suggested),
                suggested = suggested,
                confidence = data.optString("confidence", "low"),
                reasons = reasons,
                risk = data.optString("risk", ""),
                source = data.optString("source", "model")
            )
        )
    }
}
