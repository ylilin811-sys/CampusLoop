package com.example.homework2

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homework2.ui.components.InAppBanner
import com.example.homework2.ui.detail.DetailScreen
import com.example.homework2.ui.home.HomeScreen
import com.example.homework2.ui.messages.MessageScreen
import com.example.homework2.ui.mine.EditProfileScreen
import com.example.homework2.ui.mine.MineScreen
import com.example.homework2.ui.navigation.BottomNavigationBar
import com.example.homework2.ui.navigation.Screen
import com.example.homework2.ui.publish.PublishScreen
import com.example.homework2.ui.splash.SplashScreen
import com.example.homework2.ui.theme.CampusMarketTheme
import com.example.homework2.ui.viewmodel.MarketViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.SCALE_X,
                1f,
                1.3f
            )
            slideUp.interpolator = AnticipateInterpolator()
            slideUp.duration = 400L

            val fadeOut = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.ALPHA,
                1f,
                0f
            )
            fadeOut.duration = 400L

            slideUp.doOnEnd { splashScreenView.remove() }
            slideUp.start()
            fadeOut.start()
        }

        setContent {
            CampusMarketTheme {
                val navController = rememberNavController()
                val viewModel: MarketViewModel = viewModel()

                val isLoading by viewModel.isLoading.collectAsState()
                val bannerMessage by viewModel.bannerMessage.collectAsState()

                // Android 13+ 通知权限申请
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(this, "通知权限受限，新消息将仅在应用内提示", Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val check = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                        if (check != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                splashScreen.setKeepOnScreenCondition { isLoading && viewModel.goodsList.value.isEmpty() }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Publish.route,
                    Screen.Messages.route,
                    Screen.Mine.route
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                BottomNavigationBar(navController = navController)
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Splash.route) {
                                SplashScreen(
                                    viewModel = viewModel,
                                    onNavigateNext = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Home.route) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { goodsId ->
                                        navController.navigate(Screen.Detail.createRoute(goodsId))
                                    },
                                    onNavigateToPublish = { type ->
                                        navController.navigate("publish_with_type/$type")
                                    }
                                )
                            }

                            composable(Screen.Publish.route) {
                                PublishScreen(
                                    viewModel = viewModel,
                                    initialType = "secondhand",
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "publish_with_type/{type}",
                                arguments = listOf(navArgument("type") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val publishType = backStackEntry.arguments?.getString("type") ?: "secondhand"
                                PublishScreen(
                                    viewModel = viewModel,
                                    initialType = publishType,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Messages.route) {
                                MessageScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { goodsId ->
                                        navController.navigate(Screen.Detail.createRoute(goodsId))
                                    },
                                    onNavigateToHome = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(
                                route = Screen.Detail.route,
                                arguments = listOf(navArgument("goodsId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val goodsId = backStackEntry.arguments?.getLong("goodsId") ?: 0L
                                DetailScreen(
                                    viewModel = viewModel,
                                    goodsId = goodsId,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Mine.route) {
                                MineScreen(
                                    viewModel = viewModel,
                                    onNavigateToDetail = { goodsId ->
                                        navController.navigate(Screen.Detail.createRoute(goodsId))
                                    },
                                    onNavigateToEditProfile = {
                                        navController.navigate(Screen.EditProfile.route)
                                    },
                                    onNavigateToPublish = {
                                        navController.navigate(Screen.Publish.route)
                                    },
                                    onNavigateToHomeGroup = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    },
                                    onReplayGuide = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Home.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.EditProfile.route) {
                                EditProfileScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    // 重点功能：前台应用内横幅通知 (顶端滑动滑入，3秒收起，点击跳转)
                    InAppBanner(
                        message = bannerMessage,
                        onDismiss = { viewModel.dismissBanner() },
                        onClick = {
                            val msg = bannerMessage
                            if (msg != null) {
                                viewModel.markMessageAsRead(msg.id)
                                if (msg.goodsId > 0) {
                                    navController.navigate(Screen.Detail.createRoute(msg.goodsId))
                                } else {
                                    navController.navigate(Screen.Messages.route)
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}
