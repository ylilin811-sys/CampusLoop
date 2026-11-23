package com.example.homework2.data.local.entity

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val categoryTarget: String
)
