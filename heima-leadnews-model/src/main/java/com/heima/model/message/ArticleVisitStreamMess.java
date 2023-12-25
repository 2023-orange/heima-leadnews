package com.heima.model.message;

import lombok.Data;

@Data
public class ArticleVisitStreamMess {

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 阅读数
     */
    private int view;

    /**
     * 收藏数
     */
    private int collect;

    /**
     * 评论数
     */
    private int comment;

    /**
     * 点赞数
     */
    private int like;
}
