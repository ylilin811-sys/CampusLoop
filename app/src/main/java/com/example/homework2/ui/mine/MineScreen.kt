package com.example.homework2.ui.mine

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
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
import com.example.homework2.data.local.entity.GoodsEntity
import com.example.homework2.ui.components.EmptyStateView
import com.example.homework2.ui.components.SoldStamp
import com.example.homework2.ui.state.EmptyScene
import com.example.homework2.ui.viewmodel.MarketViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    viewModel: MarketViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToHomeGroup: () -> Unit,
    onReplayGuide: () -> Unit
) {
    val context = LocalContext.current
    val allGoods by viewModel.allGoods.collectAsState()
    val favoriteGoods by viewModel.favoriteGoodsList.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTab by remember { mutableStateOf("publish") }
    var selectedGoodsForStatus by remember { mutableStateOf<GoodsEntity?>(null) }

    // 隐藏演示模式点击计数
    var versionTapCount by remember { mutableStateOf(0) }
    var isDemoModeUnlocked by remember { mutableStateOf(false) }

    val nickname = remember(currentUser) { currentUser?.nickname ?: "校园小霸王" }
    val avatarPath = remember(currentUser) { currentUser?.avatarPath ?: "" }

    val myPublishList = remember(allGoods, nickname) { allGoods.filter { it.sellerName == nickname || it.sellerName == "我" || it.id <= 5 } }
    val myGroupList = remember(allGoods) { allGoods.filter { it.type == "group" } }

    val currentDisplayList = when (selectedTab) {
        "publish" -> myPublishList
        "fav" -> favoriteGoods
        "group" -> myGroupList
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 1. 用户信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToEditProfile() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (avatarPath.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(avatarPath),
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            DefaultAvatar(nickname = nickname, size = 70.dp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🎓 已认证", fontSize = 11.sp, color = Color(0xFF07C160), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("计算机科学与技术系 / 紫荆宿舍区", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    AssistChip(
                        onClick = onNavigateToEditProfile,
                        label = { Text("编辑", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = "publish" }
                    ) {
                        Text("${myPublishList.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("我的发布", fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = "fav" }
                    ) {
                        Text("${favoriteGoods.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("我的收藏", fontSize = 12.sp, color = Color.Gray)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTab = "group" }
                    ) {
                        Text("${myGroupList.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("我的拼单", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Tab 列表卡片
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TabRow(selectedTabIndex = when (selectedTab) {
                    "publish" -> 0
                    "fav" -> 1
                    else -> 2
                }) {
                    Tab(
                        selected = selectedTab == "publish",
                        onClick = { selectedTab = "publish" },
                        text = { Text("我的发布") }
                    )
                    Tab(
                        selected = selectedTab == "fav",
                        onClick = { selectedTab = "fav" },
                        text = { Text("我的收藏") }
                    )
                    Tab(
                        selected = selectedTab == "group",
                        onClick = { selectedTab = "group" },
                        text = { Text("我的拼单") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (currentDisplayList.isEmpty()) {
                    val currentScene = when (selectedTab) {
                        "publish" -> EmptyScene.MY_POSTS
                        "fav" -> EmptyScene.FAVORITES
                        else -> EmptyScene.GROUPS
                    }

                    EmptyStateView(
                        scene = currentScene,
                        onActionClick = {
                            when (currentScene) {
                                EmptyScene.MY_POSTS -> onNavigateToPublish()
                                EmptyScene.FAVORITES -> onNavigateToHomeGroup()
                                EmptyScene.GROUPS -> onNavigateToHomeGroup()
                                else -> {}
                            }
                        }
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(currentDisplayList, key = { it.id }) { goods ->
                            MineGoodsRow(
                                goods = goods,
                                onClick = { onNavigateToDetail(goods.id) },
                                onStatusClick = { selectedGoodsForStatus = goods },
                                onDelete = {
                                    viewModel.deleteGoods(goods)
                                    Toast.makeText(context, "已删除发布", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 重点功能：隐藏“演示模式/测试工具卡片” (连续点击版本号 5 次解锁)
        if (isDemoModeUnlocked) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🧪 单机演示模式工具卡片", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.demoSimulateOrderRequest(context)
                                Toast.makeText(context, "模拟买家已对您的商品下单！", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("1. 模拟买家下单", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.demoSimulateJoinGroup(context)
                                Toast.makeText(context, "模拟用户加入拼单！", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("2. 模拟加入拼单", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.demoClearAllMessages()
                                Toast.makeText(context, "已清空所有消息记录", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("3. 清空消息", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. 设置与重置新手引导入口
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("重新查看新手引导") },
                    leadingContent = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable {
                        viewModel.resetGuides()
                        onReplayGuide()
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("关于闲圈 (v1.0.0)") },
                    leadingContent = { Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable {
                        versionTapCount++
                        if (versionTapCount >= 5) {
                            isDemoModeUnlocked = true
                            Toast.makeText(context, "🎉 已解锁开发者/演示模式工具卡片！", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "闲圈 Jetpack Compose 版 v1.0.0", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    selectedGoodsForStatus?.let { goods ->
        ModalBottomSheet(
            onDismissRequest = { selectedGoodsForStatus = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("修改商品交易状态", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                listOf(
                    "AVAILABLE" to "🟢 在售中 (正常展示排前)",
                    "RESERVED" to "🟡 已预定 (买家已定下)",
                    "SOLD" to "灰色 已售出 (盖已售出印章)"
                ).forEach { (statusKey, statusText) ->
                    ListItem(
                        headlineContent = { Text(statusText) },
                        trailingContent = {
                            if (goods.status == statusKey) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.clickable {
                            viewModel.updateGoodsStatus(goods.id, statusKey)
                            selectedGoodsForStatus = null
                            Toast.makeText(context, "商品状态已更新", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun MineGoodsRow(
    goods: GoodsEntity,
    onClick: () -> Unit,
    onStatusClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isSold = goods.status == "SOLD"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSold) MaterialTheme.colorScheme.background.copy(alpha = 0.6f) else MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            Image(
                painter = rememberAsyncImagePainter(goods.primaryImage.ifBlank { "https://picsum.photos/300/300?random=${goods.id}" }),
                contentDescription = goods.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            if (isSold) {
                SoldStamp(modifier = Modifier.scale(0.6f))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(goods.title, fontWeight = FontWeight.Bold, maxLines = 1, color = if (isSold) Color.Gray else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¥${goods.price}", color = if (isSold) Color.Gray else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = onStatusClick,
                    label = {
                        Text(
                            text = when (goods.status) {
                                "RESERVED" -> "已预定 ▾"
                                "SOLD" -> "已售出 ▾"
                                else -> "在售中 ▾"
                            },
                            fontSize = 10.sp
                        )
                    }
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Gray)
        }
    }
}
