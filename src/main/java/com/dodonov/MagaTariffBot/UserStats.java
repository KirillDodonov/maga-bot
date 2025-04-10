package com.dodonov.MagaTariffBot;

public class UserStats {
    private final Long userId;
    private String username;
    private String firstName;
    private int messageCount;
    private int characterCount;

    public UserStats(Long userId, String username, String firstName, int messageCount, int characterCount) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.messageCount = messageCount;
        this.characterCount = characterCount;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(int characterCount) {
        this.characterCount = characterCount;
    }

    public void incrementMessageCount() {
        this.messageCount++;
    }

    public void addCharacters(int count) {
        this.characterCount += count;
    }
}
