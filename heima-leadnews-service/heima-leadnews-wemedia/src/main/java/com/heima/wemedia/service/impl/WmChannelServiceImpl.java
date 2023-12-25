package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.ChannelDto;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.service.WmChannelService;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {

    @Autowired
    private WmNewsService wmNewsService;

    @Override
    public ResponseResult findAll() {
         return ResponseResult.okResult(list());
    }

    @Override
    public ResponseResult findByNameAndPage(ChannelDto dto) {
        //1.检查参数
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //检查分页
        dto.checkParam();
        //2.模糊查询+分页
        IPage page = new Page(dto.getPage(),dto.getSize());
        //频道名称模糊查询
        LambdaQueryWrapper<WmChannel> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dto.getName())){
            lambdaQueryWrapper.like(WmChannel::getName,dto.getName());
        }
        page = page(page,lambdaQueryWrapper);
        //3.返回结果
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    /**
     * 保存
     * @param wmChammel
     * @return
     */
    @Override
    public ResponseResult insert(WmChannel wmChammel) {
        //1.检查参数
        if (wmChammel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmChannel channel = getOne(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getName, wmChammel.getName()));
        if (channel != null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"频道已存在");
        }
        //2.保存
        wmChammel.setCreatedTime(new Date());
        save(wmChammel);
        //3.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 更新
     * @param wmChammel
     * @return
     */
    @Override
    public ResponseResult update(WmChannel wmChammel) {
        //1.检查参数
        if (wmChammel == null || wmChammel.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.判断频道是否被引用
        int count = wmNewsService.count(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, wmChammel.getId())
                .eq(WmNews::getStatus, WmNews.Status.PUBLISHED.getCode()));
        if (count > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道已被引用");
        }
        //3.更新
        updateById(wmChammel);
        //4.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @Override
    public ResponseResult delete(Integer id) {
        //1.检查参数
        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询频道
        WmChannel channel = getById(id);
        if (channel == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"频道不存在");
        }
        //3.判断频道是否有效
        if (channel.getStatus()){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道有效，不能删除");
        }
        //4.判断频道是否被引用
        int count = wmNewsService.count(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getChannelId, id).eq(WmNews::getStatus, WmNews.Status.PUBLISHED.getCode()));
        if (count > 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"频道已被引用");
        }
        //5.删除
        removeById(id);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}