package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;

public interface WmChannelService extends IService<WmChannel> {

    /**
     * 查询所有频道
     * @return
     */
    public ResponseResult findAll();

    /**
     * 查询
     * @param dto
     * @return
     */
    ResponseResult findByNameAndPage(ChannelDto dto);

    /**
     * 保存
     * @param wmChammel
     * @return
     */
    ResponseResult insert(WmChannel wmChammel);

    /**
     * 更新
     * @param wmChammel
     * @return
     */
    ResponseResult update(WmChannel wmChammel);

    /**
     *删除
     * @param id
     * @return
     */
    ResponseResult delete(Integer id);
}