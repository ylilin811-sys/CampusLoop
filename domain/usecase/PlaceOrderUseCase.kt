package com.example.homework2.domain.usecase

import android.content.Context
import com.example.homework2.data.local.AppDatabase
import com.example.homework2.data.local.entity.GoodsEntity
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.MessageType
import com.example.homework2.data.local.entity.OrderRequestEntity
import com.example.homework2.util.NotificationHelper

class PlaceOrderUseCase(private val database: AppDatabase) {

    suspend fun execute(
        context: Context,
        goods: GoodsEntity,
        buyerId: Long = 2,
        buyerName: String,
        buyerAvatar: String
    ): MessageEntity {
        var requestId = 0L

        // 1. 创建订单申请记录
        val request = OrderRequestEntity(
            goodsId = goods.id,
            buyerId = buyerId,
            buyerName = buyerName,
            buyerAvatar = buyerAvatar,
            sellerId = 1,
            status = "PENDING"
        )
        requestId = database.orderRequestDao().insertOrderRequest(request)

        // 2. 给卖家发送下单请求消息
        val message = MessageEntity(
            receiverId = 1, // 卖家接收
            senderId = buyerId,
            senderName = buyerName,
            senderAvatar = buyerAvatar,
            type = MessageType.ORDER_REQUEST.code,
            title = "📦 收到新的求购申请",
            content = "买家【$buyerName】想要您的商品《${goods.title}》，请及时处理。",
            goodsId = goods.id,
            orderRequestId = requestId
        )
        database.messageDao().insertMessage(message)

        // 3. 发送本地系统通知
        NotificationHelper.sendNotification(
            context = context,
            title = message.title,
            content = message.content
        )

        return message
    }
}
