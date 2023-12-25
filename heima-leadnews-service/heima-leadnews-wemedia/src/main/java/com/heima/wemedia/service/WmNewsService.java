package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsDto;
import io.swagger.models.auth.In;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface WmNewsService extends IService<WmNews> {
    /**
     * 条件查询文件列表
     * @param dto
     * @return
     */
    public ResponseResult findList(WmNewsPageReqDto dto);
    /**
     *  发布文章或保存草稿
     * @param dto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto);

    /**
     * 查看详细文章
     * @param id
     * @return
     */
    public ResponseResult selectNewsById(Integer id);

    /**
     * 删除文章
     * @param id
     * @return
     */
    ResponseResult deleteNewsById(Integer id);

    /**
     * 上下架文章
     * @param dto
     * @return
     */
    ResponseResult newsDownOrUp(WmNewsDto dto);

    /**
     * 查询文章列表
     * @param dto
     * @return
     */
    ResponseResult adFindList(NewsAuthDto dto);

    /**
     * 查询文章详情
     * @param id
     * @return
     */
    ResponseResult findWmNewsVo(Integer id);

    /**
     * 文章审核失败
     * @param dto
     * @param status
     * @return
     */
    ResponseResult authNews(NewsAuthDto dto,Short status);
}
