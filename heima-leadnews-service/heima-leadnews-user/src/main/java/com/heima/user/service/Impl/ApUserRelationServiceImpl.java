package com.heima.user.service.Impl;

import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.UserRelationDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserRelationService;
import com.heima.user.thread.AppThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApUserRelationServiceImpl implements ApUserRelationService {

    @Autowired
    private CacheService cacheService;
    /**
     * 用户关注/取消关注
     * @param dto
     * @return
     */
    @Override
    public ResponseResult follow(UserRelationDto dto) {
        //1.参数检查
        if (dto == null ) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //2.获取当前用户
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.AP_USER_DATA_NOT_EXIST);
        }
        //3.关注/取消关注
        Integer id = user.getId();//当前用户id
        Integer authorId = dto.getAuthorId();//作者id
        Short operation = dto.getOperation();//关注/取消关注
        if (operation == 0) {
            //关注
            cacheService.zAdd(BehaviorConstants.APUSER_FOLLOW_RELATION + id, authorId.toString() , System.currentTimeMillis());
            cacheService.zAdd(BehaviorConstants.APUSER_FANS_RELATION + authorId, id.toString() , System.currentTimeMillis());
        } else if (operation == 1) {
            //取消关注
            cacheService.zRemove(BehaviorConstants.APUSER_FOLLOW_RELATION + id, authorId.toString());
            cacheService.zRemove(BehaviorConstants.APUSER_FANS_RELATION + authorId, id.toString());
        }
        //4.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
