package com.example.homework2.domain.usecase

import android.content.Context
import com.example.homework2.data.local.AppDatabase
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.MessageType
import com.example.homework2.util.NotificationHelper

class HandleOrderRequestUseCase(private val database: AppDatabase) {

    suspend fun execute(
        context: Context,
        orderRequestId: Long,
        isAccepted: Boolean
    ): MessageEntity? {
        val request = database.orderRequestDao().getOrderRequestById(orderRequestId) ?: return null
        val goods = database.goodsDao().getGoodsById(request.goodsId) ?: return null

        val newStatus = if (isAccepted) "ACCEPTED" else "REJECTED"
        database.orderRequestDao().updateOrderRequestStatus(orderRequestId, newStatus)

        if (isAccepted) {
            // 同意时，商品状态自动变为“已预定 (RESERVED)”
            database.goodsDao().updateGoodsStatus(goods.id, "RESERVED")
        }

        // 给买家发送结果通知消息
        val titleText = if (isAccepted) "✅ 卖家已同意您的购买申请" else "❌ 卖家拒绝了您的购买申请"
        val contentText = if (isAccepted) {
            "卖家【${goods.sellerName}】已同意您对《${goods.title}》的购买申请，商品已为您预定，请及时联系卖家进行交易。"
        } else {
            "抱歉，卖家【${goods.sellerName}】拒绝了您对《${goods.title}》的购买申请。"
        }

        val message = MessageEntity(
            receiverId = request.buyerId,
            senderId = 1,
            senderName = goods.sellerName,
            senderAvatar = goods.sellerAvatar,
            type = MessageType.ORDER_RESULT.code,
            title = titleText,
            content = contentText,
            goodsId = goods.id,
            orderRequestId = orderRequestId
        )
        database.messageDao().insertMessage(message)

        NotificationHelper.sendNotification(
            context = context,
            title = message.title,
            content = message.content
        )

        return message
    }
}
