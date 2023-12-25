package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUserRealname;


public interface ApUserRealNameService extends IService<ApUserRealname> {

    /**
     * 查询用户认证列表
     * @param dto
     * @return
     */
    ResponseResult list(AuthDto dto);

    /**
     * 通过/不通过
     * @param dto  审核信息
     * @param type 1通过 2不通过
     * @return
     */
    ResponseResult auth(AuthDto dto, Integer type);
}
