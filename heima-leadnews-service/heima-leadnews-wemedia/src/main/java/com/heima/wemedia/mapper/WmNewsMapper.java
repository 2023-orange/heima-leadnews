package com.heima.wemedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.vo.WmNewsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Mapper
@Repository
public interface WmNewsMapper  extends BaseMapper<WmNews> {
    /**
     * 查询列表
     * @param dto dto
     * @return List
     */
    List<WmNewsVo> findListAndPage(@Param("dto") NewsAuthDto dto);

    /**
     * 查询记录数
     * @param dto dto
     * @return Integer
     */
    Integer findListCount(@Param("dto") NewsAuthDto dto);
}
