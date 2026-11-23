package com.example.homework2.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homework2.R

private data class OnboardingPageData(
    val icon: String,
    val title: String,
    val desc: String,
    val bgColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPager(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPageData(
            icon = "📚 💻 🚲",
            title = "闲置变现",
            desc = "二手图书、数码宝贝、生活用品快速转手\n让闲置学霸物资在校园内高效流动起来",
            bgColor = Color(0xFFFFE8D6)
        ),
        OnboardingPageData(
            icon = "🥗 📦 🚗",
            title = "一起拼更便宜",
            desc = "拼饭外卖分摊、顺风车搭伙出行、大件快递拼单\n校友同路搭伙，省钱又超有爱",
            bgColor = Color(0xFFE3F2FD)
        ),
        OnboardingPageData(
            icon = "🎓 🛡️ 🏫",
            title = "校园内安全交易",
            desc = "实名校友交易，宿舍楼下/食堂门口当面验收\n零邮费、零诈骗、全网最放心",
            bgColor = Color(0xFFE8F5E9)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 主内容区 Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val item = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(item.bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.icon, fontSize = 48.sp)
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 40 })
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = item.title,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = item.desc,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            // 1. 指示器：放在内容区底部居中，胶囊形状
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.LightGray.copy(alpha = 0.6f)
                            )
                    )
                }
            }

            // 2. 底部橙色横幅/按钮 ("立即探索")：与指示器留出至少 16dp 垂直间距，规范无变形
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (pagerState.currentPage == pages.size - 1) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_start_action),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.fillMaxSize())
                }
            }
        }

        // 3. 右上角圆形 ✕ 按钮：Modifier.align(Alignment.TopEnd) + 避开状态栏 WindowInsets
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp, end = 12.dp)
                .size(40.dp) // 点击区域不小于 40dp
                .clickable(onClick = onFinish),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp) // 视觉直径 28dp
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.onboarding_skip),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
