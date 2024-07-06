package com.malopieds.innertune.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "album")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val themeColor: Int? = null,
    val songCount: Int,
    val duration: Int,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null,
) {
    fun toggleLike() =
        copy(
            bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now(),
        )
}
