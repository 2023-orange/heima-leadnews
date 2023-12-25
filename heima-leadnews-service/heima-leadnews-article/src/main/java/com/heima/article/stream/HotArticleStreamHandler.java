package com.heima.article.stream;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.message.ArticleVisitStreamMess;
import com.heima.model.message.UpdateArticleMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.internals.KStreamReduce;
import org.apache.kafka.streams.kstream.internals.TimeWindow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class HotArticleStreamHandler {

    @Bean
    public KStream<String,String> kStream(StreamsBuilder streamsBuilder){
        //接受消息
        KStream<String,String> stream = streamsBuilder.stream(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC);

        //聚合流式处理
        stream.map((key,value)-> {
            UpdateArticleMess mess = JSON.parseObject(value, UpdateArticleMess.class);
            //重置消息的key：12143414（文章的ID）和value：likes:1
            return new KeyValue<>(mess.getArticleId().toString(), mess.getType().name()+":"+mess.getAdd());
        })
                //按照文章ID进行聚合
                .groupBy((key,value)->key)
                //时间窗口
                .windowedBy(TimeWindows.of(Duration.ofSeconds(5)))
                /**
                 * 自行完成聚合的计算，由于我们有1和-1不能单纯的交给stream进行count
                 */
                .aggregate(new Initializer<String>() {
                    /**
                     * 初始方法，返回值是消息初始的的value，也就是下一个方法的aggvalue
                     * @return
                     */
                    @Override
                    public String apply() {
                        return "COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0";
                    }
                }, new Aggregator<String, String, String>() {
                    /**
                     * 真正的聚合操作,返回值是消息的value
                     * @param key
                     * @param value
                     * @param aggValue
                     * @return
                     */
                    @Override
                    public String apply(String key, String value, String aggValue) {
                        if (StringUtils.isBlank(value)){
                            return aggValue;
                        }
                        String[] aggAry = aggValue.split(",");
                        int col = 0,com = 0,lik = 0,vie = 0;
                        for (String agg : aggAry) {
                            String[] split = agg.split(":");
                            /**
                             * 获得初始值，时间窗口内计算的值
                             * 在这个循环中，实际上只更新了 col、com、lik、vie 这四个变量的值，
                             * 而不是更新了整个 aggValue。这是为了获得时间窗口内的初始值，以备后续累加操作。
                             */
                            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])){
                                case COLLECTION:
                                    col = Integer.parseInt(split[1]);
                                    break;
                                case COMMENT:
                                    com = Integer.parseInt(split[1]);
                                    break;
                                case LIKES:
                                    lik = Integer.parseInt(split[1]);
                                    break;
                                case VIEWS:
                                    vie = Integer.parseInt(split[1]);
                                    break;
                            }
                        }
                        /**
                         * 累加操作，时间窗口内的值+消息的值
                         */
                        String[] valAry = value.split(":");
                        switch (UpdateArticleMess.UpdateArticleType.valueOf(valAry[0])){
                            case COLLECTION:
                                col += Integer.parseInt(valAry[1]);
                                break;
                            case COMMENT:
                                com += Integer.parseInt(valAry[1]);
                                break;
                            case LIKES:
                                lik += Integer.parseInt(valAry[1]);
                                break;
                            case VIEWS:
                                vie += Integer.parseInt(valAry[1]);
                                break;
                        }
                        String formatStr = String.format("COLLECTION:%d,COMMENT:%d,LIKES:%d,VIEWS:%d", col, com, lik, vie);
                        System.out.println("文章ID："+key+"，当前时间窗口内的消息处理结果："+formatStr);
                        return formatStr;
                    }
                }, Materialized.as("hot-article-stream-count-001")).toStream()
                .map((key,value) -> {
                    return new KeyValue<>(key.key().toString(),formatObj(key.key().toString(),value));
                })
                //发送消息
                .to(HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC);
        return stream;
    }

    /**
     * 格式化消息value数据
     * @param articleId
     * @param value
     * @return
     */
    public String formatObj(String articleId,String value){
        ArticleVisitStreamMess mess = new ArticleVisitStreamMess();
        mess.setArticleId(Long.valueOf(articleId));
        //"COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0"
        String[] valueAry = value.split(",");
        for (String val : valueAry) {
            String[] split = val.split(":");
            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])){
                case COLLECTION:
                    mess.setCollect(Integer.parseInt(split[1]));
                    break;
                case COMMENT:
                    mess.setComment(Integer.parseInt(split[1]));
                    break;
                case LIKES:
                    mess.setLike(Integer.parseInt(split[1]));
                    break;
                case VIEWS:
                    mess.setView(Integer.parseInt(split[1]));
                    break;
            }
        }
        log.info("聚合消息处理之后的结果为：{}",JSON.toJSONString(mess));
        return JSON.toJSONString(mess);
    }

}
