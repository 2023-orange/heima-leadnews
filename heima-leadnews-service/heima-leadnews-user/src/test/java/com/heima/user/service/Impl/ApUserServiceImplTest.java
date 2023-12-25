package com.heima.user.service.Impl;

import org.junit.Test;
import org.springframework.util.DigestUtils;

import static org.junit.Assert.*;

public class ApUserServiceImplTest {

    @Test
    public void login() {
        String salt = "sdsa";
        String password = "wangwu";
        String pswd = DigestUtils.md5DigestAsHex((password + salt).getBytes());
        System.out.println(pswd);
    }
}