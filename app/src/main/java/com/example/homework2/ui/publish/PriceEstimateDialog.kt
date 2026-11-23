package com.example.homework2.ui.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework2.data.remote.ComparableItem
import com.example.homework2.data.remote.EstimateRequest
import com.example.homework2.data.remote.EstimateResult
import com.example.homework2.data.remote.PriceEstimate
import com.example.homework2.data.remote.PriceEstimateClient
import kotlinx.coroutines.launch

private val CONDITION_OPTIONS = listOf("全新", "99新", "95新", "9成新", "8成新")

/**
 * AI 估价弹窗。
 *
 * 用法：在发布页价格输入框下面点「AI 估价」→ 选成色（可选填原价）→ 开始估价
 * → 显示建议价、区间、理由 → 一键把建议价填回价格输入框。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceEstimateDialog(
    title: String,
    description: String,
    category: String,
    type: String,
    groupSize: Int,
    comparables: List<ComparableItem>,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    var condition by remember { mutableStateOf(if (type == "group") "全新" else "9成新") }
    var originalPrice by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var estimate by remember { mutableStateOf<PriceEstimate?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun startEstimate() {
        loading = true
        error = null
        scope.launch {
            val request = EstimateRequest(
                title = title.trim(),
                description = description.trim(),
                condition = condition,
                category = category,
                originalPrice = originalPrice.toDoubleOrNull(),
                groupSize = if (type == "group") groupSize else 1,
                comparables = comparables
            )
            when (val result = PriceEstimateClient.estimate(request)) {
                is EstimateResult.Success -> estimate = result.estimate
                is EstimateResult.Failure -> error = result.message
            }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 智能估价", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val current = estimate
                if (current == null) {
                    Text(
                        text = if (type == "group") "按拼单人数和同类商品行情，给出人均参考价。"
                        else "按成色和同类商品行情，给出一个参考区间。",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("成色", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CONDITION_OPTIONS.forEach { option ->
                            FilterChip(
                                selected = condition == option,
                                onClick = { condition = option },
                                label = { Text(option, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = originalPrice,
                        onValueChange = { input ->
                            originalPrice = input.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("原价（选填，元）") },
                        placeholder = { Text("例：199") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (comparables.isEmpty()) {
                            "本机暂无同类商品，填一下原价会更准"
                        } else {
                            "将参考本机 ${comparables.size} 条同类商品"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    if (loading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI 正在计算…", fontSize = 12.sp, color = Color.Gray)
                    }

                    error?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    EstimateResultView(current)
                }
            }
        },
        confirmButton = {
            val current = estimate
            if (current == null) {
                Button(onClick = { startEstimate() }, enabled = !loading) {
                    Text(if (loading) "计算中…" else "开始估价")
                }
            } else {
                Button(onClick = { onApply(current.suggested) }) {
                    Text("用这个价 ¥${current.suggested}")
                }
            }
        },
        dismissButton = {
            Row {
                if (estimate != null) {
                    TextButton(onClick = {
                        estimate = null
                        error = null
                    }) { Text("重新估") }
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

@Composable
private fun EstimateResultView(estimate: PriceEstimate) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "¥${estimate.suggested}",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        ConfidenceBadge(estimate.confidence)
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "参考区间 ¥${estimate.low} – ¥${estimate.high}",
        fontSize = 13.sp,
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

    estimate.reasons.forEach { reason ->
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text("· ", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Text(reason, fontSize = 13.sp)
        }
    }

    if (estimate.risk.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "⚠ ${estimate.risk}",
            fontSize = 12.sp,
            color = Color(0xFFB26A00)
        )
    }

    if (estimate.source == "fallback") {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前为规则估算（未连上 AI 服务），结果仅供参考",
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun ConfidenceBadge(confidence: String) {
    val (label, color) = when (confidence) {
        "high" -> "置信度 高" to Color(0xFF2E7D32)
        "medium" -> "置信度 中" to Color(0xFFEF6C00)
        else -> "置信度 低" to Color(0xFF757575)
    }
    Text(
        text = label,
        fontSize = 11.sp,
        color = color,
        modifier = Modifier
            .padding(bottom = 4.dp)
    )
}
