package com.example.homework2.ui.mine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.homework2.ui.viewmodel.MarketViewModel
import com.example.homework2.util.ContactUtils
import com.example.homework2.util.ImageUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: MarketViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()

    val initialNickname = remember(currentUser) { currentUser?.nickname ?: "校园小霸王" }
    val initialAvatarPath = remember(currentUser) { currentUser?.avatarPath ?: "" }
    val initialWechatId = remember(currentUser) { currentUser?.wechatId ?: "" }
    val initialQqNumber = remember(currentUser) { currentUser?.qqNumber ?: "" }

    var nickname by remember { mutableStateOf(initialNickname) }
    var avatarPath by remember { mutableStateOf(initialAvatarPath) }
    var wechatId by remember { mutableStateOf(initialWechatId) }
    var qqNumber by remember { mutableStateOf(initialQqNumber) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var wechatError by remember { mutableStateOf<String?>(null) }
    var qqError by remember { mutableStateOf<String?>(null) }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    var isAvatarPressed by remember { mutableStateOf(false) }
    val avatarScale by animateFloatAsState(
        targetValue = if (isAvatarPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "avatarScale"
    )

    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        scope.launch {
            repeat(4) {
                shakeOffset.animateTo(10f, tween(40, easing = LinearEasing))
                shakeOffset.animateTo(-10f, tween(40, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(40, easing = LinearEasing))
        }
    }

    var isSavingSuccess by remember { mutableStateOf(false) }
    val checkAnimProgress = remember { Animatable(0f) }

    val hasUnsavedChanges = remember(nickname, avatarPath, wechatId, qqNumber, initialNickname, initialAvatarPath, initialWechatId, initialQqNumber) {
        nickname != initialNickname || avatarPath != initialAvatarPath || wechatId != initialWechatId || qqNumber != initialQqNumber
    }

    fun handleBack() {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBack() }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTempUri != null) {
            scope.launch {
                val compressed = ImageUtils.compressAndSaveImage(context, cameraTempUri!!)
                if (compressed != null) {
                    avatarPath = compressed
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
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = ImageUtils.compressAndSaveImage(context, uri)
                if (compressed != null) {
                    avatarPath = compressed
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑个人资料") },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(avatarScale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isAvatarPressed = true
                                tryAwaitRelease()
                                isAvatarPressed = false
                            },
                            onTap = { showBottomSheet = true }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = avatarPath, label = "avatarCrossfade") { path ->
                    if (path.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(path)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "头像",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        DefaultAvatar(
                            nickname = nickname,
                            size = 110.dp,
                            fontSize = 42.dp,
                            modifier = Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "更换头像",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("点击更换头像", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // 昵称卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("个人昵称", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { input ->
                            if (input.length <= 12) {
                                nickname = input
                                errorMessage = null
                            }
                        },
                        placeholder = { Text("请输入2-12位昵称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessage != null,
                        trailingIcon = {
                            Text(
                                text = "${nickname.length}/12",
                                fontSize = 12.sp,
                                color = if (nickname.length in 2..12) Color.Gray else Color.Red,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重点功能：卖家联系方式区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("卖家联系方式 (至少填写一项)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = wechatId,
                        onValueChange = {
                            wechatId = it.trim()
                            wechatError = null
                        },
                        label = { Text("微信号") },
                        placeholder = { Text("例: wx_student88") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = wechatError != null
                    )
                    if (wechatError != null) {
                        Text(wechatError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = qqNumber,
                        onValueChange = {
                            qqNumber = it.trim()
                            qqError = null
                        },
                        label = { Text("QQ 号") },
                        placeholder = { Text("例: 12345678") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = qqError != null
                    )
                    if (qqError != null) {
                        Text(qqError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("💡 小提示：联系方式仅在买家主动点击查看时解密显示，请放心填写并保护隐私。", fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    val trimmedNick = nickname.trim()
                    val regexNick = Regex("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{2,12}$")
                    if (trimmedNick.length < 2) {
                        errorMessage = "昵称至少需要 2 个字符"
                        triggerShake()
                        return@Button
                    }
                    if (!regexNick.matches(trimmedNick)) {
                        errorMessage = "只能包含中英文、数字及下划线"
                        triggerShake()
                        return@Button
                    }

                    if (wechatId.isNotBlank() && !ContactUtils.isValidWechatId(wechatId)) {
                        wechatError = "微信号格式不正确 (6-20位以字母开头，仅含字母数字下划线/减号)"
                        triggerShake()
                        return@Button
                    }

                    if (qqNumber.isNotBlank() && !ContactUtils.isValidQqNumber(qqNumber)) {
                        qqError = "QQ号格式不正确 (5-11位纯数字)"
                        triggerShake()
                        return@Button
                    }

                    scope.launch {
                        val isTaken = viewModel.checkNicknameExists(trimmedNick)
                        if (isTaken) {
                            errorMessage = "该昵称已被使用，请换一个吧"
                            triggerShake()
                            return@launch
                        }

                        viewModel.saveUserProfile(trimmedNick, avatarPath, wechatId, qqNumber)

                        isSavingSuccess = true
                        checkAnimProgress.animateTo(1f, tween(400))
                        delay(300)
                        Toast.makeText(context, "资料更新成功！", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSavingSuccess) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.5f)
                                lineTo(size.width * 0.45f, size.height * 0.75f)
                                lineTo(size.width * 0.8f, size.height * 0.25f)
                            }
                            val pathMeasure = PathMeasure().apply { setPath(path, false) }
                            val subPath = Path()
                            pathMeasure.getSegment(0f, pathMeasure.length * checkAnimProgress.value, subPath, true)

                            drawPath(
                                path = subPath,
                                color = Color.White,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存成功", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("保存修改", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("更换头像", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))

                ListItem(
                    headlineContent = { Text("📷 拍照") },
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
                    headlineContent = { Text("🖼️ 从相册选择") },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("🔄 恢复默认头像", color = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        avatarPath = ""
                        Toast.makeText(context, "已恢复默认组合头像", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("放弃修改？") },
            text = { Text("有未保存的修改，确定要放弃修改并返回吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    onNavigateBack()
                }) {
                    Text("确定放弃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("继续编辑")
                }
            }
        )
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text("相机权限已禁用") },
            text = { Text("闲圈需要相机权限以拍摄个人头像照片。请在设置中开启相机权限。") },
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
}
