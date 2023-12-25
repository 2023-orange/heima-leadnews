package com.heima.wemedia.client;

import com.heima.apis.user.IWmUserClient;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.wemedia.service.WmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("wemedia/user")
public class WmUserClient implements IWmUserClient {

    @Autowired
    private WmUserService wmUserService;

    @PostMapping("/add")
    @Override
    public ResponseResult addUser(@RequestBody Map<String, String> map) {
        String name = map.get("name");
        return wmUserService.addUser(name);
    }
}
