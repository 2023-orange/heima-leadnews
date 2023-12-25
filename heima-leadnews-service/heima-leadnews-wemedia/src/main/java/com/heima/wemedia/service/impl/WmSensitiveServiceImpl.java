package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.SensitiveDto;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.service.WmSensitiveService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class WmSensitiveServiceImpl extends ServiceImpl<WmSensitiveMapper,WmSensitive> implements WmSensitiveService {
    /**
     * 新增敏感词
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult insert(WmSensitive wmSensitive) {
        //1.检查参数
        if (wmSensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"敏感词不能为空");
        }
        //已存在的敏感词，不能保存
        WmSensitive sensitive = getOne(Wrappers.<WmSensitive>lambdaQuery().eq(WmSensitive::getSensitives, wmSensitive.getSensitives()));
        if (sensitive != null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"敏感词已存在");
        }
        //2.保存
        wmSensitive.setCreatedTime(new Date());
        save(wmSensitive);
        //3.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 查询敏感词列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(SensitiveDto dto) {
        //1.检查参数
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.检查分页
        dto.checkParam();
        //3.模糊查询+分页
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmSensitive> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dto.getName())){
            lambdaQueryWrapper.like(WmSensitive::getSensitives,dto.getName());
        }
        page = page(page,lambdaQueryWrapper);
        //4.返回结果
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    /**
     * 修改敏感词
     * @param wmSensitive
     * @return
     */
    @Override
    public ResponseResult update(WmSensitive wmSensitive) {
        //1.检查参数
        if (wmSensitive == null || wmSensitive.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.修改
        updateById(wmSensitive);
        //3.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 删除敏感词
     * @param id
     * @return
     */
    @Override
    public ResponseResult delete(Integer id) {
        //1.检查参数
        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询敏感词
        WmSensitive wmSensitive = getById(id);
        if (wmSensitive == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"敏感词不存在");
        }
        //2.删除
        removeById(id);
        //3.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
