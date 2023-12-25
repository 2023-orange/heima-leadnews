package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;

public interface WmSensitiveService extends IService<WmSensitive>{

    /**
     * 新增敏感词
     * @param wmSensitive
     * @return
     */
    ResponseResult insert(WmSensitive wmSensitive);

    /**
     * 查询敏感词列表
     * @param dto
     * @return
     */
    ResponseResult list(SensitiveDto dto);

    /**
     * 修改敏感词
     * @param wmSensitive
     * @return
     */
    ResponseResult update(WmSensitive wmSensitive);

    /**
     * 删除敏感词
     * @param id
     * @return
     */
    ResponseResult delete(Integer id);
}
