package com.heima.model.article.dtos;

import lombok.Data;

@Data
public class ArticleInfoDto {
    // 设备ID
    Integer equipmentId;
    
    // 文章ID
    Long articleId;
    
    // 作者ID
    Integer authorId;
}