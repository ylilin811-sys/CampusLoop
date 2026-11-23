package com.example.homework2.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Splash : Screen("splash", "启动页")
    object Home : Screen("home", "首页")
    object Publish : Screen("publish", "发布")
    object Messages : Screen("messages", "消息")
    object Mine : Screen("mine", "我的")
    object EditProfile : Screen("edit_profile", "编辑个人资料")
    object Detail : Screen("detail/{goodsId}", "详情") {
        fun createRoute(goodsId: Long) = "detail/$goodsId"
    }
}
