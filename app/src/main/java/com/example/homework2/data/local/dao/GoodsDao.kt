package com.example.homework2.data.local.dao

import androidx.room.*
import com.example.homework2.data.local.entity.GoodsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoodsDao {
    @Query("SELECT * FROM goods ORDER BY createTime DESC")
    fun getAllGoods(): Flow<List<GoodsEntity>>

    @Query("SELECT * FROM goods WHERE id = :id")
    suspend fun getGoodsById(id: Long): GoodsEntity?

    @Query("SELECT * FROM goods WHERE category = :category ORDER BY createTime DESC")
    fun getGoodsByCategory(category: String): Flow<List<GoodsEntity>>

    @Query("SELECT * FROM goods WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR extraInfoJson LIKE '%' || :query || '%' ORDER BY createTime DESC")
    fun searchGoods(query: String): Flow<List<GoodsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoods(goods: GoodsEntity): Long

    @Update
    suspend fun updateGoods(goods: GoodsEntity)

    @Delete
    suspend fun deleteGoods(goods: GoodsEntity)

    @Query("UPDATE goods SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFav: Boolean)

    @Query("UPDATE goods SET status = :status WHERE id = :id")
    suspend fun updateGoodsStatus(id: Long, status: String)

    @Query("UPDATE goods SET currentCount = currentCount + 1 WHERE id = :id AND currentCount < maxCount")
    suspend fun incrementGroupCount(id: Long): Int
}
