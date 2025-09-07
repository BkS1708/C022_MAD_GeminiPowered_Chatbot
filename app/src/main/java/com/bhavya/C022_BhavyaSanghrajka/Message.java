package com.bhavya.C022_BhavyaSanghrajka;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "messages")
public class Message {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String text;
    private boolean isUser; // true = user, false = bot

    @Ignore
    private boolean isTyping;

    // Constructor
    public Message(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.isTyping = false;
    }

    public Message(String text, boolean isUser, boolean isTyping) {
        this.text = text;
        this.isUser = isUser;
        this.isTyping = isTyping;
    }

    // Empty constructor required by Room
    public Message() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }

    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }
}
