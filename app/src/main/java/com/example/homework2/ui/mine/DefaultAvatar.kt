package com.example.homework2.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun DefaultAvatar(
    nickname: String,
    modifier: Modifier = Modifier,
    size: Dp = 70.dp,
    fontSize: Dp = 28.dp
) {
    val charStr = nickname.trim().take(1).uppercase().ifBlank { "闲" }

    // 根据昵称哈希值取固定色彩
    val palette = listOf(
        Color(0xFFFF8A3D), // 主调橙
        Color(0xFF4A90E2), // 活力蓝
        Color(0xFF07C160), // 清新绿
        Color(0xFF9C27B0), // 紫罗兰
        Color(0xFFFFD166), // 暖黄
        Color(0xFFEF476F)  // 胭脂红
    )
    val colorIndex = abs(nickname.hashCode()) % palette.size
    val bgColor = palette[colorIndex]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = charStr,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.value.sp
        )
    }
}
