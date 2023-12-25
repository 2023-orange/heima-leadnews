package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNewsDto;
import com.heima.wemedia.service.WmNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {

    @Autowired
    private WmNewsService wmNewsService;

    @PostMapping("/list")
    public ResponseResult findList(@RequestBody WmNewsPageReqDto dto){
        return wmNewsService.findList(dto);
    }

    @PostMapping("/submit")
    public ResponseResult submitNews(@RequestBody WmNewsDto dto){
        return wmNewsService.submitNews(dto);
    }

    @GetMapping("/one/{id}")
    public ResponseResult selectNewsById(@PathVariable Integer id){
        return wmNewsService.selectNewsById(id);
    }

    @GetMapping("/del_news/{id}")
    public ResponseResult deleteNewsById(@PathVariable Integer id){
        return wmNewsService.deleteNewsById(id);
    }

    @PostMapping("/down_or_up")
    public ResponseResult newsDownOrUp(@RequestBody WmNewsDto dto){
        return wmNewsService.newsDownOrUp(dto);
    }

    @PostMapping("/list_vo")
    public ResponseResult adFindList(@RequestBody NewsAuthDto dto){
        return wmNewsService.adFindList(dto);
    }

    @GetMapping("/one_vo/{id}")
    public ResponseResult findWmNewsVo(@PathVariable("id") Integer id){
        return wmNewsService.findWmNewsVo(id);
    }

    @PostMapping("/auth_fail")
    public ResponseResult authNewsFail(@RequestBody NewsAuthDto dto,Short status){
        return wmNewsService.authNews(dto, (short) 2);
    }

    @PostMapping("/auth_pass")
    public ResponseResult authPass(@RequestBody NewsAuthDto newsAuthDto,Short status) {
        return wmNewsService.authNews(newsAuthDto, (short) 1);
    }
}
