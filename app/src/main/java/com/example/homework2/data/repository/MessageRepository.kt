package com.example.homework2.data.repository

import com.example.homework2.data.local.dao.MessageDao
import com.example.homework2.data.local.dao.OrderRequestDao
import com.example.homework2.data.local.entity.MessageEntity
import com.example.homework2.data.local.entity.OrderRequestEntity
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessagesForUser(userId: Long = 1): Flow<List<MessageEntity>>
    fun getUnreadCount(userId: Long = 1): Flow<Int>
    suspend fun insertMessage(message: MessageEntity): Long
    suspend fun markAsRead(id: Long)
    suspend fun markAllAsRead(userId: Long = 1)
    suspend fun deleteMessage(id: Long)
    suspend fun deleteAllMessages(userId: Long = 1)

    // 订单请求方法
    suspend fun getOrderRequestById(id: Long): OrderRequestEntity?
    fun getOrderRequestByIdFlow(id: Long): Flow<OrderRequestEntity?>
    suspend fun insertOrderRequest(request: OrderRequestEntity): Long
    suspend fun updateOrderRequestStatus(id: Long, status: String)
}

class LocalMessageRepository(
    private val messageDao: MessageDao,
    private val orderRequestDao: OrderRequestDao
) : MessageRepository {

    override fun getMessagesForUser(userId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForUser(userId)
    }

    override fun getUnreadCount(userId: Long): Flow<Int> {
        return messageDao.getUnreadCount(userId)
    }

    override suspend fun insertMessage(message: MessageEntity): Long {
        return messageDao.insertMessage(message)
    }

    override suspend fun markAsRead(id: Long) {
        messageDao.markAsRead(id)
    }

    override suspend fun markAllAsRead(userId: Long) {
        messageDao.markAllAsRead(userId)
    }

    override suspend fun deleteMessage(id: Long) {
        messageDao.deleteMessage(id)
    }

    override suspend fun deleteAllMessages(userId: Long) {
        messageDao.deleteAllMessages(userId)
    }

    override suspend fun getOrderRequestById(id: Long): OrderRequestEntity? {
        return orderRequestDao.getOrderRequestById(id)
    }

    override fun getOrderRequestByIdFlow(id: Long): Flow<OrderRequestEntity?> {
        return orderRequestDao.getOrderRequestByIdFlow(id)
    }

    override suspend fun insertOrderRequest(request: OrderRequestEntity): Long {
        return orderRequestDao.insertOrderRequest(request)
    }

    override suspend fun updateOrderRequestStatus(id: Long, status: String) {
        orderRequestDao.updateOrderRequestStatus(id, status)
    }
}
