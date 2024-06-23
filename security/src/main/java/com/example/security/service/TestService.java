package com.example.security.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    public void test1() {
        System.out.println("test1");
    }

    @PreAuthorize("hasAuthority('/test')")
    public void test2() {
        System.out.println("test2");
    }

    @PreAuthorize("hasAuthority('/test3')")
    public void test3() {
        System.out.println("test3");
    }
}
