package com.bhavya.C022_BhavyaSanghrajka;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insertMessage(Message message);

    @Query("SELECT * FROM messages ORDER BY id ASC")
    List<Message> getAllMessages();

    @Query("DELETE FROM messages")
    void clearMessages();
}
