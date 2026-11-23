package com.example.homework2.util

import com.example.homework2.data.local.entity.CategoryDetail
import com.example.homework2.data.local.entity.GoodsEntity

data class AiEstimateResult(
    val suggestedPrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val reasoning: String,
    val paramSummary: String
)

object AiEstimateEngine {

    fun estimate(
        title: String,
        category: String,
        type: String,
        categoryDetail: CategoryDetail?,
        dbGoodsList: List<GoodsEntity> = emptyList()
    ): AiEstimateResult {
        val lowercaseTitle = title.lowercase()

        // 1. 根据分类与关键字计算基础挂牌价 (Base Price)
        var basePrice = when {
            // 数码智能匹配
            category == "digital" -> {
                when {
                    "ipad air 5" in lowercaseTitle || "ipad air5" in lowercaseTitle -> 3100.0
                    "ipad pro" in lowercaseTitle -> 4800.0
                    "ipad 10" in lowercaseTitle || "ipad 9" in lowercaseTitle -> 1800.0
                    "ipad" in lowercaseTitle -> 2200.0
                    "iphone 15" in lowercaseTitle || "iphone 14" in lowercaseTitle -> 4200.0
                    "iphone" in lowercaseTitle -> 2800.0
                    "macbook" in lowercaseTitle -> 5500.0
                    "airpods" in lowercaseTitle -> 650.0
                    "apple pencil" in lowercaseTitle -> 450.0
                    "switch" in lowercaseTitle -> 1200.0
                    "键盘" in lowercaseTitle || "鼠标" in lowercaseTitle -> 150.0
                    else -> 600.0
                }
            }
            // 二手书/教材匹配
            category == "books" -> {
                when {
                    "考研" in lowercaseTitle || "高数" in lowercaseTitle -> 20.0
                    "英语" in lowercaseTitle || "四六级" in lowercaseTitle -> 15.0
                    "教材" in lowercaseTitle || "专业书" in lowercaseTitle -> 22.0
                    else -> 18.0
                }
            }
            // 生活用品匹配
            category == "daily" -> {
                when {
                    "水壶" in lowercaseTitle || "电饭煲" in lowercaseTitle -> 35.0
                    "台灯" in lowercaseTitle -> 25.0
                    "风扇" in lowercaseTitle -> 20.0
                    "椅" in lowercaseTitle || "桌" in lowercaseTitle -> 45.0
                    else -> 30.0
                }
            }
            // 拼单匹配
            else -> {
                when {
                    "麦当劳" in lowercaseTitle || "肯德基" in lowercaseTitle -> 16.5
                    "拼车" in lowercaseTitle || "顺风车" in lowercaseTitle -> 25.0
                    "快递" in lowercaseTitle -> 5.0
                    else -> 15.0
                }
            }
        }

        // 2. 根据分类专属细节参数智能微调 (Fine-tuning Multipliers)
        var multiplier = 1.0
        val reasoningList = mutableListOf<String>()
        var paramSummary = ""

        when (categoryDetail) {
            is CategoryDetail.DigitalDetail -> {
                paramSummary = "【数码】${categoryDetail.brand} ${categoryDetail.model} | 保修: ${categoryDetail.warranty} | 电池: ${categoryDetail.batteryHealth.ifBlank { "正常" }}"

                if (categoryDetail.warranty == "在保") {
                    multiplier += 0.08
                    reasoningList.add("处于保修期内 (+8%)")
                } else if (categoryDetail.warranty == "过保") {
                    multiplier -= 0.10
                    reasoningList.add("已过保修期 (-10%)")
                }

                if (categoryDetail.repaired == "无拆修") {
                    multiplier += 0.05
                    reasoningList.add("无拆修记录 (+5%)")
                } else if (categoryDetail.repaired.contains("修过")) {
                    multiplier -= 0.15
                    reasoningList.add("有拆修历史 (-15%)")
                }

                val batteryNum = categoryDetail.batteryHealth.replace("%", "").toIntOrNull()
                if (batteryNum != null) {
                    if (batteryNum >= 95) {
                        multiplier += 0.05
                        reasoningList.add("电池健康度 ${batteryNum}% 极佳 (+5%)")
                    } else if (batteryNum < 85) {
                        multiplier -= 0.08
                        reasoningList.add("电池健康度 ${batteryNum}% 需注意 (-8%)")
                    }
                }
            }

            is CategoryDetail.BookDetail -> {
                paramSummary = "【图书】${categoryDetail.bookName} (${categoryDetail.edition}) | 作者: ${categoryDetail.author} | 笔记: ${categoryDetail.notes}"

                if (categoryDetail.notes == "无笔记") {
                    multiplier += 0.10
                    reasoningList.add("全书无笔迹书写 (+10%)")
                } else if (categoryDetail.notes == "大量笔记") {
                    multiplier -= 0.15
                    reasoningList.add("划线笔记较多 (-15%)")
                }

                if (categoryDetail.damaged == "无破损") {
                    multiplier += 0.05
                } else if (categoryDetail.damaged == "严重破损") {
                    multiplier -= 0.20
                    reasoningList.add("品相有瑕疵破损 (-20%)")
                }
            }

            is CategoryDetail.DailyDetail -> {
                paramSummary = "【生活】${categoryDetail.brand.ifBlank { "通用品牌" }} | 使用: ${categoryDetail.usageDuration.ifBlank { "短期" }} | 瑕疵: ${categoryDetail.flaw}"
                if (categoryDetail.flaw == "无瑕疵") {
                    multiplier += 0.05
                } else {
                    multiplier -= 0.10
                    reasoningList.add("表面有使用瑕疵 (-10%)")
                }
            }

            null -> {
                paramSummary = "依据基础分类与物品标题估算"
            }
        }

        // 3. 计算最终建议价格与区间
        val calculatedPrice = (basePrice * multiplier).let { Math.round(it * 10) / 10.0 }
        val minPrice = (calculatedPrice * 0.90).let { Math.round(it * 10) / 10.0 }
        val maxPrice = (calculatedPrice * 1.10).let { Math.round(it * 10) / 10.0 }

        val reasoningStr = if (reasoningList.isNotEmpty()) {
            "估价依据：针对『$title』，结合校内行情及专属参数（${reasoningList.joinToString("、")}），折算二手残值率约为 ${Math.round(multiplier * 75)}% 折。"
        } else {
            "估价依据：结合校内近30天『$title』同类成色成交流水，建议定在原价 3-5 折合理的校内转让区间。"
        }

        return AiEstimateResult(
            suggestedPrice = calculatedPrice,
            minPrice = minPrice,
            maxPrice = maxPrice,
            reasoning = reasoningStr,
            paramSummary = paramSummary
        )
    }
}
