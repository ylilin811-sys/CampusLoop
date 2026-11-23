package com.example.homework2.ui.messages

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.MessageType
import com.example.homework2.domain.usecase.OrderRequestResult
import com.example.homework2.ui.components.EmptyStateView
import com.example.homework2.ui.mine.DefaultAvatar
import com.example.homework2.ui.state.EmptyScene
import com.example.homework2.ui.theme.*
import com.example.homework2.ui.viewmodel.MarketViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    viewModel: MarketViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val messages by viewModel.messagesList.collectAsState()
    var recentlyDeletedMessage by remember { mutableStateOf<MessageEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消息通知") },
                actions = {
                    if (messages.any { !it.isRead }) {
                        TextButton(onClick = {
                            viewModel.markAllMessagesAsRead()
                            Toast.makeText(context, "已全部标记为已读", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "全部已读",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("全部已读", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (messages.isEmpty()) {
                EmptyStateView(
                    scene = EmptyScene.MESSAGES,
                    onActionClick = onNavigateToHome
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    recentlyDeletedMessage = message
                                    viewModel.deleteMessage(message.id)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "消息已删除",
                                            actionLabel = "撤销",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            recentlyDeletedMessage?.let { deleted ->
                                                viewModel.restoreMessage(deleted)
                                            }
                                        }
                                    }
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MessageUnreadCardBgLight)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MessageTimeColorLight
                                    )
                                }
                            },
                            content = {
                                MessageItemCard(
                                    viewModel = viewModel,
                                    message = message,
                                    onClick = {
                                        viewModel.markMessageAsRead(message.id)
                                        if (message.goodsId > 0) {
                                            onNavigateToDetail(message.goodsId)
                                        }
                                    },
                                    onShowSnackbar = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MessageItemCard(
    viewModel: MarketViewModel,
    message: MessageEntity,
    onClick: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // 从 Room Flow 实时观察最新 OrderRequestEntity 状态 (防止用本地临时变量，重新进入页面 100% 保持一致)
    val orderRequestState by remember(message.orderRequestId) {
        viewModel.getOrderRequestByIdFlow(message.orderRequestId)
    }.collectAsState(initial = null)

    var isProcessing by remember { mutableStateOf(false) }

    val currentStatus = orderRequestState?.status ?: "PENDING"

    val cardBg = if (!message.isRead) {
        if (isDark) MessageUnreadCardBgDark else MessageUnreadCardBgLight
    } else {
        if (isDark) MessageReadCardBgDark else MessageReadCardBgLight
    }
    val borderColor = if (isDark) MessageCardBorderDark else MessageCardBorderLight
    val titleColor = if (isDark) MessageTitleColorDark else MessageTitleColorLight
    val bodyColor = if (isDark) MessageBodyColorDark else MessageBodyColorLight
    val timeColor = if (isDark) MessageTimeColorDark else MessageTimeColorLight
    val dividerColor = if (isDark) MessageDividerColorDark else MessageDividerColorLight

    val infiniteTransition = rememberInfiniteTransition(label = "pulseDot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.createdAt) { dateFormat.format(Date(message.createdAt)) }

    val iconText = when (message.type) {
        MessageType.ORDER_REQUEST.code -> "📦"
        MessageType.ORDER_RESULT.code -> "✅"
        MessageType.GROUP_SUCCESS.code -> "🤝"
        MessageType.GROUP_FAILED.code -> "💔"
        else -> "🎉"
    }

    val cleanTitle = remember(message.title) {
        message.title.replace(Regex("^[📦✅🤝💔🎉🔔\\s]+"), "").trim()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!message.isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange.copy(alpha = dotAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (message.senderAvatar.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(message.senderAvatar),
                        contentDescription = message.senderName,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    DefaultAvatar(nickname = message.senderName, size = 42.dp, fontSize = 18.dp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$iconText $cleanTitle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = titleColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeStr,
                            fontSize = 11.sp,
                            color = timeColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = message.content,
                        fontSize = 13.sp,
                        color = bodyColor,
                        lineHeight = 18.sp
                    )
                }
            }

            // 重点功能：根据 OrderRequest.status 严格状态机渲染按钮或结果标签
            if (message.type == MessageType.ORDER_REQUEST.code && message.orderRequestId > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(10.dp))

                // 淡入 + 轻微缩放过渡动画
                AnimatedContent(
                    targetState = currentStatus,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.85f) togetherWith
                                fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.85f)
                    },
                    label = "orderRequestStatusMorph"
                ) { status ->
                    when (status) {
                        "ACCEPTED" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F8F0)
                            ) {
                                Text(
                                    text = "✅ 已同意并预定",
                                    color = Color(0xFF07C160),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        "REJECTED" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF2F2F2)
                            ) {
                                Text(
                                    text = "❌ 已拒绝",
                                    color = Color(0xFF757575),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        else -> { // PENDING
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryOrange
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (isProcessing) return@OutlinedButton
                                        isProcessing = true
                                        viewModel.handleOrderRequest(context, message.orderRequestId, false) { result ->
                                            isProcessing = false
                                            when (result) {
                                                OrderRequestResult.SuccessRejected -> {
                                                    Toast.makeText(context, "已拒绝买家申请", Toast.LENGTH_SHORT).show()
                                                }
                                                OrderRequestResult.AlreadyProcessed -> {
                                                    onShowSnackbar("该申请已在其他设备或页面处理完毕")
                                                }
                                                else -> {}
                                            }
                                        }
                                    },
                                    enabled = !isProcessing,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, PrimaryOrange),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PrimaryOrange,
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    Text("拒绝", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = {
                                        if (isProcessing) return@Button
                                        isProcessing = true
                                        viewModel.handleOrderRequest(context, message.orderRequestId, true) { result ->
                                            isProcessing = false
                                            when (result) {
                                                OrderRequestResult.SuccessAccepted -> {
                                                    Toast.makeText(context, "已同意买家申请，商品已自动置为预定状态！", Toast.LENGTH_SHORT).show()
                                                }
                                                OrderRequestResult.AlreadyProcessed -> {
                                                    onShowSnackbar("该申请已在其他设备或页面处理完毕")
                                                }
                                                OrderRequestResult.GoodsAlreadyReserved -> {
                                                    onShowSnackbar("该商品已被其他学友预定，无法重复预定")
                                                }
                                                else -> {}
                                            }
                                        }
                                    },
                                    enabled = !isProcessing,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryOrange,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("同意并预定", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
