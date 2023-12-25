package com.heima.common.baiduyun; /**
 * @Classname BaiDuYunContentModerationUtil
 * @Date 2023/7/13 8:55
 * @Created ZFC
 */

import com.baidu.aip.contentcensor.AipContentCensor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 百度智能云内容审核工具类
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "baiduyun")
public class BaiDuYunContentModerationUtil {
	/**
     * 这里的参数之所以没有直接声明是因为使用到了nacos
     * 因为用到了@ConfigurationProperties注解，他会在springboot启动的时候注入baiduyun
     *
     * 我在nacos定义的内容是：
     * baiduyun:
     *   api_key: ******
     *   secret_key: ******
     *   appid: ******
     */

    private String api_key;
    private String secret_key;
    private String appid;

    /**
     * 内容审核
     * @return
     */
    public AipContentCensor contentScan(){
        AipContentCensor aipContentCensor = new AipContentCensor(appid,api_key,secret_key);
        // 可选：设置网络连接参数
        aipContentCensor.setConnectionTimeoutInMillis(3000);
        aipContentCensor.setSocketTimeoutInMillis(60000);
        //这里可能还有其他相关的参数，我也不是太了解，暂时先用着，如果看到这篇文章的大佬有更好的工具类希望可以分享一下，嘿嘿
        return aipContentCensor;
    }
}
