package com.example.homework2.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework2.R
import com.example.homework2.ui.state.EmptyScene
import kotlinx.coroutines.delay

@Composable
fun EmptyStateView(
    scene: EmptyScene,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. 无限漂浮动效
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // 2. 阶梯淡入 100ms 间隔动画 State
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(scene) {
        showTitle = false
        showSubtitle = false
        showButton = false
        delay(100)
        showTitle = true
        delay(100)
        showSubtitle = true
        delay(100)
        showButton = true
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // 漂浮插画
            Image(
                painter = painterResource(id = R.drawable.ic_empty_market),
                contentDescription = stringResource(id = scene.titleRes),
                modifier = Modifier
                    .size(130.dp)
                    .offset(y = floatOffset.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 标题淡入
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(tween(300)) + slideInVertically { 20 }
            ) {
                Text(
                    text = stringResource(id = scene.titleRes),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题淡入
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(300)) + slideInVertically { 20 }
            ) {
                Text(
                    text = stringResource(id = scene.subtitleRes),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮淡入
            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(tween(300)) + slideInVertically { 20 }
            ) {
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(id = scene.actionRes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
