package com.example.homework2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OrderRequestStatus(val code: String, val label: String) {
    PENDING("PENDING", "待处理"),
    ACCEPTED("ACCEPTED", "已同意"),
    REJECTED("REJECTED", "已拒绝")
}

@Entity(tableName = "order_requests")
data class OrderRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goodsId: Long,
    val buyerId: Long,
    val buyerName: String,
    val buyerAvatar: String,
    val sellerId: Long = 1,
    val status: String = "PENDING", // OrderRequestStatus code
    val createdAt: Long = System.currentTimeMillis()
)
