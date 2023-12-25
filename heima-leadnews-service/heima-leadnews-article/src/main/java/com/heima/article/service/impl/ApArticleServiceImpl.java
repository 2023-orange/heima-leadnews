package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.APArticleContentMapper;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.article.thread.AppThreadLocalUtil;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.BehaviorConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.message.ArticleVisitStreamMess;
import com.heima.model.message.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    private final static short MAX_PAGE_SIZE = 50;
    @Override
    public ResponseResult load(ArticleHomeDto dto,Short type) {
        //1.校验参数
        //分页条数的校验
        Integer size = dto.getSize();
        if (size == null || size == 0){
            size = 10;
        }
        //分页的值不超过50
        size = Math.min(size, MAX_PAGE_SIZE);
        dto.setSize(size);
        //校验参数  type
        if (!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //频道参数校验
        if (StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //时间校验
        if (dto.getMaxBehotTime() == null)dto.setMaxBehotTime(new Date());
        if (dto.getMinBehotTime() == null)dto.setMinBehotTime(new Date());
        //2.查询
        List<ApArticle> articleList = apArticleMapper.loadArticleList(dto,type);
        //3.结果返回
        return ResponseResult.okResult(articleList);
    }

    /**
     * 加载文章列表
     *
     * @param dto
     * @param type      1 加载更多  2 加载最新
     * @param firstPage true    是首页      false 不是首页
     * @return
     */
    @Override
    public ResponseResult load2(ArticleHomeDto dto, Short type, boolean firstPage) {
        if (firstPage){
            String jsonStr = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if (StringUtils.isNotBlank(jsonStr)){
                List<HotArticleVo> hotArticleVoList = JSON.parseArray(jsonStr, HotArticleVo.class);
                return ResponseResult.okResult(hotArticleVoList);
            }
        }
        return load(dto,type);
    }

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private APArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;
    /**
     * 保存app端相关文章
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
        //降级处理测试
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //1.检查参数
        if (dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto, apArticle);
        //2.判断是否存在id
        if (dto.getId() == null){
            //2.1不存在id保存 文章 文章配置 文章内容

            //保存文章
            save(apArticle);
            //保存配置
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);
            //保存内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(apArticleContent);
        }else {
            //2.2存在id 修改文章 文章内容
            //修改文章
            updateById(apArticle);
            //修改文章内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }

        //异步调用  生成静态文件，上传到minio
        articleFreemarkerService.buildArticleToMinIo(apArticle, dto.getContent());
        //3.结果返回 文章的id
        return ResponseResult.okResult(apArticle.getId());
    }

    @Autowired
    private CacheService cacheService;
    /**
     * 加载文章详情 数据回显
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {
        //1.检查参数
        if (dto == null || dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        boolean isfollow = false;
        boolean islike = false;
        boolean isunlike = false;
        boolean iscollection = false;
        //2.获取当前用户
        ApUser user = AppThreadLocalUtil.getUser();
        if (user != null){
            //3.查询用户的行为
            //3.1查询喜欢行为
            String likeBehaviorJson  = (String) cacheService.hGet(BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId(), user.getId().toString());
            if (StringUtils.isNotBlank(likeBehaviorJson)){
                islike = true;
            }
            //3.2查询不喜欢行为
            String unlikeBehaviorJson  = (String) cacheService.hGet(BehaviorConstants.UN_LIKE_BEHAVIOR + dto.getArticleId(), user.getId().toString());
            if (StringUtils.isNotBlank(unlikeBehaviorJson)){
                isunlike = true;
            }
            //3.3查询收藏行为
            String collectionBehaviorJson  = (String) cacheService.hGet(BehaviorConstants.COLLECTION_BEHAVIOR + dto.getArticleId(), user.getId().toString());
            if (StringUtils.isNotBlank(collectionBehaviorJson)){
                iscollection = true;
            }
            //3.4查询关注行为
            Double score = cacheService.zScore(BehaviorConstants.APUSER_FOLLOW_RELATION + user.getId(), dto.getAuthorId().toString());
            if (score != null){
                isfollow = true;
            }

        }
        //4.结果返回
        Map<String, Object> map = new HashMap<>();
        map.put("isfollow", isfollow);
        map.put("islike", islike);
        map.put("isunlike", isunlike);
        map.put("iscollection", iscollection);
        return ResponseResult.okResult(map);
    }

    /**
     * 更新文章的分值  更新缓存中的热点文章数据
     *
     * @param mess
     */
    @Override
    public void updateScore(ArticleVisitStreamMess mess) {
        //1.更新文章的阅读、点赞、收藏、评论数量
        ApArticle apArticle = UpdateArticle(mess);
        //2.计算文章的分值
        Integer score = computeScore(apArticle);
        score = score * 3;

        //3。替换当前文章对应频道的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());

        //4.替换推荐对应的热点数据
        replaceDataToRedis(apArticle, score, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);
    }

    /**
     * 替换热点数据并且存入到Redis中
     * @param apArticle
     * @param score
     * @param s
     */
    private void replaceDataToRedis(ApArticle apArticle, Integer score, String s) {
        String articleListStr = cacheService.get(s);
        if (StringUtils.isNotBlank(articleListStr)) {
            List<HotArticleVo> hotArticleVoList = JSON.parseArray(articleListStr, HotArticleVo.class);

            boolean flag = true;
            //如果缓存中存在该文章，只更新分值
            for (HotArticleVo hotArticleVo : hotArticleVoList) {
                if (hotArticleVo.getId().equals(apArticle.getId())) {
                    hotArticleVo.setScore(score);
                    flag = false;
                    break;
                }
            }
            if (flag) {
                //如果缓存中不存在，查询缓存中分值最小的一条数据，进行分值比较，如果当前文章的分值大于缓存中文章的分值就替换
                if (hotArticleVoList.size() >= 30) {
                    hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
                    HotArticleVo lastHot = hotArticleVoList.get(hotArticleVoList.size() - 1);
                    if (lastHot.getScore() < score) {
                        hotArticleVoList.remove(lastHot);
                        HotArticleVo hot = new HotArticleVo();
                        BeanUtils.copyProperties(apArticle, hot);
                        hot.setScore(score);
                        hotArticleVoList.add(hot);
                    }
                } else {
                    HotArticleVo hot = new HotArticleVo();
                    BeanUtils.copyProperties(apArticle, hot);
                    hot.setScore(score);
                    hotArticleVoList.add(hot);
                }
            }
            //更新缓存
            hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
            cacheService.set(s, JSON.toJSONString(hotArticleVoList));
        }
    }

    /**
     * 更新文章的阅读、点赞、收藏、评论数量
     * @param mess
     */
    private ApArticle UpdateArticle(ArticleVisitStreamMess mess) {
        ApArticle apArticle = getById(mess.getArticleId());
        apArticle.setCollection(apArticle.getCollection()==null?0:apArticle.getCollection() + mess.getCollect());
        apArticle.setLikes(apArticle.getLikes()==null?0:apArticle.getLikes() + mess.getLike());
        apArticle.setComment(apArticle.getComment()==null?0:apArticle.getComment() + mess.getComment());
        apArticle.setViews(apArticle.getViews()==null?0:apArticle.getViews() + mess.getView());
        updateById(apArticle);
        return apArticle;
    }

    /**
     *计算文章的具体分值
     * @param apArticle
     * @return
     */
    private Integer computeScore(ApArticle apArticle) {
        Integer score = 0;
        if (apArticle.getLikes() != null) {
            score += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if (apArticle.getCollection() != null) {
            score += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }
        if (apArticle.getComment() != null) {
            score += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if (apArticle.getViews() != null) {
            score += apArticle.getViews() * ArticleConstants.HOT_ARTICLE_READ_WEIGHT;
        }
        return score;
    }
}
