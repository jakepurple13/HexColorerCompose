package com.programmersbox.hexcolorercompose

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(entities = [ColorItem::class], version = 1)
abstract class ColorDatabase : RoomDatabase() {

    abstract fun colorDao(): ColorDao

    companion object {

        @Volatile
        private var INSTANCE: ColorDatabase? = null

        fun getInstance(context: Context): ColorDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: buildDatabase(context).also { INSTANCE = it } }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, ColorDatabase::class.java, "color.db")
                .build()
    }

}

@Dao
interface ColorDao {

    @Query("SELECT * FROM Color ORDER BY time ASC")
    fun getAllColors(): Flow<List<ColorItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColor(model: ColorItem)

    @Delete
    suspend fun deleteColor(model: ColorItem)

}

@Entity(tableName = "Color")
data class ColorItem(
    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis(),
    @PrimaryKey
    @ColumnInfo(name = "color")
    val color: String
)