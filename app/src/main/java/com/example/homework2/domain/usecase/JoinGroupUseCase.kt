package com.example.homework2.domain.usecase

import android.content.Context
import com.example.homework2.data.local.AppDatabase
import com.example.homework2.data.local.entity.GroupOrderEntity
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.MessageType
import com.example.homework2.util.NotificationHelper

class JoinGroupUseCase(private val database: AppDatabase) {

    suspend fun execute(
        context: Context,
        goodsId: Long,
        memberName: String,
        memberAvatar: String
    ): Pair<Boolean, MessageEntity?> {
        val goods = database.goodsDao().getGoodsById(goodsId) ?: return Pair(false, null)

        val affected = database.goodsDao().incrementGroupCount(goodsId)
        if (affected <= 0) return Pair(false, null)

        database.groupOrderDao().insertMember(
            GroupOrderEntity(
                goodsId = goodsId,
                memberName = memberName,
                memberAvatar = memberAvatar,
                isCreator = false
            )
        )

        val updatedGoods = database.goodsDao().getGoodsById(goodsId)
        var successMessage: MessageEntity? = null

        if (updatedGoods != null && updatedGoods.currentCount >= updatedGoods.maxCount) {
            val title = "🎉 拼单成功通告"
            val content = "恭喜！您参与的拼单《${goods.title}》已成功达到 ${goods.maxCount} 人组团！请按照约定时间地点分摊取货。"

            successMessage = MessageEntity(
                receiverId = 1,
                senderName = "拼单小助手",
                senderAvatar = "https://api.iconify.design/twemoji:party-popper.svg",
                type = MessageType.GROUP_SUCCESS.code,
                title = title,
                content = content,
                goodsId = goodsId,
                groupId = goodsId
            )
            database.messageDao().insertMessage(successMessage)

            NotificationHelper.sendNotification(
                context = context,
                title = title,
                content = content
            )
        }

        return Pair(true, successMessage)
    }
}
