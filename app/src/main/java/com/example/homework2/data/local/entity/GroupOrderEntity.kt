package com.example.homework2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_orders")
data class GroupOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goodsId: Long,
    val memberName: String,
    val memberAvatar: String,
    val isCreator: Boolean = false,
    val joinTime: Long = System.currentTimeMillis()
)
