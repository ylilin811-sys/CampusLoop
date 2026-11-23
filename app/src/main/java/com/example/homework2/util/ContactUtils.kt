package com.example.homework2.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.json.JSONObject

data class ContactInfo(
    val wechatId: String = "",
    val qqNumber: String = "",
    val isPublic: Boolean = true
)

object ContactUtils {

    /**
     * 将打码格式转换为星号，例如 "wx_123456" -> "wx_1****6"
     */
    fun maskContact(text: String): String {
        if (text.length <= 3) return "***"
        val prefixLen = (text.length / 3).coerceAtLeast(1)
        val suffixLen = (text.length / 4).coerceAtLeast(1)
        val middleLen = text.length - prefixLen - suffixLen
        val stars = "*".repeat(middleLen.coerceAtLeast(2))
        return text.take(prefixLen) + stars + text.takeLast(suffixLen)
    }

    /**
     * 将 ContactInfo 序列化为 JSON 字符串
     */
    fun toJson(contactInfo: ContactInfo): String {
        val json = JSONObject()
        json.put("wechatId", contactInfo.wechatId)
        json.put("qqNumber", contactInfo.qqNumber)
        json.put("isPublic", contactInfo.isPublic)
        return json.toString()
    }

    /**
     * 从 JSON 字符串解析 ContactInfo
     */
    fun fromJson(jsonStr: String?): ContactInfo {
        if (jsonStr.isNullOrBlank()) return ContactInfo()
        return try {
            val json = JSONObject(jsonStr)
            ContactInfo(
                wechatId = json.optString("wechatId"),
                qqNumber = json.optString("qqNumber"),
                isPublic = json.optBoolean("isPublic", true)
            )
        } catch (e: Exception) {
            ContactInfo()
        }
    }

    /**
     * 校验微信号: 6–20位，字母开头，仅含字母、数字、下划线、减号
     */
    fun isValidWechatId(wechat: String): Boolean {
        if (wechat.isBlank()) return true
        val regex = Regex("^[a-zA-Z][a-zA-Z0-9_-]{5,19}$")
        return regex.matches(wechat)
    }

    /**
     * 校验 QQ 号: 5–11位纯数字
     */
    fun isValidQqNumber(qq: String): Boolean {
        if (qq.isBlank()) return true
        val regex = Regex("^[1-9][0-9]{4,10}$")
        return regex.matches(qq)
    }

    /**
     * 复制文本到系统剪贴板
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制 $label 到剪贴板", Toast.LENGTH_SHORT).show()
    }

    /**
     * 唤起 QQ 聊天 Intent (mqqwpa://im/chat?chat_type=wpa&uin=号码)
     */
    fun launchQq(context: Context, qqNumber: String) {
        try {
            val url = "mqqwpa://im/chat?chat_type=wpa&uin=$qqNumber"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            copyToClipboard(context, "QQ号", qqNumber)
            Toast.makeText(context, "未检测到已安装的 QQ，号码已复制到剪贴板", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 打开微信应用
     */
    fun launchWechat(context: Context, wechatId: String) {
        try {
            copyToClipboard(context, "微信号", wechatId)
            val intent = context.packageManager.getLaunchIntentForPackage("com.tencent.mm")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "微信号已复制，请在微信搜索添加", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "微信号已复制，请打开微信添加好友", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            copyToClipboard(context, "微信号", wechatId)
            Toast.makeText(context, "微信号已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }
}
