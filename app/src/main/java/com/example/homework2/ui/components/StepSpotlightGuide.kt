package com.example.homework2.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class StepData(
    val title: String,
    val desc: String,
    val alignment: Alignment
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StepSpotlightGuide(
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    val steps = listOf(
        StepData(
            title = "🔍 顶部搜索栏",
            desc = "在这里可以输入关键字，快速搜索二手宝贝或拼单！",
            alignment = Alignment.TopCenter
        ),
        StepData(
            title = "➕ 悬浮发布按钮",
            desc = "点击右下角按钮，随时发布二手闲置或发起拼餐/拼车。",
            alignment = Alignment.BottomEnd
        ),
        StepData(
            title = "👤 个人中心",
            desc = "在'我的'页面管理你的发布、收藏及查看新手引导。",
            alignment = Alignment.BottomCenter
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { /* 阻止下层点击 */ }
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "step_guide"
        ) { stepIndex ->
            val step = steps[stepIndex]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = step.alignment
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(vertical = 100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "步骤 ${stepIndex + 1}/${steps.size}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = step.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = step.desc,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onFinish) {
                                Text("跳过", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    if (currentStep < steps.size - 1) {
                                        currentStep++
                                    } else {
                                        onFinish()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(if (currentStep < steps.size - 1) "下一步" else "我知道了")
                            }
                        }
                    }
                }
            }
        }
    }
}
