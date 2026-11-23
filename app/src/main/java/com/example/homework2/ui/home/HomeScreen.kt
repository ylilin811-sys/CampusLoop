package com.example.homework2.ui.home

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.homework2.data.local.entity.CategoryDetail
import com.example.homework2.data.local.entity.GoodsEntity
import com.example.homework2.ui.components.BannerCarousel
import com.example.homework2.ui.components.EmptyStateView
import com.example.homework2.ui.components.ErrorStateView
import com.example.homework2.ui.components.OnboardingPager
import com.example.homework2.ui.components.PopularTopicChips
import com.example.homework2.ui.components.SkeletonItem
import com.example.homework2.ui.components.SoldStamp
import com.example.homework2.ui.components.StepSpotlightGuide
import com.example.homework2.ui.state.EmptyScene
import com.example.homework2.ui.state.ErrorScene
import com.example.homework2.ui.state.UiState
import com.example.homework2.ui.theme.TagGroupBg
import com.example.homework2.ui.theme.TagGroupText
import com.example.homework2.ui.theme.TagSecondhandBg
import com.example.homework2.ui.theme.TagSecondhandText
import com.example.homework2.ui.viewmodel.MarketViewModel
import com.example.homework2.util.CategoryDetailParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MarketViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToPublish: (String) -> Unit
) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val isStepGuideCompleted by viewModel.isStepGuideCompleted.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val activities by viewModel.activities.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showSoldItems by viewModel.showSoldItems.collectAsState()

    var isFabExpanded by remember { mutableStateOf(false) }

    SideEffect {
        Log.d("HomeScreen", "activities count: ${activities.size}")
    }

    if (!isOnboardingCompleted) {
        OnboardingPager(onFinish = { viewModel.completeOnboarding() })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. 顶部搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("搜二手书、数码、拼餐、拼车...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            // 常态无搜索关键词时展示横向活动/热点轮播和热门话题
            if (searchQuery.isBlank()) {
                BannerCarousel(
                    activities = activities,
                    onActivityClick = { activity ->
                        if (activity.categoryTarget.isNotBlank()) {
                            viewModel.selectCategory(activity.categoryTarget)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                PopularTopicChips(
                    onTopicClick = { topic -> viewModel.search(topic) }
                )

                Spacer(modifier = Modifier.height(6.dp))
            }

            // 2. 分类标签横向滑动与“含已售”开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val categories = listOf(
                    "all" to "🔥 全部",
                    "books" to "📚 二手书",
                    "digital" to "💻 数码",
                    "daily" to "🛋️ 生活",
                    "group" to "🤝 拼单"
                )

                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (id, name) ->
                        val isSelected = selectedCategory == id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(id) },
                            label = { Text(name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("含已售", fontSize = 11.sp, color = Color.Gray)
                    Switch(
                        checked = showSoldItems,
                        onCheckedChange = { viewModel.toggleShowSoldItems() },
                        modifier = Modifier.scale(0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 列表及状态区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is UiState.Loading -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(6) { SkeletonItem() }
                        }
                    }

                    is UiState.Empty -> {
                        when (state.scene) {
                            EmptyScene.SEARCH -> {
                                EmptyStateView(
                                    scene = EmptyScene.SEARCH,
                                    onActionClick = { viewModel.search("") }
                                )
                            }
                            else -> {
                                EmptyStateView(
                                    scene = EmptyScene.HOME,
                                    onActionClick = { onNavigateToPublish("secondhand") }
                                )
                            }
                        }
                    }

                    is UiState.Error -> {
                        ErrorStateView(
                            scene = ErrorScene.GENERIC,
                            onRetry = { viewModel.search(searchQuery) }
                        )
                    }

                    is UiState.Success -> {
                        val goodsList = state.data

                        if (selectedCategory == "books" && goodsList.isNotEmpty()) {
                            val bookGroups = remember(goodsList) {
                                goodsList.filter { it.category == "books" }
                                    .groupBy { goods ->
                                        val detail = CategoryDetailParser.fromJson(goods.extraInfoJson) as? CategoryDetail.BookDetail
                                        detail?.bookName?.ifBlank { goods.title } ?: goods.title
                                    }
                            }

                            LazyColumn(
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(bookGroups.entries.toList(), key = { it.key }) { entry ->
                                    BookGroupCard(
                                        bookTitleGroup = entry.key,
                                        goodsItems = entry.value,
                                        onNavigateToDetail = onNavigateToDetail
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(goodsList, key = { _, item -> item.id }) { _, item ->
                                    GoodsCard(
                                        goods = item,
                                        onClick = { onNavigateToDetail(item.id) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 悬浮发布按钮
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(visible = isFabExpanded) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            onNavigateToPublish("group")
                        },
                        icon = { Text("🤝") },
                        text = { Text("发起拼单") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            onNavigateToPublish("secondhand")
                        },
                        icon = { Text("🏷️") },
                        text = { Text("发布二手") },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                modifier = Modifier.scale(if (isFabExpanded) 1f else pulseScale),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "发布"
                )
            }
        }

        if (!isStepGuideCompleted) {
            StepSpotlightGuide(onFinish = { viewModel.completeStepGuide() })
        }
    }
}

@Composable
fun BookGroupCard(
    bookTitleGroup: String,
    goodsItems: List<GoodsEntity>,
    onNavigateToDetail: (Long) -> Unit
) {
    var isExpanded by remember { mutableStateOf(goodsItems.size > 1) }

    val minPrice = remember(goodsItems) { goodsItems.minOf { it.price } }
    val maxPrice = remember(goodsItems) { goodsItems.maxOf { it.price } }
    val firstItem = goodsItems.first()
    val detail = remember(firstItem) { CategoryDetailParser.fromJson(firstItem.extraInfoJson) as? CategoryDetail.BookDetail }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookTitleGroup,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (detail != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "作者: ${detail.author} | ${detail.publisher}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (goodsItems.size > 1) "共 ${goodsItems.size} 个版次在售" else "单版次在售",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (minPrice == maxPrice) "¥$minPrice" else "¥$minPrice – ¥$maxPrice",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "收起版次对比 ▴" else "展开各版次对比 ▾", fontSize = 13.sp)
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    goodsItems.forEach { goods ->
                        val bookDetail = CategoryDetailParser.fromJson(goods.extraInfoJson) as? CategoryDetail.BookDetail
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .clickable { onNavigateToDetail(goods.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(goods.primaryImage.ifBlank { "https://picsum.photos/300/300?random=${goods.id}" }),
                                contentDescription = goods.title,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = bookDetail?.edition ?: "基础版次",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (bookDetail?.isLatestEdition == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🔥最新版", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "成色: ${bookDetail?.notes ?: "正常"} | 卖家: ${goods.sellerName}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Text(
                                text = "¥${goods.price}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            Icon(Icons.Default.ChevronRight, contentDescription = "详情", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoodsCard(
    goods: GoodsEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    var isFavState by remember(goods.isFavorite) { mutableStateOf(goods.isFavorite) }
    val heartScale by animateFloatAsState(
        targetValue = if (isFavState) 1.3f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )

    val context = LocalContext.current
    val imagePath = goods.primaryImage.ifBlank { "https://picsum.photos/300/300?random=${goods.id}" }
    val isSold = goods.status == "SOLD"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSold) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSold) 1.dp else 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(imagePath)
                            .crossfade(true)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_report_image)
                            .build()
                    ),
                    contentDescription = goods.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                val statusText = when (goods.status) {
                    "RESERVED" -> "已预定"
                    "SOLD" -> "已售出"
                    else -> if (goods.type == "group") "拼单" else "二手"
                }

                val statusBg = when (goods.status) {
                    "RESERVED" -> Color(0xFFFFB300)
                    "SOLD" -> Color(0xFF757575)
                    else -> if (goods.type == "group") TagGroupBg else TagSecondhandBg
                }

                val statusTextColor = when (goods.status) {
                    "RESERVED", "SOLD" -> Color.White
                    else -> if (goods.type == "group") TagGroupText else TagSecondhandText
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }

                if (isSold) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoldStamp(text = "已售出")
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = goods.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    color = if (isSold) Color.Gray else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (goods.type == "group") {
                    val progress = goods.currentCount.toFloat() / goods.maxCount.toFloat()
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已拼 ${goods.currentCount}/${goods.maxCount}人",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¥${goods.price}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSold) Color.Gray else MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            isFavState = !isFavState
                            onFavoriteToggle()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavState) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "收藏",
                            tint = if (isFavState) Color.Red else Color.Gray,
                            modifier = Modifier.scale(heartScale)
                        )
                    }
                }
            }
        }
    }
}
