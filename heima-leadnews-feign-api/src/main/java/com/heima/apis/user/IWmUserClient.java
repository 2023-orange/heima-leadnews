package com.heima.apis.user;

import com.heima.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "leadnews-wemedia",contextId = "userClient")
public interface IWmUserClient {

    @PostMapping("wemedia/user/add")
    ResponseResult addUser(@RequestBody Map<String,String> params);
}
