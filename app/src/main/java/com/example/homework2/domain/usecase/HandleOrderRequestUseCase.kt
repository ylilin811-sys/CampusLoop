package com.example.homework2.domain.usecase

import android.content.Context
import com.example.homework2.data.local.AppDatabase
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.MessageType
import com.example.homework2.util.NotificationHelper

sealed class OrderRequestResult {
    object SuccessAccepted : OrderRequestResult()
    object SuccessRejected : OrderRequestResult()
    object AlreadyProcessed : OrderRequestResult()
    object GoodsAlreadyReserved : OrderRequestResult()
}

class HandleOrderRequestUseCase(private val database: AppDatabase) {

    suspend fun execute(
        context: Context,
        orderRequestId: Long,
        isAccepted: Boolean
    ): OrderRequestResult {
        // 1. 读取订单请求当前状态 (FSM 状态机检查)
        val request = database.orderRequestDao().getOrderRequestById(orderRequestId)
            ?: return OrderRequestResult.AlreadyProcessed

        // 仅在“待处理 (PENDING)”状态下方可继续执行，否则直接拦截
        if (request.status != "PENDING") {
            return OrderRequestResult.AlreadyProcessed
        }

        val goods = database.goodsDao().getGoodsById(request.goodsId)
            ?: return OrderRequestResult.AlreadyProcessed

        // 2. 如果卖家点击“同意”
        if (isAccepted) {
            // 校验商品是否已被预定或售出
            if (goods.status == "RESERVED" || goods.status == "SOLD") {
                return OrderRequestResult.GoodsAlreadyReserved
            }

            // 在同一个事务中更新业务数据并生成消息
            database.orderRequestDao().updateOrderRequestStatus(orderRequestId, "ACCEPTED")
            database.goodsDao().updateGoodsStatus(goods.id, "RESERVED")

            // 给买家发送同意通知
            val titleText = "✅ 卖家已同意您的购买申请"
            val contentText = "卖家【${goods.sellerName}】已同意您对《${goods.title}》的购买申请，商品已为您预定，请及时联系卖家进行交易。"

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

            // 规则：同意某个买家后，同一商品下其他“待处理”的请求自动改为“已拒绝”，并通知对应买家
            val otherPendingList = database.orderRequestDao().getOtherPendingRequests(goods.id, orderRequestId)
            if (otherPendingList.isNotEmpty()) {
                database.orderRequestDao().rejectOtherPendingRequests(goods.id, orderRequestId)
                otherPendingList.forEach { otherReq ->
                    val otherMsg = MessageEntity(
                        receiverId = otherReq.buyerId,
                        senderId = 1,
                        senderName = goods.sellerName,
                        senderAvatar = goods.sellerAvatar,
                        type = MessageType.ORDER_RESULT.code,
                        title = "❌ 购买申请已取消",
                        content = "抱歉，您求购的商品《${goods.title}》已被其他学友预定，您的购买申请已自动取消。",
                        goodsId = goods.id,
                        orderRequestId = otherReq.id
                    )
                    database.messageDao().insertMessage(otherMsg)
                }
            }

            return OrderRequestResult.SuccessAccepted
        } else {
            // 3. 如果卖家点击“拒绝”
            database.orderRequestDao().updateOrderRequestStatus(orderRequestId, "REJECTED")

            val titleText = "❌ 卖家拒绝了您的购买申请"
            val contentText = "抱歉，卖家【${goods.sellerName}】拒绝了您对《${goods.title}》的购买申请。"

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

            return OrderRequestResult.SuccessRejected
        }
    }
}
