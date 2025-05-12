package com.djangofiles.djangofiles.db

import android.content.Context
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.djangofiles.djangofiles.ServerApi.AlbumData
import java.util.Base64


@Dao
interface AlbumDao {
    @Query("SELECT * FROM albumentity")
    fun getAll(): List<AlbumEntity>

    @Query("SELECT * FROM albumentity WHERE name = :name LIMIT 1")
    fun getByName(name: String): AlbumEntity?

    @Insert
    fun add(album: AlbumEntity)

    @Upsert
    fun addOrUpdate(album: AlbumEntity)

    @Delete
    fun delete(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM AlbumEntity WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<Int>)

    suspend fun syncAlbums(albumDataList: List<AlbumData>) {
        val entities = albumDataList.map {
            AlbumEntity(
                id = it.id,
                name = it.name,
                password = it.password,
                private = it.private,
                info = it.info,
                expr = it.expr,
                date = it.date,
                url = it.url
            )
        }
        val newIds = entities.map { it.id }
        insertAll(entities)
        deleteMissing(newIds)
    }
}


@Entity
data class AlbumEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val password: String,
    val private: Boolean,
    val info: String,
    val expr: String,
    val date: String,
    val url: String
)


@Database(entities = [AlbumEntity::class], version = 1)
abstract class AlbumDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao

    companion object {
        private val instances = mutableMapOf<String, AlbumDatabase>()

        fun getInstance(context: Context, url: String): AlbumDatabase {
            val safeName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(url.toByteArray(Charsets.UTF_8))
            return instances[safeName] ?: synchronized(this) {
                instances[safeName] ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlbumDatabase::class.java,
                    "album-$safeName"
                ).build().also { instances[safeName] = it }
            }
        }
    }
}
