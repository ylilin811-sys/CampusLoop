package com.example.homework2.ui.detail

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.homework2.data.local.entity.CategoryDetail
import com.example.homework2.data.local.entity.GoodsEntity
import com.example.homework2.ui.components.ConfettiCanvas
import com.example.homework2.ui.mine.DefaultAvatar
import com.example.homework2.ui.theme.TagGroupBg
import com.example.homework2.ui.theme.TagGroupText
import com.example.homework2.ui.theme.TagSecondhandBg
import com.example.homework2.ui.theme.TagSecondhandText
import com.example.homework2.ui.viewmodel.MarketViewModel
import com.example.homework2.util.CategoryDetailParser
import com.example.homework2.util.ContactUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun DetailScreen(
    viewModel: MarketViewModel,
    goodsId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var goods by remember { mutableStateOf<GoodsEntity?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsState()
    val isContactSecurityAlertRead by viewModel.isContactSecurityAlertRead.collectAsState()

    var isContactUnlocked by remember { mutableStateOf(false) }
    var showSecurityAlertDialog by remember { mutableStateOf(false) }

    var isWechatCopied by remember { mutableStateOf(false) }
    var isQqCopied by remember { mutableStateOf(false) }

    val membersFlow = remember(goodsId) { viewModel.getGroupMembers(goodsId) }
    val members by membersFlow.collectAsState(initial = emptyList())

    LaunchedEffect(goodsId) {
        goods = viewModel.getGoodsById(goodsId)
    }

    val item = goods ?: return

    val isSelfItem = remember(currentUser, item) {
        currentUser != null && (currentUser?.nickname == item.sellerName || item.sellerName == "我")
    }

    val contactInfo = remember(item.contactOverrideJson) {
        ContactUtils.fromJson(item.contactOverrideJson)
    }

    val imageList = remember(item.images) {
        if (item.images.isNotEmpty()) item.images else listOf("https://picsum.photos/600/600?random=${item.id}")
    }
    val pagerState = rememberPagerState(pageCount = { imageList.size })

    val categoryDetail = remember(item.extraInfoJson) {
        CategoryDetailParser.fromJson(item.extraInfoJson)
    }

    val isSold = item.status == "SOLD"
    val isReserved = item.status == "RESERVED"

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("物品详情") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("价格", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                "¥${item.price}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSold) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (item.type == "secondhand") {
                            val buttonText = when {
                                isSelfItem -> "这是您发布的商品"
                                isSold -> "商品已被买走 (已售出)"
                                isReserved -> "已被其他学友预定"
                                else -> "我想要 (发送求购申请)"
                            }

                            Button(
                                onClick = {
                                    if (isSelfItem || isSold || isReserved) return@Button
                                    viewModel.placeOrder(context, item)
                                    Toast.makeText(context, "已向卖家【${item.sellerName}】发送求购申请！", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isSelfItem && !isSold && !isReserved,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSold || isReserved) Color.Gray else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(buttonText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val isFull = item.currentCount >= item.maxCount || isSold
                            val groupBtnText = when {
                                isSold -> "拼单已结束"
                                isFull -> "该拼单已满员"
                                else -> "加入拼单分摊"
                            }

                            Button(
                                onClick = {
                                    if (isFull) return@Button
                                    viewModel.joinGroupOrderWithUseCase(context, item.id)
                                    goods = goods?.copy(currentCount = (goods?.currentCount ?: 1) + 1)
                                    showConfetti = true
                                    Toast.makeText(context, "成功加入拼单！", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isFull,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFull) Color.Gray else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(groupBtnText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 多图轮播 Swiper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(imageList[page])
                                    .crossfade(true)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_report_image)
                                    .build()
                            ),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (imageList.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${imageList.size}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    val isGroup = item.type == "group"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isGroup) TagGroupBg else TagSecondhandBg)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isGroup) "拼单进行中" else "二手闲置",
                            fontWeight = FontWeight.Bold,
                            color = if (isGroup) TagGroupText else TagSecondhandText
                        )
                    }
                }

                // 2. 拼单专属进度条 & 动态数字滚动
                if (item.type == "group") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔥 拼单动态进度", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("截止时间: ${item.deadline}", fontSize = 12.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val progressTarget = item.currentCount.toFloat() / item.maxCount.toFloat()
                            val animatedProgress by animateFloatAsState(
                                targetValue = progressTarget,
                                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                label = "progress"
                            )

                            LinearProgressIndicator(
                                progress = animatedProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("已拼 ")
                                AnimatedContent(
                                    targetState = item.currentCount,
                                    transitionSpec = {
                                        slideInVertically { height -> height } + fadeIn() togetherWith
                                                slideOutVertically { height -> -height } + fadeOut()
                                    },
                                    label = "headcountRoll"
                                ) { count ->
                                    Text(
                                        "$count",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(" 人 / 人数上限 ${item.maxCount} 人")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                members.forEach { member ->
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(member.memberAvatar),
                                            contentDescription = member.memberName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 核心价格与标题
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            var isFavState by remember(item.isFavorite) { mutableStateOf(item.isFavorite) }
                            val heartScale by animateFloatAsState(
                                targetValue = if (isFavState) 1.3f else 1.0f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "heartScale"
                            )

                            IconButton(onClick = {
                                isFavState = !isFavState
                                viewModel.toggleFavorite(item)
                            }) {
                                Icon(
                                    imageVector = if (isFavState) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (isFavState) Color.Red else Color.Gray,
                                    modifier = Modifier.scale(heartScale)
                                )
                            }
                        }

                        if (categoryDetail != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                when (categoryDetail) {
                                    is CategoryDetail.BookDetail -> {
                                        if (categoryDetail.isLatestEdition) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text("🔥 最新版", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                                            )
                                        }
                                        SuggestionChip(onClick = {}, label = { Text("📖 ${categoryDetail.edition}", fontSize = 11.sp) })
                                        SuggestionChip(onClick = {}, label = { Text("📝 ${categoryDetail.notes}", fontSize = 11.sp) })
                                        SuggestionChip(onClick = {}, label = { Text("✨ ${categoryDetail.damaged}", fontSize = 11.sp) })
                                    }
                                    is CategoryDetail.DigitalDetail -> {
                                        SuggestionChip(onClick = {}, label = { Text("📱 ${categoryDetail.model}", fontSize = 11.sp) })
                                        SuggestionChip(onClick = {}, label = { Text("🛡️ ${categoryDetail.warranty}", fontSize = 11.sp) })
                                        if (categoryDetail.batteryHealth.isNotBlank()) {
                                            SuggestionChip(onClick = {}, label = { Text("🔋 健康度${categoryDetail.batteryHealth}", fontSize = 11.sp) })
                                        }
                                    }
                                    is CategoryDetail.DailyDetail -> {
                                        if (categoryDetail.usageDuration.isNotBlank()) {
                                            SuggestionChip(onClick = {}, label = { Text("🕒 使用${categoryDetail.usageDuration}", fontSize = 11.sp) })
                                        }
                                        SuggestionChip(onClick = {}, label = { Text("✨ ${categoryDetail.flaw}", fontSize = 11.sp) })
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("📍 交易/集合地点: ${item.location}", fontSize = 14.sp, color = Color.Gray)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Text("详细说明", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.description, fontSize = 15.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }

                // 4. 卖家卡片 & 联系方式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.sellerAvatar.isNotBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(item.sellerAvatar),
                                    contentDescription = item.sellerName,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                DefaultAvatar(nickname = item.sellerName, size = 50.dp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.sellerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("🎓 已完成校友实名认证", fontSize = 12.sp, color = Color(0xFF07C160))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        if (isSelfItem) {
                            Text("这是您自己发布的商品", fontSize = 13.sp, color = Color.Gray)
                        } else if (!contactInfo.isPublic) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("卖家暂未公开联系方式，建议通过站内私信沟通", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { Toast.makeText(context, "已向卖家发送私信", Toast.LENGTH_SHORT).show() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("站内私信", fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            if (!isContactUnlocked) {
                                Button(
                                    onClick = {
                                        if (!isContactSecurityAlertRead) {
                                            showSecurityAlertDialog = true
                                        } else {
                                            isContactUnlocked = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = "解锁", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🔓 点击解锁卖家联系方式", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💬 卖家联系方式", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                                    if (contactInfo.wechatId.isNotBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text("微信: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                AnimatedContent(
                                                    targetState = contactInfo.wechatId,
                                                    transitionSpec = {
                                                        fadeIn(animationSpec = tween(400)) + slideInVertically { 20 } togetherWith
                                                                fadeOut(animationSpec = tween(200))
                                                    },
                                                    label = "wechatUnmask"
                                                ) { wechat ->
                                                    Text(wechat, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        ContactUtils.copyToClipboard(context, "微信号", contactInfo.wechatId)
                                                        scope.launch {
                                                            isWechatCopied = true
                                                            delay(1000)
                                                            isWechatCopied = false
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isWechatCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                        contentDescription = "复制",
                                                        tint = if (isWechatCopied) Color(0xFF07C160) else Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                AssistChip(
                                                    onClick = { ContactUtils.launchWechat(context, contactInfo.wechatId) },
                                                    label = { Text("打开微信", fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }

                                    if (contactInfo.qqNumber.isNotBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text("QQ: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                AnimatedContent(
                                                    targetState = contactInfo.qqNumber,
                                                    transitionSpec = {
                                                        fadeIn(animationSpec = tween(400)) + slideInVertically { 20 } togetherWith
                                                                fadeOut(animationSpec = tween(200))
                                                    },
                                                    label = "qqUnmask"
                                                ) { qq ->
                                                    Text(qq, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        ContactUtils.copyToClipboard(context, "QQ号", contactInfo.qqNumber)
                                                        scope.launch {
                                                            isQqCopied = true
                                                            delay(1000)
                                                            isQqCopied = false
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isQqCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                        contentDescription = "复制",
                                                        tint = if (isQqCopied) Color(0xFF07C160) else Color.Gray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                AssistChip(
                                                    onClick = { ContactUtils.launchQq(context, contactInfo.qqNumber) },
                                                    label = { Text("打开 QQ", fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. 分类专属“参数表”卡片
                if (categoryDetail != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📋 物品详细参数表", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))

                            when (categoryDetail) {
                                is CategoryDetail.BookDetail -> {
                                    SpecRow("书名", categoryDetail.bookName)
                                    SpecRow("作者", categoryDetail.author)
                                    SpecRow("出版社", categoryDetail.publisher)
                                    SpecRow("版次", "${categoryDetail.edition}${if (categoryDetail.isLatestEdition) " (最新版)" else ""}")
                                    if (categoryDetail.isbn.isNotBlank()) SpecRow("ISBN", categoryDetail.isbn)
                                    if (categoryDetail.course.isNotBlank()) SpecRow("适用课程", categoryDetail.course)
                                    SpecRow("笔记情况", categoryDetail.notes)
                                    SpecRow("破损/缺页", categoryDetail.damaged)
                                }
                                is CategoryDetail.DigitalDetail -> {
                                    SpecRow("品牌/型号", "${categoryDetail.brand} ${categoryDetail.model}")
                                    SpecRow("购买时间", categoryDetail.purchaseDate)
                                    if (categoryDetail.specs.isNotBlank()) SpecRow("配置/容量", categoryDetail.specs)
                                    SpecRow("保修情况", categoryDetail.warranty)
                                    if (categoryDetail.batteryHealth.isNotBlank()) SpecRow("电池健康度", categoryDetail.batteryHealth)
                                    if (categoryDetail.accessories.isNotEmpty()) SpecRow("包含配件", categoryDetail.accessories.joinToString(" / "))
                                    SpecRow("拆修记录", categoryDetail.repaired)
                                }
                                is CategoryDetail.DailyDetail -> {
                                    if (categoryDetail.brand.isNotBlank()) SpecRow("品牌", categoryDetail.brand)
                                    if (categoryDetail.usageDuration.isNotBlank()) SpecRow("使用时长", categoryDetail.usageDuration)
                                    SpecRow("瑕疵情况", categoryDetail.flaw)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 一次性安全交易提醒对话框
        if (showSecurityAlertDialog) {
            AlertDialog(
                onDismissRequest = { showSecurityAlertDialog = false },
                title = { Text("🛡️ 校内安全交易提醒") },
                text = {
                    Text(
                        "为保障您的财产安全：\n1. 建议校内公共场所当面交易、验货后再付款。\n2. 谨防提前转账、兼职刷单及不明网址链接。\n3. 如遇诈骗请即时保留证据并联系校园保卫处。",
                        lineHeight = 20.sp,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showSecurityAlertDialog = false
                        viewModel.markContactSecurityAlertRead()
                        isContactUnlocked = true
                    }) {
                        Text("我知道了，查看联系方式")
                    }
                }
            )
        }

        if (showConfetti) {
            ConfettiCanvas()
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
