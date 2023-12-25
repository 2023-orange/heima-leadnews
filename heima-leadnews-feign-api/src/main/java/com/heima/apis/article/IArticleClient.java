package com.heima.apis.article;

import com.heima.apis.article.fallback.IArticleClientfallback;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "leadnews-article",fallback = IArticleClientfallback.class)
public interface IArticleClient {

    @PostMapping("/api/v1/article/save")
    public ResponseResult saveArticle(ArticleDto articleDto);
}
