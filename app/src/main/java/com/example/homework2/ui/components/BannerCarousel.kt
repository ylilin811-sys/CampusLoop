package com.example.homework2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework2.data.local.entity.ActivityEntity
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(
    activities: List<ActivityEntity>,
    onActivityClick: (ActivityEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activities.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { activities.size })

    // 每 4 秒自动滚动，按住时自动暂停
    LaunchedEffect(pagerState, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % activities.size
                pagerState.animateScrollToPage(nextPage, animationSpec = tween(600, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) { page ->
            val activity = activities[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = lerp(0.88f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(0.65f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))

            val (cardBrush, cardIcon, badgeLabel) = when (activity.categoryTarget) {
                "group" -> Triple(
                    Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFFA07A))),
                    Icons.Default.Group,
                    "拼团热榜"
                )
                "hot" -> Triple(
                    Brush.linearGradient(listOf(Color(0xFFFF5E3A), Color(0xFFFF9A3D))),
                    Icons.Default.Whatshot,
                    "爆款热选"
                )
                "graduation" -> Triple(
                    Brush.linearGradient(listOf(Color(0xFFFF8A3D), Color(0xFFFFC15E))),
                    Icons.Default.AutoAwesome,
                    "毕业季"
                )
                else -> Triple(
                    Brush.linearGradient(listOf(Color(0xFF2BB5A0), Color(0xFF7ADFC4))),
                    Icons.Default.MenuBook,
                    "教材开学季"
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clickable { onActivityClick(activity) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cardBrush)
                ) {
                    // 背景反向视差装饰光圈与大图标
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = -pageOffset * 18f }
                    ) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = size.height * 0.8f,
                            center = Offset(size.width * 0.85f, size.height * 0.3f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.10f),
                            radius = size.height * 0.5f,
                            center = Offset(size.width * 0.15f, size.height * 0.9f)
                        )
                    }

                    // 右侧大图标背景
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                            .graphicsLayer { translationX = -pageOffset * 15f }
                    ) {
                        Icon(
                            imageVector = cardIcon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.28f),
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    // 左侧渐变遮罩保护文字
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Transparent
                                    ),
                                    startX = 0f,
                                    endX = 400f
                                )
                            )
                    )

                    // 左半区域文字排版 (宽度限制约 58%)，正向视差位移
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(0.58f)
                            .padding(start = 18.dp, end = 8.dp)
                            .graphicsLayer { translationX = pageOffset * 25f }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = badgeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(id = activity.titleRes),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = activity.subtitleRes),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 指示器：当前页圆点平滑拉长为胶囊形
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(activities.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 7.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "indicatorWidth"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(7.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }
}

@Composable
fun PopularTopicChips(
    onTopicClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topics = listOf(
        "# 考研书籍",
        "# 宿舍好物",
        "# 毕业季出清",
        "# 数码极客",
        "# 拼餐合伙人"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(topics) { topic ->
            SuggestionChip(
                onClick = { onTopicClick(topic.removePrefix("# ")) },
                label = { Text(topic, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}
