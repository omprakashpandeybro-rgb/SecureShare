package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
  @Query("SELECT * FROM transfer_records ORDER BY timestamp DESC")
  fun getAllTransfers(): Flow<List<TransferRecord>>

  @Query("SELECT * FROM transfer_records WHERE direction = :direction ORDER BY timestamp DESC")
  fun getTransfersByDirection(direction: String): Flow<List<TransferRecord>>

  @Query("SELECT * FROM transfer_records WHERE id = :id")
  suspend fun getTransferById(id: Long): TransferRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransfer(record: TransferRecord): Long

  @Update
  suspend fun updateTransfer(record: TransferRecord)

  @Delete
  suspend fun deleteTransfer(record: TransferRecord)

  @Query("DELETE FROM transfer_records")
  suspend fun clearAll()
}
