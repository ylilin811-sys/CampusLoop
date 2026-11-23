package com.example.homework2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Long = 1, // 当前主登录用户默认为 ID 1
    val nickname: String,
    val avatarPath: String = "",
    val wechatId: String = "",
    val qqNumber: String = "",
    val updateTime: Long = System.currentTimeMillis()
)
