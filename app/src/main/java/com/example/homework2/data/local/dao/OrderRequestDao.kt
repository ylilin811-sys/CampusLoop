package com.example.homework2.data.local.dao

import androidx.room.*
import com.example.homework2.data.local.entity.OrderRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderRequestDao {
    @Query("SELECT * FROM order_requests WHERE id = :id")
    suspend fun getOrderRequestById(id: Long): OrderRequestEntity?

    @Query("SELECT * FROM order_requests WHERE id = :id")
    fun getOrderRequestByIdFlow(id: Long): Flow<OrderRequestEntity?>

    @Query("SELECT * FROM order_requests WHERE goodsId = :goodsId ORDER BY createdAt DESC")
    fun getOrderRequestsByGoodsId(goodsId: Long): Flow<List<OrderRequestEntity>>

    @Query("SELECT * FROM order_requests WHERE goodsId = :goodsId AND status = 'PENDING' AND id != :currentRequestId")
    suspend fun getOtherPendingRequests(goodsId: Long, currentRequestId: Long): List<OrderRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderRequest(request: OrderRequestEntity): Long

    @Query("UPDATE order_requests SET status = :status WHERE id = :id")
    suspend fun updateOrderRequestStatus(id: Long, status: String)

    @Query("UPDATE order_requests SET status = 'REJECTED' WHERE goodsId = :goodsId AND status = 'PENDING' AND id != :currentRequestId")
    suspend fun rejectOtherPendingRequests(goodsId: Long, currentRequestId: Long)
}
