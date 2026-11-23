package com.example.homework2.data.local.dao

import androidx.room.*
import com.example.homework2.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Long = 1): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users WHERE nickname = :nickname AND id != :currentUserId")
    suspend fun checkNicknameExists(nickname: String, currentUserId: Long = 1): Int
}
