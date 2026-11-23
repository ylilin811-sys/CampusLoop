package com.example.homework2.data.repository

import com.example.homework2.R
import com.example.homework2.data.local.dao.ActivityDao
import com.example.homework2.data.local.dao.FavoriteDao
import com.example.homework2.data.local.dao.GoodsDao
import com.example.homework2.data.local.dao.GroupOrderDao
import com.example.homework2.data.local.dao.UserDao
import com.example.homework2.data.local.entity.ActivityEntity
import com.example.homework2.data.local.entity.FavoriteEntity
import com.example.homework2.data.local.entity.GoodsEntity
import com.example.homework2.data.local.entity.GroupOrderEntity
import com.example.homework2.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MarketRepository(
    private val goodsDao: GoodsDao,
    private val groupOrderDao: GroupOrderDao,
    private val favoriteDao: FavoriteDao,
    private val userDao: UserDao,
    private val activityDao: ActivityDao
) {
    private val defaultActivities = listOf(
        ActivityEntity(
            titleRes = R.string.banner_graduation_title,
            subtitleRes = R.string.banner_graduation_subtitle,
            categoryTarget = "graduation"
        ),
        ActivityEntity(
            titleRes = R.string.banner_group_title,
            subtitleRes = R.string.banner_group_subtitle,
            categoryTarget = "group"
        ),
        ActivityEntity(
            titleRes = R.string.banner_hot_title,
            subtitleRes = R.string.banner_hot_subtitle,
            categoryTarget = "hot"
        ),
        ActivityEntity(
            titleRes = R.string.banner_textbook_title,
            subtitleRes = R.string.banner_textbook_subtitle,
            categoryTarget = "books"
        )
    )

    fun getCurrentUser(): Flow<UserEntity?> = userDao.getUserById(1)

    fun getAllActivities(): Flow<List<ActivityEntity>> = activityDao.getAllActivities().map { list ->
        if (list.isEmpty()) defaultActivities else list
    }

    suspend fun updateUserProfile(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun checkNicknameExists(nickname: String): Boolean {
        return userDao.checkNicknameExists(nickname, currentUserId = 1) > 0
    }

    fun getAllGoods(): Flow<List<GoodsEntity>> = goodsDao.getAllGoods()

    fun getGoodsByCategory(category: String): Flow<List<GoodsEntity>> {
        return if (category == "all") goodsDao.getAllGoods() else goodsDao.getGoodsByCategory(category)
    }

    fun searchGoods(query: String): Flow<List<GoodsEntity>> = goodsDao.searchGoods(query)

    suspend fun getGoodsById(id: Long): GoodsEntity? = goodsDao.getGoodsById(id)

    suspend fun insertGoods(goods: GoodsEntity): Long {
        val id = goodsDao.insertGoods(goods)
        if (goods.type == "group") {
            groupOrderDao.insertMember(
                GroupOrderEntity(
                    goodsId = id,
                    memberName = goods.sellerName,
                    memberAvatar = goods.sellerAvatar,
                    isCreator = true
                )
            )
        }
        return id
    }

    suspend fun updateGoodsStatus(id: Long, status: String) {
        goodsDao.updateGoodsStatus(id, status)
    }

    suspend fun deleteGoods(goods: GoodsEntity) {
        goodsDao.deleteGoods(goods)
        favoriteDao.deleteFavoriteByGoodsId(goods.id)
    }

    suspend fun toggleFavorite(goods: GoodsEntity) {
        val newStatus = !goods.isFavorite
        goodsDao.updateFavoriteStatus(goods.id, newStatus)
        if (newStatus) {
            favoriteDao.insertFavorite(FavoriteEntity(goodsId = goods.id))
        } else {
            favoriteDao.deleteFavoriteByGoodsId(goods.id)
        }
    }

    fun getFavoriteGoods(): Flow<List<GoodsEntity>> = favoriteDao.getFavoriteGoods()

    fun getGroupMembers(goodsId: Long): Flow<List<GroupOrderEntity>> = groupOrderDao.getMembersByGoodsId(goodsId)

    suspend fun joinGroupOrder(goodsId: Long, memberName: String, memberAvatar: String): Boolean {
        val affected = goodsDao.incrementGroupCount(goodsId)
        if (affected > 0) {
            groupOrderDao.insertMember(
                GroupOrderEntity(
                    goodsId = goodsId,
                    memberName = memberName,
                    memberAvatar = memberAvatar,
                    isCreator = false
                )
            )
            return true
        }
        return false
    }
}
