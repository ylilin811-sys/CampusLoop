package com.example.homework2.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework2.ui.viewmodel.MarketViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SplashScreen(
    viewModel: MarketViewModel,
    onNavigateNext: () -> Unit
) {
    var isSkipped by remember { mutableStateOf(false) }

    val coverScale = remember { Animatable(0.85f) }
    var showSlogan by remember { mutableStateOf(false) }
    var showChips by remember { mutableStateOf(false) }

    fun finishAndNavigate() {
        if (!isSkipped) {
            isSkipped = true
            onNavigateNext()
        }
    }

    LaunchedEffect(Unit) {
        coverScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        showSlogan = true
        delay(200)

        showChips = true
        delay(1400)

        finishAndNavigate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF8E43),
                        Color(0xFFFF8536),
                        Color(0xFFFFA259)
                    )
                )
            )
            .clickable { finishAndNavigate() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. 核心精美 Vector 插画封面图 (锁包笑脸、放大镜与 4 环 Badge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .scale(coverScale.value),
                contentAlignment = Alignment.Center
            ) {
                // 背景虚线光圈与投影
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = 115.dp.toPx(),
                        center = centerOffset,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    drawOval(
                        color = Color.Black.copy(alpha = 0.12f),
                        topLeft = Offset(centerOffset.x - 45.dp.toPx(), centerOffset.y + 55.dp.toPx()),
                        size = Size(90.dp.toPx(), 16.dp.toPx())
                    )
                }

                // 核心白笑脸包与放大镜
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // 提手
                        drawArc(
                            color = Color.White,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(w * 0.28f, h * 0.05f),
                            size = Size(w * 0.44f, h * 0.44f),
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 提包主体
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.15f, h * 0.25f),
                            size = Size(w * 0.7f, h * 0.7f),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )

                        // 眼睛与微笑弧线
                        drawCircle(color = Color(0xFFFF8536), radius = 4.5.dp.toPx(), center = Offset(w * 0.38f, h * 0.52f))
                        drawCircle(color = Color(0xFFFF8536), radius = 4.5.dp.toPx(), center = Offset(w * 0.62f, h * 0.52f))

                        drawArc(
                            color = Color(0xFFFF8536),
                            startAngle = 20f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(w * 0.33f, h * 0.58f),
                            size = Size(w * 0.34f, h * 0.25f),
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 放大镜
                        drawCircle(color = Color.White, radius = 22.dp.toPx(), center = Offset(w * 0.85f, h * 0.78f))
                        drawCircle(color = Color(0xFFFF8536), radius = 22.dp.toPx(), center = Offset(w * 0.85f, h * 0.78f), style = Stroke(width = 5.dp.toPx()))
                        drawLine(color = Color.White, start = Offset(w * 0.95f, h * 0.88f), end = Offset(w * 1.12f, h * 1.05f), strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
                    }
                }

                // 4 个环形悬浮 Badge
                Box(modifier = Modifier.size(260.dp)) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 10.dp, start = 10.dp).size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TabletMac, contentDescription = null, tint = Color(0xFFFF8536), modifier = Modifier.size(22.dp))
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(24.dp))
                    }
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 10.dp, start = 10.dp).size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFFF8536), modifier = Modifier.size(22.dp))
                    }
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 10.dp, end = 10.dp).size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color(0xFFFF8536), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 2. 品牌大字「闲 圈」
                Text(
                    text = "闲  圈",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Slogan「闲置不闲，拼着更省」及子标题
                AnimatedVisibility(
                    visible = showSlogan,
                    enter = fadeIn(tween(400)) + slideInVertically { 20 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "闲置不闲，拼着更省",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "XIANQUAN  •  CAMPUS MARKETPLACE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. 四个圆角特征胶囊标签 (`二手交易` `校园拼单` `AI 估价` `拍照上传`)
                AnimatedVisibility(
                    visible = showChips,
                    enter = fadeIn(tween(400)) + slideInVertically { 30 }
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(listOf("二手交易", "校园拼单", "AI 估价", "拍照上传")) { tag ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. 底部跳过提示
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "点击任意处跳过 ❯",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        // 右上角倒计时跳过按键
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f))
                .clickable { finishAndNavigate() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("跳过 ❯", color = Color.White, fontSize = 12.sp)
        }
    }
}
