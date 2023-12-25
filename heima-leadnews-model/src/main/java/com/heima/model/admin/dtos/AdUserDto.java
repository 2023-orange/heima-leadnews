package com.heima.model.admin.dtos;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AdUserDto {
    /**
     * 用户名
     */
    @ApiModelProperty(value = "用户名",required = true)
    private String name;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码",required = true)
    private String password;
}