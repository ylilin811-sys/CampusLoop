package com.example.homework2.ui.publish

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.homework2.data.local.entity.CategoryDetail
import com.example.homework2.ui.viewmodel.IsbnLookupState
import com.example.homework2.ui.viewmodel.MarketViewModel
import com.example.homework2.util.AiEstimateEngine
import com.example.homework2.util.BarcodeScannerHelper
import com.example.homework2.util.CategoryDetailParser
import com.example.homework2.util.ContactInfo
import com.example.homework2.util.ContactUtils
import com.example.homework2.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ImageItemState(
    val uriOrPath: String,
    val isCompressing: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun PublishScreen(
    viewModel: MarketViewModel,
    initialType: String = "secondhand",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    var type by remember { mutableStateOf(initialType) }

    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (initialType == "group") "group" else "books") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var maxCount by remember { mutableStateOf("2") }
    var deadline by remember { mutableStateOf("20:00") }

    // 联系方式 State (自动带入个人资料中的 wechat 和 qq)
    var overrideWechat by remember { mutableStateOf(currentUser?.wechatId ?: "") }
    var overrideQq by remember { mutableStateOf(currentUser?.qqNumber ?: "") }
    var isContactPublic by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        if (overrideWechat.isBlank() && currentUser?.wechatId?.isNotBlank() == true) {
            overrideWechat = currentUser?.wechatId ?: ""
        }
        if (overrideQq.isBlank() && currentUser?.qqNumber?.isNotBlank() == true) {
            overrideQq = currentUser?.qqNumber ?: ""
        }
    }

    // --- 分类专属字段 State ---
    var bookName by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var bookPublisher by remember { mutableStateOf("") }
    var bookEdition by remember { mutableStateOf("第1版") }
    var bookIsbn by remember { mutableStateOf("") }
    var bookCourse by remember { mutableStateOf("") }
    var bookNotes by remember { mutableStateOf("无笔记") }
    var bookDamaged by remember { mutableStateOf("无破损") }
    var isLatestEdition by remember { mutableStateOf(true) }

    var editionExpanded by remember { mutableStateOf(false) }
    val editionOptions = listOf("第1版", "第2版", "第3版", "第4版", "第5版", "第6版", "第7版", "第8版", "第9版", "第10版", "第17版", "第18版", "其他")
    var isEditionWarningDismissed by remember { mutableStateOf(false) }

    var digitalBrand by remember { mutableStateOf("") }
    var digitalModel by remember { mutableStateOf("") }
    var digitalPurchaseDate by remember { mutableStateOf("") }
    var digitalSpecs by remember { mutableStateOf("") }
    var digitalWarranty by remember { mutableStateOf("在保") }
    var digitalBatteryHealth by remember { mutableStateOf("95%") }
    val digitalAccessories = remember { mutableStateListOf("充电器", "包装盒") }
    var digitalRepaired by remember { mutableStateOf("无拆修") }

    var dailyBrand by remember { mutableStateOf("") }
    var dailyUsageDuration by remember { mutableStateOf("3个月") }
    var dailyFlaw by remember { mutableStateOf("无瑕疵") }

    var invalidFields by remember { mutableStateOf(setOf<String>()) }
    var showPriceAnomalyDialog by remember { mutableStateOf(false) }
    var pendingPublishAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var priceAnomalyText by remember { mutableStateOf("") }

    var isFieldsHighlighted by remember { mutableStateOf(false) }
    val imageList = remember { mutableStateListOf<ImageItemState>() }
    val allGoodsList by viewModel.allGoods.collectAsState()

    val existingSameBookList = remember(bookName, title, allGoodsList) {
        val queryName = bookName.ifBlank { title }.trim()
        if (queryName.length >= 2) {
            allGoodsList.filter {
                it.category == "books" && (it.title.contains(queryName, ignoreCase = true) ||
                        CategoryDetailParser.fromJson(it.extraInfoJson)?.let { detail ->
                            detail is CategoryDetail.BookDetail && detail.bookName.contains(queryName, ignoreCase = true)
                        } == true)
            }
        } else emptyList()
    }

    val otherEditionBooks = remember(existingSameBookList, bookEdition) {
        existingSameBookList.filter {
            val detail = CategoryDetailParser.fromJson(it.extraInfoJson) as? CategoryDetail.BookDetail
            detail?.edition != bookEdition
        }
    }

    val isbnState by viewModel.isbnLookupState.collectAsState()

    LaunchedEffect(isbnState) {
        when (val st = isbnState) {
            is IsbnLookupState.Success -> {
                val book = st.bookInfo
                viewModel.resetIsbnState()

                isFieldsHighlighted = true
                bookIsbn = book.isbn
                delay(150)
                title = book.title
                bookName = book.title
                delay(150)
                bookAuthor = book.author
                delay(150)
                bookPublisher = book.publisher
                delay(150)
                bookEdition = book.edition

                if (book.coverUrl.isNotBlank() && imageList.none { it.uriOrPath == book.coverUrl }) {
                    if (imageList.size < 9) {
                        imageList.add(0, ImageItemState(uriOrPath = book.coverUrl))
                    }
                }

                Toast.makeText(context, "已为您自动录入《${book.title}》信息！", Toast.LENGTH_SHORT).show()
                delay(800)
                isFieldsHighlighted = false
            }

            is IsbnLookupState.NotFound -> {
                bookIsbn = st.isbn
                viewModel.resetIsbnState()
                Toast.makeText(context, "未找到该 ISBN 图书信息，已保留 ISBN，请手动填写", Toast.LENGTH_LONG).show()
            }

            is IsbnLookupState.Error -> {
                viewModel.resetIsbnState()
                Toast.makeText(context, st.message, Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showPriceEstimateDialog by remember { mutableStateOf(false) }
    var previewStartIndex by remember { mutableStateOf(-1) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShakeAnimation() {
        scope.launch {
            repeat(4) {
                shakeOffset.animateTo(12f, animationSpec = tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-12f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
        }
    }

    fun fillSampleData(presetTitle: String? = null, presetPrice: String? = null) {
        if (type == "group" || category == "group") {
            type = "group"
            category = "group"
            title = presetTitle ?: "麦当劳超值双人餐 差1人拼团分摊"
            price = presetPrice ?: "16.5"
            maxCount = "2"
            deadline = "20:30"
            location = "二食堂门口"
            description = "麦当劳双人餐拼单分摊，还差1人，楼下拿号当面享用。"
            if (imageList.isEmpty()) imageList.add(ImageItemState("https://picsum.photos/600/600?random=2"))
        } else if (category == "digital") {
            type = "secondhand"
            title = presetTitle ?: "iPad Air 5 64G 紫色 99新"
            price = presetPrice ?: "3100.0"
            digitalBrand = "Apple"
            digitalModel = "iPad Air 5"
            digitalPurchaseDate = "2023年9月"
            digitalSpecs = "64GB 紫色"
            digitalWarranty = "在保"
            digitalBatteryHealth = "98%"
            digitalRepaired = "无拆修"
            location = "紫荆12舍楼下"
            description = "无划痕保养极好，箱说齐全，屏幕贴膜，附送保护壳！"
            if (imageList.isEmpty()) imageList.add(ImageItemState("https://picsum.photos/600/600?random=3"))
        } else if (category == "daily") {
            type = "secondhand"
            title = presetTitle ?: "九成新九阳电水壶 1.8L 快速烧水"
            price = presetPrice ?: "35.0"
            dailyBrand = "九阳"
            dailyUsageDuration = "3个月"
            dailyFlaw = "无瑕疵"
            location = "紫荆5舍楼下"
            description = "宿舍可用低功率，3分钟快速烧开，毕业离校转让。"
            if (imageList.isEmpty()) imageList.add(ImageItemState("https://picsum.photos/600/600?random=5"))
        } else {
            type = "secondhand"
            category = "books"
            title = presetTitle ?: "考研高等数学辅导讲义（张宇第18讲）"
            price = presetPrice ?: "18.0"
            bookName = "高等数学辅导讲义"
            bookAuthor = "张宇"
            bookPublisher = "北京理工大学出版社"
            bookEdition = "第18版"
            isLatestEdition = true
            bookIsbn = "9787568288888"
            bookCourse = "高等数学(一)"
            bookNotes = "少量笔记"
            bookDamaged = "无破损"
            location = "紫荆宿舍5舍楼下"
            description = "九成新无划痕，随书附赠历年考研真题解析，宿舍楼下当面交付！"
            if (imageList.isEmpty()) imageList.add(ImageItemState("https://picsum.photos/600/600?random=1"))
        }
        invalidFields = emptySet()
        isEditionWarningDismissed = false
        Toast.makeText(context, "已一键填入示例内容！", Toast.LENGTH_SHORT).show()
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            val targetUri = cameraTempUri!!
            val placeholderIdx = imageList.size
            if (placeholderIdx < 9) {
                imageList.add(ImageItemState(uriOrPath = targetUri.toString(), isCompressing = true))
                scope.launch {
                    val compressedPath = ImageUtils.compressAndSaveImage(context, targetUri)
                    if (compressedPath != null && placeholderIdx < imageList.size) {
                        imageList[placeholderIdx] = ImageItemState(uriOrPath = compressedPath, isCompressing = false)
                    } else if (placeholderIdx < imageList.size) {
                        imageList.removeAt(placeholderIdx)
                        Toast.makeText(context, "图片压缩失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = ImageUtils.createCameraImageUri(context)
            cameraTempUri = uri
            takePictureLauncher.launch(uri)
        } else {
            showCameraPermissionDialog = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = (9 - imageList.size).coerceAtLeast(1)
        )
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { selectedUri ->
                if (imageList.size < 9) {
                    val currentIdx = imageList.size
                    imageList.add(ImageItemState(uriOrPath = selectedUri.toString(), isCompressing = true))
                    scope.launch {
                        val compressedPath = ImageUtils.compressAndSaveImage(context, selectedUri)
                        if (compressedPath != null && currentIdx < imageList.size) {
                            imageList[currentIdx] = ImageItemState(uriOrPath = compressedPath, isCompressing = false)
                        } else if (currentIdx < imageList.size) {
                            imageList.removeAt(currentIdx)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (type == "group") "发起拼单" else "发布二手闲置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { fillSampleData() }) {
                        Text("✨ 快捷填样", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 类型选择 Segment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                listOf("secondhand" to "🏷️ 发布二手", "group" to "🤝 发起拼单").forEach { (t, label) ->
                    val isSelected = type == t
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable {
                                type = t
                                if (t == "group") category = "group"
                                else if (category == "group") category = "books"
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 实物图片上传九宫格区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("实物图片 (第一张为封面)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${imageList.size}/9 张", color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val totalGridItems = if (imageList.size < 9) imageList.size + 1 else 9
                    val gridHeight = ((totalGridItems + 2) / 3 * 105).dp

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false
                    ) {
                        itemsIndexed(imageList) { index, item ->
                            ImageThumbnailItem(
                                item = item,
                                index = index,
                                totalCount = imageList.size,
                                onClick = { previewStartIndex = index },
                                onDelete = { imageList.removeAt(index) },
                                onMoveLeft = {
                                    if (index > 0) {
                                        val temp = imageList[index]
                                        imageList[index] = imageList[index - 1]
                                        imageList[index - 1] = temp
                                    }
                                },
                                onMoveRight = {
                                    if (index < imageList.size - 1) {
                                        val temp = imageList[index]
                                        imageList[index] = imageList[index + 1]
                                        imageList[index + 1] = temp
                                    }
                                }
                            )
                        }

                        if (imageList.size < 9) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(95.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                        .background(Color.LightGray.copy(alpha = 0.12f))
                                        .clickable { showBottomSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = "添加",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("添加图片", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 核心基础表单
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFieldsHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("热门示例标题 (点击自动填入):", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            listOf(
                                "📚 考研高数讲义" to "15.0",
                                "🍔 麦当劳双人餐" to "16.5",
                                "💻 iPad Air 5 64G" to "3100.0",
                                "🛋️ 九阳快速烧水壶" to "35.0"
                            )
                        ) { (presetTitleText, presetPriceText) ->
                            SuggestionChip(
                                onClick = { fillSampleData(presetTitleText.substring(2), presetPriceText) },
                                label = { Text(presetTitleText, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) invalidFields = invalidFields - "title"
                        },
                        label = { Text("标题 *") },
                        placeholder = { Text("例如：考研高数讲义 / 麦当劳拼餐") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = "title" in invalidFields
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = {
                                price = it
                                if (it.isNotBlank()) invalidFields = invalidFields - "price"
                            },
                            label = { Text(if (type == "group") "人均价格 (元) *" else "出售价格 (元) *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = "price" in invalidFields
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        AssistChip(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "先填标题，AI 才能联动分类参数估价", Toast.LENGTH_SHORT).show()
                                } else {
                                    showPriceEstimateDialog = true
                                }
                            },
                            label = { Text("AI 估价", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("选择分类 *", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "books" to "📚 二手书",
                            "digital" to "💻 数码",
                            "daily" to "🛋️ 生活",
                            "group" to "🤝 拼单"
                        ).forEach { (catId, catName) ->
                            val isSelected = category == catId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    category = catId
                                    if (catId == "group") type = "group"
                                    else if (type == "group") type = "secondhand"
                                },
                                label = { Text(catName, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重点功能：卖家联系方式专属配置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("卖家联系方式 (本次交易专用)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("向买家公开", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = isContactPublic,
                                onCheckedChange = { isContactPublic = it }
                            )
                        }
                    }

                    if (isContactPublic) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = overrideWechat,
                                onValueChange = { overrideWechat = it.trim() },
                                label = { Text("微信号") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = overrideQq,
                                onValueChange = { overrideQq = it.trim() },
                                label = { Text("QQ 号") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("💡 微信号和 QQ 号至少填一项，买家点击解锁后方可展示。", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("🔒 当前设置为【不公开联系方式】，买家仅可通过站内留言沟通。", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 按分类动态表单区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFieldsHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (category) {
                                "books" -> "📚 二手书 / 教材专属参数"
                                "digital" -> "💻 数码产品专属参数"
                                "daily" -> "🛋️ 生活用品专属参数"
                                else -> "🤝 拼单专属规则"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (category == "books") {
                            OutlinedButton(
                                onClick = {
                                    BarcodeScannerHelper.startIsbnScan(
                                        context = context,
                                        onSuccess = { isbn -> viewModel.searchBookByIsbn(isbn) }
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "扫码",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("扫码录入", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (category == "books" && isbnState is IsbnLookupState.Loading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔍 正在联网查询 ISBN 图书信息，请稍候...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedContent(
                        targetState = category,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) + slideInVertically { 30 } togetherWith
                                    fadeOut(animationSpec = tween(200))
                        },
                        label = "dynamic_category_form"
                    ) { currentCategory ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (currentCategory) {
                                "books" -> {
                                    OutlinedTextField(
                                        value = bookName,
                                        onValueChange = { bookName = it; if (it.isNotBlank()) invalidFields = invalidFields - "bookName" },
                                        label = { Text("书名 *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        isError = "bookName" in invalidFields
                                    )

                                    AnimatedVisibility(
                                        visible = otherEditionBooks.isNotEmpty() && !isEditionWarningDismissed,
                                        enter = slideInVertically { -30 } + fadeIn(),
                                        exit = slideOutVertically { -30 } + fadeOut()
                                    ) {
                                        val otherEditions = otherEditionBooks.map {
                                            (CategoryDetailParser.fromJson(it.extraInfoJson) as? CategoryDetail.BookDetail)?.edition ?: "旧版"
                                        }.distinct().joinToString("、")
                                        val minP = otherEditionBooks.minOfOrNull { it.price } ?: 10.0
                                        val maxP = otherEditionBooks.maxOfOrNull { it.price } ?: 30.0

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "提示",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "⚠️ 提示：本校已有该书【$otherEditions】在售，价格区间 ¥$minP - ¥$maxP。版次不同价格差异较大，请仔细核对所需版次后定价！",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f),
                                                    lineHeight = 16.sp
                                                )
                                                IconButton(
                                                    onClick = { isEditionWarningDismissed = true },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = bookAuthor,
                                            onValueChange = { bookAuthor = it; if (it.isNotBlank()) invalidFields = invalidFields - "bookAuthor" },
                                            label = { Text("作者 *") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            isError = "bookAuthor" in invalidFields
                                        )

                                        ExposedDropdownMenuBox(
                                            expanded = editionExpanded,
                                            onExpandedChange = { editionExpanded = !editionExpanded },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                value = bookEdition,
                                                onValueChange = {
                                                    bookEdition = it
                                                    if (it.isNotBlank()) invalidFields = invalidFields - "bookEdition"
                                                },
                                                label = { Text("版次 *") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editionExpanded) },
                                                modifier = Modifier.menuAnchor(),
                                                singleLine = true,
                                                isError = "bookEdition" in invalidFields
                                            )

                                            ExposedDropdownMenu(
                                                expanded = editionExpanded,
                                                onDismissRequest = { editionExpanded = false }
                                            ) {
                                                editionOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option) },
                                                        onClick = {
                                                            bookEdition = option
                                                            editionExpanded = false
                                                            if (option.isNotBlank()) invalidFields = invalidFields - "bookEdition"
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("是否为最新版教材", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = isLatestEdition,
                                            onCheckedChange = { isLatestEdition = it }
                                        )
                                    }

                                    OutlinedTextField(
                                        value = bookPublisher,
                                        onValueChange = { bookPublisher = it; if (it.isNotBlank()) invalidFields = invalidFields - "bookPublisher" },
                                        label = { Text("出版社 *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        isError = "bookPublisher" in invalidFields
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = bookIsbn,
                                            onValueChange = { bookIsbn = it },
                                            label = { Text("ISBN (选填)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = bookCourse,
                                            onValueChange = { bookCourse = it },
                                            label = { Text("适用课程 (选填)") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Text("笔记情况", fontSize = 13.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("无笔记", "少量笔记", "大量笔记").forEach { option ->
                                            FilterChip(
                                                selected = bookNotes == option,
                                                onClick = { bookNotes = option },
                                                label = { Text(option, fontSize = 12.sp) }
                                            )
                                        }
                                    }

                                    Text("破损/缺页", fontSize = 13.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("无破损", "少量划痕", "严重破损").forEach { option ->
                                            FilterChip(
                                                selected = bookDamaged == option,
                                                onClick = { bookDamaged = option },
                                                label = { Text(option, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                "digital" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = digitalBrand,
                                            onValueChange = { digitalBrand = it; if (it.isNotBlank()) invalidFields = invalidFields - "digitalBrand" },
                                            label = { Text("品牌 (如Apple) *") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            isError = "digitalBrand" in invalidFields
                                        )
                                        OutlinedTextField(
                                            value = digitalModel,
                                            onValueChange = { digitalModel = it; if (it.isNotBlank()) invalidFields = invalidFields - "digitalModel" },
                                            label = { Text("型号 (如iPad Air 5) *") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            isError = "digitalModel" in invalidFields
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = digitalPurchaseDate,
                                            onValueChange = { digitalPurchaseDate = it; if (it.isNotBlank()) invalidFields = invalidFields - "digitalPurchaseDate" },
                                            label = { Text("购买时间 *") },
                                            placeholder = { Text("如2023年9月") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            isError = "digitalPurchaseDate" in invalidFields
                                        )
                                        OutlinedTextField(
                                            value = digitalBatteryHealth,
                                            onValueChange = { digitalBatteryHealth = it },
                                            label = { Text("电池健康度 (选填)") },
                                            placeholder = { Text("如95%") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    OutlinedTextField(
                                        value = digitalSpecs,
                                        onValueChange = { digitalSpecs = it },
                                        label = { Text("配置/容量规格 (选填)") },
                                        placeholder = { Text("例如 256GB 紫色 Wifi版") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Text("保修情况", fontSize = 13.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("在保", "过保", "官换机").forEach { option ->
                                            FilterChip(
                                                selected = digitalWarranty == option,
                                                onClick = { digitalWarranty = option },
                                                label = { Text(option, fontSize = 12.sp) }
                                            )
                                        }
                                    }

                                    Text("拆修记录", fontSize = 13.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("无拆修", "换过电池", "修过屏幕").forEach { option ->
                                            FilterChip(
                                                selected = digitalRepaired == option,
                                                onClick = { digitalRepaired = option },
                                                label = { Text(option, fontSize = 12.sp) }
                                            )
                                        }
                                    }

                                    Text("包含配件 (多选)", fontSize = 13.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("充电器", "包装盒", "发票", "保护壳").forEach { acc ->
                                            val isSelected = acc in digitalAccessories
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    if (isSelected) digitalAccessories.remove(acc)
                                                    else digitalAccessories.add(acc)
                                                },
                                                label = { Text(acc, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                "daily" -> {
                                    OutlinedTextField(
                                        value = dailyBrand,
                                        onValueChange = { dailyBrand = it },
                                        label = { Text("品牌 (选填)") },
                                        placeholder = { Text("如: 九阳 / 宜家") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = dailyUsageDuration,
                                        onValueChange = { dailyUsageDuration = it },
                                        label = { Text("使用时长 (选填)") },
                                        placeholder = { Text("如: 3个月 / 1学期") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Text("瑕疵情况", fontSize = 13.sp, color = Color.Gray)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("无瑕疵", "轻微磨损", "有小划痕").forEach { option ->
                                            FilterChip(
                                                selected = dailyFlaw == option,
                                                onClick = { dailyFlaw = option },
                                                label = { Text(option, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                "group" -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = maxCount,
                                            onValueChange = { maxCount = it },
                                            label = { Text("拼单人数上限 (人) *") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = deadline,
                                            onValueChange = { deadline = it },
                                            label = { Text("截止时间 *") },
                                            placeholder = { Text("如 20:00") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 地点与详细描述说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("交易 / 集合地点") },
                        placeholder = { Text("例: 宿舍5舍楼下 / 食堂门口") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("详细描述说明") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 确认发布逻辑函数
            val performPublish: () -> Unit = {
                val categoryDetail: CategoryDetail? = when (category) {
                    "books" -> CategoryDetail.BookDetail(
                        bookName = bookName,
                        author = bookAuthor,
                        publisher = bookPublisher,
                        edition = bookEdition,
                        isbn = bookIsbn,
                        course = bookCourse,
                        notes = bookNotes,
                        damaged = bookDamaged,
                        isLatestEdition = isLatestEdition
                    )
                    "digital" -> CategoryDetail.DigitalDetail(
                        brand = digitalBrand,
                        model = digitalModel,
                        purchaseDate = digitalPurchaseDate,
                        specs = digitalSpecs,
                        warranty = digitalWarranty,
                        batteryHealth = digitalBatteryHealth,
                        accessories = digitalAccessories.toList(),
                        repaired = digitalRepaired
                    )
                    "daily" -> CategoryDetail.DailyDetail(
                        brand = dailyBrand,
                        usageDuration = dailyUsageDuration,
                        flaw = dailyFlaw
                    )
                    else -> null
                }

                val extraJson = CategoryDetailParser.toJson(categoryDetail)
                val contactJson = ContactUtils.toJson(
                    ContactInfo(
                        wechatId = overrideWechat,
                        qqNumber = overrideQq,
                        isPublic = isContactPublic
                    )
                )
                val paths = imageList.map { it.uriOrPath }

                viewModel.publishGoods(
                    title = title,
                    price = price.toDouble(),
                    category = category,
                    type = type,
                    images = paths,
                    description = description,
                    location = location,
                    maxCount = maxCount.toIntOrNull() ?: 2,
                    deadline = deadline,
                    extraInfoJson = extraJson,
                    contactOverrideJson = contactJson,
                    onSuccess = {
                        Toast.makeText(context, "发布成功！", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                )
            }

            // 确认发布按钮
            Button(
                onClick = {
                    val newInvalids = mutableSetOf<String>()

                    if (imageList.isEmpty()) {
                        triggerShakeAnimation()
                        Toast.makeText(context, "至少上传一张实物图", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (title.isBlank()) newInvalids.add("title")
                    val priceVal = price.toDoubleOrNull()
                    if (priceVal == null || priceVal <= 0) newInvalids.add("price")

                    if (isContactPublic && overrideWechat.isBlank() && overrideQq.isBlank()) {
                        Toast.makeText(context, "请至少填写一种联系方式 (微信号或QQ号)", Toast.LENGTH_SHORT).show()
                        triggerShakeAnimation()
                        return@Button
                    }

                    if (isContactPublic) {
                        if (overrideWechat.isNotBlank() && !ContactUtils.isValidWechatId(overrideWechat)) {
                            Toast.makeText(context, "微信号格式不正确", Toast.LENGTH_SHORT).show()
                            triggerShakeAnimation()
                            return@Button
                        }
                        if (overrideQq.isNotBlank() && !ContactUtils.isValidQqNumber(overrideQq)) {
                            Toast.makeText(context, "QQ号格式不正确", Toast.LENGTH_SHORT).show()
                            triggerShakeAnimation()
                            return@Button
                        }
                    }

                    when (category) {
                        "books" -> {
                            if (bookName.isBlank()) newInvalids.add("bookName")
                            if (bookAuthor.isBlank()) newInvalids.add("bookAuthor")
                            if (bookPublisher.isBlank()) newInvalids.add("bookPublisher")
                            if (bookEdition.isBlank()) newInvalids.add("bookEdition")
                        }
                        "digital" -> {
                            if (digitalBrand.isBlank()) newInvalids.add("digitalBrand")
                            if (digitalModel.isBlank()) newInvalids.add("digitalModel")
                            if (digitalPurchaseDate.isBlank()) newInvalids.add("digitalPurchaseDate")
                        }
                    }

                    if (newInvalids.isNotEmpty()) {
                        invalidFields = newInvalids
                        triggerShakeAnimation()
                        Toast.makeText(context, "请先填写标红的必填项！", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (category == "books" && existingSameBookList.isNotEmpty()) {
                        val sameEditionBooks = existingSameBookList.filter {
                            val d = CategoryDetailParser.fromJson(it.extraInfoJson) as? CategoryDetail.BookDetail
                            d?.edition == bookEdition
                        }
                        if (sameEditionBooks.isNotEmpty()) {
                            val avgPrice = sameEditionBooks.map { it.price }.average()
                            if (priceVal!! < avgPrice * 0.5 || priceVal > avgPrice * 1.5) {
                                priceAnomalyText = "提示：本校同版次《$bookName》($bookEdition) 均价为 ¥${Math.round(avgPrice * 10) / 10.0}，您设定的价格为 ¥$priceVal，偏离均价较多。确定按此价格发布吗？"
                                pendingPublishAction = performPublish
                                showPriceAnomalyDialog = true
                                return@Button
                            }
                        }
                    }

                    performPublish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("确认发布", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ModalBottomSheet 选择拍照或相册
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("选择上传方式", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                ListItem(
                    headlineContent = { Text("📷 拍照上传") },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        val checkPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (checkPermission == PackageManager.PERMISSION_GRANTED) {
                            val uri = ImageUtils.createCameraImageUri(context)
                            cameraTempUri = uri
                            takePictureLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("🖼️ 从手机相册选择") },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
    }

    // 价格离群预警弹窗
    if (showPriceAnomalyDialog) {
        AlertDialog(
            onDismissRequest = { showPriceAnomalyDialog = false },
            title = { Text("⚠️ 价格离群提示") },
            text = { Text(priceAnomalyText) },
            confirmButton = {
                Button(onClick = {
                    showPriceAnomalyDialog = false
                    pendingPublishAction?.invoke()
                }) {
                    Text("坚持发布")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceAnomalyDialog = false }) {
                    Text("修改价格")
                }
            }
        )
    }

    // 权限被拒绝对话框
    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text("相机权限已禁用") },
            text = { Text("闲圈需要相机权限以拍摄物品照片。请在设置中开启相机权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showCameraPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // AI 估价弹窗
    if (showPriceEstimateDialog) {
        val currentCategoryDetail: CategoryDetail? = when (category) {
            "books" -> CategoryDetail.BookDetail(
                bookName = bookName,
                author = bookAuthor,
                publisher = bookPublisher,
                edition = bookEdition,
                isbn = bookIsbn,
                course = bookCourse,
                notes = bookNotes,
                damaged = bookDamaged,
                isLatestEdition = isLatestEdition
            )
            "digital" -> CategoryDetail.DigitalDetail(
                brand = digitalBrand,
                model = digitalModel,
                purchaseDate = digitalPurchaseDate,
                specs = digitalSpecs,
                warranty = digitalWarranty,
                batteryHealth = digitalBatteryHealth,
                accessories = digitalAccessories.toList(),
                repaired = digitalRepaired
            )
            "daily" -> CategoryDetail.DailyDetail(
                brand = dailyBrand,
                usageDuration = dailyUsageDuration,
                flaw = dailyFlaw
            )
            else -> null
        }

        val estimateResult = remember(title, category, type, currentCategoryDetail) {
            AiEstimateEngine.estimate(
                title = title,
                category = category,
                type = type,
                categoryDetail = currentCategoryDetail
            )
        }

        AlertDialog(
            onDismissRequest = { showPriceEstimateDialog = false },
            title = { Text("✨ AI 智能估价分析") },
            text = {
                Column {
                    Text("结合分类与专属细分参数分析：", fontSize = 13.sp, color = Color.Gray)
                    if (estimateResult.paramSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(estimateResult.paramSummary, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "建议定价范围：¥${estimateResult.minPrice} - ¥${estimateResult.maxPrice}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        estimateResult.reasoning,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    price = estimateResult.suggestedPrice.toString()
                    showPriceEstimateDialog = false
                    Toast.makeText(context, "已采纳 AI 建议估价 ¥${estimateResult.suggestedPrice}", Toast.LENGTH_SHORT).show()
                }) {
                    Text("采纳估价 (¥${estimateResult.suggestedPrice}元)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceEstimateDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 全屏预览 Dialog
    if (previewStartIndex in imageList.indices) {
        FullScreenPreviewDialog(
            images = imageList.map { it.uriOrPath },
            startIndex = previewStartIndex,
            onDismiss = { previewStartIndex = -1 }
        )
    }
}

@Composable
fun ImageThumbnailItem(
    item: ImageItemState,
    index: Int,
    totalCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(95.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(item.uriOrPath)
                    .crossfade(true)
                    .build()
            ),
            contentDescription = "图片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (index == 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomEnd = 8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("封面", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (item.isCompressing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (index > 0) {
                Text(
                    "◀",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clickable(onClick = onMoveLeft)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else Spacer(modifier = Modifier.width(1.dp))

            if (index < totalCount - 1) {
                Text(
                    "▶",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .clickable(onClick = onMoveRight)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenPreviewDialog(
    images: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { images.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 3.5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(context)
                                .data(images[page])
                                .build()
                        ),
                        contentDescription = "全屏预览",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${images.size}",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
