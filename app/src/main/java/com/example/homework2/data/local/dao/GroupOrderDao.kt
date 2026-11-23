package com.example.homework2.data.local.dao

import androidx.room.*
import com.example.homework2.data.local.entity.GroupOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupOrderDao {
    @Query("SELECT * FROM group_orders WHERE goodsId = :goodsId ORDER BY joinTime ASC")
    fun getMembersByGoodsId(goodsId: Long): Flow<List<GroupOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupOrderEntity)

    @Query("SELECT * FROM group_orders ORDER BY joinTime DESC")
    fun getAllGroupOrders(): Flow<List<GroupOrderEntity>>
}
