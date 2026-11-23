package com.example.homework2.data.local.dao

import androidx.room.*
import com.example.homework2.data.local.entity.FavoriteEntity
import com.example.homework2.data.local.entity.GoodsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE goodsId = :goodsId")
    suspend fun deleteFavoriteByGoodsId(goodsId: Long)

    @Query("SELECT * FROM goods WHERE isFavorite = 1 ORDER BY createTime DESC")
    fun getFavoriteGoods(): Flow<List<GoodsEntity>>
}
