package org.perso.mlt

import androidx.room.*

@Entity
data class Word(
    @PrimaryKey var uid: Int,
    @ColumnInfo(name = "english") var english: String?,
    @ColumnInfo(name = "spanish") var spanish: String?,
    @ColumnInfo(name = "french") var french: String?
    ) {}

@Dao
interface WordDao {
    @Query("SELECT * FROM word")
    fun getAll(): List<Word>

    @Query("SELECT * FROM word WHERE uid IN (:wordIds)")
    fun loadAllByIds(wordIds: IntArray): List<Word>

    @Query("SELECT * FROM word WHERE english LIKE :english LIMIT 1")
    fun findByEnglish(english: String): Word

    @Insert
    fun insertAll(vararg words: Word)

    @Query("DELETE FROM word")
    fun deleteAll()

    @Delete
    fun delete(word: Word)
}

@Database(entities = [Word::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}