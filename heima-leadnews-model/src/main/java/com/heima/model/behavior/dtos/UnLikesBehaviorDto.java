package com.heima.model.behavior.dtos;

import lombok.Data;

@Data
public class UnLikesBehaviorDto {
    // 文章ID
    Long articleId;

    /**
     * 不喜欢操作方式
     * 0 不喜欢
     * 1 取消不喜欢
     */
    Short type;
}