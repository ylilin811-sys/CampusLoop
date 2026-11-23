package com.example.homework2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.homework2.data.local.Converters

enum class GoodsStatus(val code: String, val label: String) {
    AVAILABLE("AVAILABLE", "在售"),
    RESERVED("RESERVED", "已预定"),
    SOLD("SOLD", "已售出")
}

sealed class CategoryDetail {
    data class BookDetail(
        val bookName: String,
        val author: String,
        val publisher: String,
        val edition: String,
        val isbn: String = "",
        val course: String = "",
        val notes: String = "无笔记",
        val damaged: String = "无破损",
        val isLatestEdition: Boolean = false
    ) : CategoryDetail()

    data class DigitalDetail(
        val brand: String,
        val model: String,
        val purchaseDate: String,
        val specs: String = "",
        val warranty: String = "在保",
        val batteryHealth: String = "",
        val accessories: List<String> = emptyList(),
        val repaired: String = "无拆修"
    ) : CategoryDetail()

    data class DailyDetail(
        val brand: String = "",
        val usageDuration: String = "",
        val flaw: String = "无瑕疵"
    ) : CategoryDetail()
}

@Entity(tableName = "goods")
@TypeConverters(Converters::class)
data class GoodsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val price: Double,
    val category: String, // "books", "digital", "daily", "group"
    val type: String,     // "secondhand" (二手) 或 "group" (拼单)
    val images: List<String> = emptyList(), // 多张本地/网络图片路径
    val description: String,
    val location: String,
    val sellerName: String,
    val sellerAvatar: String,
    val currentCount: Int = 1, // 拼单专有：已拼人数
    val maxCount: Int = 1,     // 拼单专有：人数上限
    val deadline: String = "", // 拼单专有：截止时间
    val isFavorite: Boolean = false,
    val extraInfoJson: String = "", // 存放分类专属 JSON 字符串
    val contactOverrideJson: String = "", // 存放联系方式 JSON 字符串
    val status: String = "AVAILABLE", // 在售 (AVAILABLE) / 已预定 (RESERVED) / 已售出 (SOLD)
    val createTime: Long = System.currentTimeMillis()
) {
    val primaryImage: String
        get() = images.firstOrNull() ?: ""

    val goodsStatus: GoodsStatus
        get() = when (status) {
            "RESERVED" -> GoodsStatus.RESERVED
            "SOLD" -> GoodsStatus.SOLD
            else -> GoodsStatus.AVAILABLE
        }
}
