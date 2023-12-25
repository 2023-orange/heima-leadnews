package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.apis.article.IArticleClient;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.*;
import com.heima.model.wemedia.vo.WmNewsVo;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.core.toolkit.StringUtils.*;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl  extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    /**
     * 条件查询文件列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        //1。检查参数
        //分页检查
        dto.checkParam();
        //2.分页的条件查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper = new LambdaQueryWrapper<>();
        //状态精确查询
        queryWrapper.eq(dto.getStatus() != null,WmNews::getStatus,dto.getStatus());
        //频道精确查询
        queryWrapper.eq(dto.getChannelId() != null,WmNews::getChannelId,dto.getChannelId());
        //时间范围查询
        queryWrapper.between(dto.getBeginPubDate() != null && dto.getEndPubDate() != null,
                WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        //关键字的模糊查询
        queryWrapper.like(isNotBlank(dto.getKeyword()),WmNews::getTitle,dto.getKeyword());
        //查询当前登录人的文章
        queryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());
        //按照发布时间倒序查询
        queryWrapper.orderByDesc(WmNews::getPublishTime);
        page = page(page, queryWrapper);

        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;
    /**
     * 发布修改文章或保存为草稿
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        // 0.条件判断
        if (dto == null && dto.getContent() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 1.保存或修改文章
        WmNews wmNews = new WmNews();
        //属性拷贝 属性名词和类型相同才能拷贝
        BeanUtils.copyProperties(dto,wmNews);
        // 封面图片 list --> String 以便保存数据库中
        if(dto.getImages() != null && dto.getImages().size() > 0){
            //[1dddfsd.jpg,sdlfjldk.jpg]-->   1dddfsd.jpg,sdlfjldk.jpg
            String imageStr = StringUtils.join(dto.getImages(), "，");
            wmNews.setImages(imageStr);
        }
        // 如果当前封面类型为自动，传值为-1
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);
        // 2.判断是否为草稿，如果为草稿，结束当前方法
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())){
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }
        // 3.不是草稿，保存文章内容图片与素材的关系
        // 提取到文章内容中所有图片的信息
        List<String> materials = ectractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(materials, wmNews.getId());
        // 4.不是草稿，保存文章封面图片与素材的关系：如果当前布局是自动的，需要匹配封面图片
        saveRelativeInfoForCover(dto, wmNews, materials);

        //审核文章
//        wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        wmNewsTaskService.addNewsToTask(wmNews.getId(),wmNews.getPublishTime());
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 第一个功能：如果当前封面类型为自动，则设置封面类型的数据
     *  匹配规则：
     *      1. 如果内容图片大于等于1，小于3  单图  type 1
     *      2. 如果内容图片大于等于3 多图  type 3
     *      2. 如果内容没有图片 无图  type 0
     * 第二个功能：保存封面图片与素材的关系
     * @param dto
     * @param wmNews
     * @param materials
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> materials){
        List<String> images = dto.getImages();
        // 1.如果当前封面类型为自动，则设置封面类型的数据
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            //多图
            if (materials.size() >= 3){
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            }else if (materials.size() >= 1 && materials.size() < 3){
                // 单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            }else {
                // 无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
        }
        // 修改文章
        if (images != null && images.size() > 0){
            wmNews.setImages(org.apache.commons.lang.StringUtils.join(images, ","));
        }
        updateById(wmNews);
        // 2.保存封面图片与素材的关系
        if (images != null && images.size() > 0){
            saveRelativeInfo(images, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
        }
    }

    /**
     * 处理文章内容图片与素材的关系
     * @param materials
     * @param newsId
     */
    private void saveRelativeInfoForContent(List<String> materials, Integer newsId) {
        saveRelativeInfo(materials, newsId, WemediaConstants.WM_CONTENT_REFERENCE);
    }

    /**
     * 保存文章图片与素材的关系到数据库中
     * @param materials
     * @param newsId
     * @param type
     */
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type){
        if (materials != null && !materials.isEmpty()){
            // 通过图片的url查询素材的id
            LambdaQueryWrapper<WmMaterial> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(WmMaterial::getUrl,materials);
            List<WmMaterial> materialList = wmMaterialMapper.selectList(wrapper);
            // 判断素材是否有效
            if (materialList == null || materialList.size() == 0){
                // 手动抛出异常
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }
            // 传递参数的数量 与 数据库查询的数量 不匹配
            if (materials.size() != materialList.size()){
                // 手动抛出异常
                throw new CustomException(AppHttpCodeEnum.MATERIAL_REFERENCE_FAIL);
            }
            List<Integer> idList = materialList.stream().map(WmMaterial::getId).collect(Collectors.toList());

            // 批量保存
            wmNewsMaterialMapper.saveRelations(idList, newsId, type);
        }

    }

    /**
     * 提取文章内容中的图片信息
     * @param content
     * @return
     */
    private List<String> ectractUrlInfo(String content){
        List<String> materials = new ArrayList<>();
        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if (map.get("type").equals("image")){
                String imgUrl = (String) map.get("value");
                materials.add(imgUrl);
            }
        }
        return materials;
    }

    /**
     * 保存或修改文章
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        // 补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1); // 默认文章上架
        if (wmNews.getId() == null){
            save(wmNews);
        }else {
            // 修改文章
            // 删除文章图片与素材的关系
            LambdaQueryWrapper<WmNewsMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(WmNewsMaterial::getNewsId,wmNews.getId());
            wmNewsMaterialMapper.delete(lambdaQueryWrapper);
            updateById(wmNews);
        }

    }

    /**
     * 获取详细id
     * @param id
     * @return
     */
    @Override
    public ResponseResult selectNewsById(Integer id) {
        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(id);
        return ResponseResult.okResult(wmNews);
    }

    /**
     * 删除文章
     * @param id
     * @return
     */
    @Override
    public ResponseResult deleteNewsById(Integer id) {
        // 1.检查参数
        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(id);
        // 2.判断文章是否存在
        if (wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 3.判断文章是否已发布
        if (wmNews.getStatus().equals(WmNews.Status.PUBLISHED)){
            // 已发布文章，不能删除
            return ResponseResult.errorResult(AppHttpCodeEnum.MATERIAL_IS_PUBLISHED);
        }
        // 4.可以删除文章
        // 4.1.删除wm_news数据库
        removeById(id);
        // 4.2.删除wm_news_material数据库
        LambdaQueryWrapper<WmNewsMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WmNewsMaterial::getId,id);
        wmNewsMaterialMapper.delete(queryWrapper);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;
    /**
     * 上下架文章
     * @param dto
     * @return
     */
    @Override
    public ResponseResult newsDownOrUp(WmNewsDto dto) {
        // 1.检查参数
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        WmNews wmNews = getById(dto.getId());
        // 2.判断文章是否存在
        if (wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 3.文章存在，判断是否是发布状态：正在发布状态，不能操作上下架
        if (wmNews.getStatus().equals(WmNews.Status.PUBLISHED)){
            return ResponseResult.errorResult(AppHttpCodeEnum.MATERIAL_IS_PUBLISHED);
        }

        // 4.可以操作上下架
        wmNews.setEnable(dto.getEnable());
        updateById(wmNews);

        if (wmNews.getArticleId() != null){
            //发送消息，通知article修改文章的配置
            Map<String,Object> map = new HashMap<>();
            map.put("articleId",wmNews.getArticleId());
            map.put("enable", dto.getEnable());
            kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,JSON.toJSONString(map));
        }

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private WmNewsMapper wmNewsMapper;
    /**
     * 查询文章列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult adFindList(NewsAuthDto dto) {
        //参数校验
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.分页参数检验
        dto.checkParam();
        //2.记录当前页
        Integer page = dto.getPage();
        //3.分页查询+count查询
        dto.setPage((dto.getPage() - 1) * dto.getSize());
        List<WmNewsVo> wmNewsVoList = wmNewsMapper.findListAndPage(dto);
        int count = wmNewsMapper.findListCount(dto);
        //4.结果返回
        ResponseResult responseResult = new PageResponseResult(page, dto.getSize(), count);
        responseResult.setData(wmNewsVoList);
        return responseResult;
    }

    @Autowired
    private WmUserMapper wmUserMapper;
    /**
     * 查询文章详情
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult findWmNewsVo(Integer id) {
        //1.检验参数
        if (id == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询文章详情
        WmNews wmNews = getById(id);
        if (wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //3.查询作者信息
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        //4.封装结果
        WmNewsVo wmNewsVo = new WmNewsVo();
        //5.属性拷贝
        BeanUtils.copyProperties(wmNews, wmNewsVo);
        if (wmUser != null){
            wmNewsVo.setAuthorName(wmUser.getName());
        }
        //6.结果返回
        return ResponseResult.okResult(wmNewsVo);
    }

    /**
     * 文章审核，修改状态
     * @param status 2  审核失败  4 审核成功
     * @param dto
     * @return
     */
    public ResponseResult authNews(NewsAuthDto dto,Short status) {
        //1.检查参数
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.查询文章信息
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //3.修改文章的状态
        wmNews.setStatus(status);
        if(org.apache.commons.lang3.StringUtils.isNotBlank(dto.getMsg())){
            wmNews.setReason(dto.getMsg());
        }
        updateById(wmNews);

        //审核成功，则需要创建app端文章数据，并修改自媒体文章
        if(status.equals(4)){
            //创建app端文章数据
            ResponseResult responseResult = wmNewsAutoScanService.saveAppArticle(wmNews);
            if(responseResult.getCode().equals(200)){
                wmNews.setArticleId((Long) responseResult.getData());
                wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
                updateById(wmNews);
            }
        }
        //4.返回
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

}
