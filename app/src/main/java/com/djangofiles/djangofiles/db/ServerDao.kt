package com.djangofiles.djangofiles.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert


@Dao
interface ServerDao {
    @Query("SELECT * FROM server")
    fun getAll(): List<Server>

    @Query("SELECT * FROM server WHERE active = 1 LIMIT 1")
    fun getActive(): Server?

    @Query("SELECT * FROM server WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): Server?

    @Query("UPDATE server SET token = :token WHERE url = :url")
    fun setToken(url: String, token: String)

    @Insert
    fun add(server: Server)

    @Upsert
    fun addOrUpdate(server: Server)

    @Query("UPDATE server SET active = 1 WHERE url = :url")
    fun activate(url: String)

    @Delete
    fun delete(server: Server)
}


@Entity
data class Server(
    @PrimaryKey val url: String,
    val token: String = "",
    val active: Boolean = false,
    val size: Long? = null,
    val count: Int? = null,
    val shorts: Int? = null,
    val humanSize: String? = null,
)


@Database(entities = [Server::class], version = 2)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        @Volatile
        private var instance: ServerDatabase? = null

        fun getInstance(context: Context): ServerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ServerDatabase::class.java,
                    "server-database"
                ).build().also { instance = it }
            }
    }
}
