package com.heima.model.message;

import lombok.Data;

@Data
public class UpdateArticleMess {

    /**
     * 文章id
     */
    private Long articleId;
    /**
     * 更新类型
     */
    private UpdateArticleType type;

    /**
     * 修改数据的增量，可为正负数
     */
    private Integer add;

    public enum UpdateArticleType {
        COLLECTION, // 收藏
        COMMENT, // 评论
        LIKES, // 点赞
        VIEWS; // 浏览
    }
}
