package com.heima.behavior.service.Impl;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.ApUnlikesBehaviorService;
import com.heima.behavior.thread.AppThreadLocalUtil;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApUnlikesBehaviorServiceImpl implements ApUnlikesBehaviorService {

    @Autowired
    private CacheService cacheService;
    /**
     * 不喜欢
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult unLike(UnLikesBehaviorDto dto) {
        //1.参数检查
        if (dto.getArticleId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.获取用户信息
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //3.更改行为
        if (dto.getType() == 0) {
            //点击不喜欢
            cacheService.hPut(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId().toString(), user.getId().toString(), JSON.toJSONString(dto));
            //取消不喜欢
        } else{
            cacheService.hDelete(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId().toString(), user.getId().toString());
        }
        //4.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}

