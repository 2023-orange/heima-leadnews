package com.heima.behavior.service;

import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

/**
 * <p>
 * APP不喜欢行为
 * </p>
 */
public interface ApUnlikesBehaviorService {

    /**
     * 不喜欢
     * @param dto
     * @return
     */
    ResponseResult unLike(UnLikesBehaviorDto dto);

}