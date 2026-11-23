package com.example.homework2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType(val code: String) {
    ORDER_REQUEST("ORDER_REQUEST"),   // 买家“我想要”下单申请
    ORDER_RESULT("ORDER_RESULT"),     // 卖家处理结果（同意/拒绝）
    GROUP_SUCCESS("GROUP_SUCCESS"),   // 拼单成功成团
    GROUP_FAILED("GROUP_FAILED"),     // 拼单未成团
    SYSTEM("SYSTEM")                  // 系统通知/欢迎消息
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiverId: Long = 1,
    val senderId: Long = 0,
    val senderName: String = "系统",
    val senderAvatar: String = "",
    val type: String, // MessageType code
    val title: String,
    val content: String,
    val goodsId: Long = 0,
    val groupId: Long = 0,
    val orderRequestId: Long = 0, // 关联的订单请求 ID
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
