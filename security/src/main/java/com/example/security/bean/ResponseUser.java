package com.example.security.bean;

import lombok.Data;

@Data
public class ResponseUser {

    private Long id;

    private String username;

    private String token;

    public ResponseUser(User user, String token) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.token = token;
    }
}
