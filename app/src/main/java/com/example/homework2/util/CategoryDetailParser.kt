package com.example.homework2.util

import com.example.homework2.data.local.entity.CategoryDetail
import org.json.JSONArray
import org.json.JSONObject

object CategoryDetailParser {

    fun toJson(detail: CategoryDetail?): String {
        if (detail == null) return ""
        val json = JSONObject()
        when (detail) {
            is CategoryDetail.BookDetail -> {
                json.put("type", "book")
                json.put("bookName", detail.bookName)
                json.put("author", detail.author)
                json.put("publisher", detail.publisher)
                json.put("edition", detail.edition)
                json.put("isbn", detail.isbn)
                json.put("course", detail.course)
                json.put("notes", detail.notes)
                json.put("damaged", detail.damaged)
                json.put("isLatestEdition", detail.isLatestEdition)
            }
            is CategoryDetail.DigitalDetail -> {
                json.put("type", "digital")
                json.put("brand", detail.brand)
                json.put("model", detail.model)
                json.put("purchaseDate", detail.purchaseDate)
                json.put("specs", detail.specs)
                json.put("warranty", detail.warranty)
                json.put("batteryHealth", detail.batteryHealth)
                json.put("repaired", detail.repaired)
                val accArray = JSONArray()
                detail.accessories.forEach { accArray.put(it) }
                json.put("accessories", accArray)
            }
            is CategoryDetail.DailyDetail -> {
                json.put("type", "daily")
                json.put("brand", detail.brand)
                json.put("usageDuration", detail.usageDuration)
                json.put("flaw", detail.flaw)
            }
        }
        return json.toString()
    }

    fun fromJson(jsonStr: String?): CategoryDetail? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val json = JSONObject(jsonStr)
            when (json.optString("type")) {
                "book" -> CategoryDetail.BookDetail(
                    bookName = json.optString("bookName"),
                    author = json.optString("author"),
                    publisher = json.optString("publisher"),
                    edition = json.optString("edition"),
                    isbn = json.optString("isbn"),
                    course = json.optString("course"),
                    notes = json.optString("notes", "无笔记"),
                    damaged = json.optString("damaged", "无破损"),
                    isLatestEdition = json.optBoolean("isLatestEdition", false)
                )
                "digital" -> {
                    val accList = mutableListOf<String>()
                    val accArray = json.optJSONArray("accessories")
                    if (accArray != null) {
                        for (i in 0 until accArray.length()) {
                            accList.add(accArray.getString(i))
                        }
                    }
                    CategoryDetail.DigitalDetail(
                        brand = json.optString("brand"),
                        model = json.optString("model"),
                        purchaseDate = json.optString("purchaseDate"),
                        specs = json.optString("specs"),
                        warranty = json.optString("warranty", "在保"),
                        batteryHealth = json.optString("batteryHealth"),
                        accessories = accList,
                        repaired = json.optString("repaired", "无拆修")
                    )
                }
                "daily" -> CategoryDetail.DailyDetail(
                    brand = json.optString("brand"),
                    usageDuration = json.optString("usageDuration"),
                    flaw = json.optString("flaw", "无瑕疵")
                )
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
