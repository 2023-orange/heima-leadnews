package com.heima.article.service.impl;
import com.alibaba.fastjson.JSON;
import com.heima.article.service.ApCollectionService;
import com.heima.article.thread.AppThreadLocalUtil;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApCollectionServiceImpl implements ApCollectionService {

    @Autowired
    private CacheService cacheService;
    /**
     * 收藏
     * @param dto
     * @return
     */
    @Override
    public ResponseResult collection(CollectionBehaviorDto dto) {
        // 1.参数检查
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 2.获取用户信息
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer userId = user.getId();
        Short type = dto.getType();
        Long entryId = dto.getEntryId();
        // 收藏
        if (type == 0){
            //查询缓存
            Object obj = cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR + entryId, userId.toString());
            if (obj != null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "已收藏");
            }
            //添加缓存
            cacheService.hPut(BehaviorConstants.COLLECTION_BEHAVIOR + entryId, userId.toString(), JSON.toJSONString(dto));
        } else if (type == 1) {
            // 取消收藏
            cacheService.hDelete(BehaviorConstants.COLLECTION_BEHAVIOR + entryId, userId.toString());
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
