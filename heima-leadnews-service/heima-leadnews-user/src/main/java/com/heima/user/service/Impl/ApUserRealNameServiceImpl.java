package com.heima.user.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.apis.user.IWmUserClient;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealNameService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class ApUserRealNameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealNameService {
    /**
     * 查询用户认证列表
     * @param dto
     * @return
     */

    @Autowired
    private ApUserRealnameMapper apUserRealnameMapper;

    @Autowired
    private ApUserMapper apUserMapper;

    @Override
    public ResponseResult list(AuthDto dto) {
        //1.参数校验
        if(dto==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"参数错误");
        }
        //2.分页参数校验
        dto.checkParam();
        //3.构建分页条件
        Page<ApUserRealname> page = new Page<>(dto.getPage(),dto.getSize());
        //4.构建查询条件
        LambdaQueryWrapper<ApUserRealname> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dto.getStatus() != null,ApUserRealname::getStatus,dto.getStatus());
        //5.执行查询数据
        Page<ApUserRealname> apUserRealnamePage = apUserRealnameMapper.selectPage(page, lambdaQueryWrapper);
        //6.组装查询结果
        long total = apUserRealnamePage.getTotal();
        PageResponseResult pageResponseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)total);
        pageResponseResult.setData(apUserRealnamePage.getRecords());
        return pageResponseResult;
    }


    @Autowired
    private IWmUserClient iWmUserClient;
    /**
     * 通过/不通过
     *
     * @param dto  审核信息
     * @param type 1通过 2不通过
     * @return
     */
    @Override
    public ResponseResult auth(AuthDto dto, Integer type) {
        //1.入参检测
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.提取id
        Integer id = dto.getId();
        //3.根据id查询认证记录
        ApUserRealname apUserRealname = apUserRealnameMapper.selectById(id);
        if (apUserRealname == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"认证记录不存在");
        }
        //4.更改认证记录状态
        apUserRealname.setStatus((short) (type == 1 ? 9 : 2));
        apUserRealname.setId(id);
        apUserRealname.setReason(dto.getMsg());
        apUserRealnameMapper.updateById(apUserRealname);
        //5.通过
        if (type == 1){
            //5.1通过认证记录获得对应的用户名
            String name = apUserRealname.getName();
            //5.2调用feign接口，完成媒体账号创建
            Map<String,String> map = new HashMap<>();
            map.put("name", name);
            try {
                ResponseResult responseResult = iWmUserClient.addUser(map);
                // 处理响应
            } catch (FeignException e) {
                log.error("Feign调用失败", e);
                throw new RuntimeException("自媒体用户创建失败", e);
            }
//            if (responseResult == null || responseResult.getCode() != 200){
//                throw new RuntimeException("自媒体用户创建失败");
//            }
            //5.3更新app用户的flag状态
            ApUser apUser = new ApUser();
            apUser.setId(apUserRealname.getUserId());
            apUser.setFlag((short) 1);
            int updateResult = apUserMapper.updateById(apUser);
            if (updateResult < 1){
                throw new RuntimeException("app用户flag状态更新失败");
            }
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
