package com.ushareit.query.web.converter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.google.common.collect.Lists;

/**
 * 添加fastjson的转换
 *
 * @author wuyan
 * @date 2018/9/25
 */
@Configuration
public class FastJsonHttpMessageConverter implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        // 定义一个转换消息的对象
        com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter
                converter = new com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter();

        // 自定义配置，添加fastjson的配置信息 比如 ：是否要格式化返回的json数据
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        // 处理中文乱码问题
        fastJsonConfig.setCharset(Charset.defaultCharset());

        // 添加支持持的MediaType
        List<MediaType> fastMediaTypes = Lists.newArrayList();
        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        // fastMediaTypes.add(MediaType.TEXT_PLAIN);
        converter.setSupportedMediaTypes(fastMediaTypes);

        // 在转换器中添加配置信息
        converter.setFastJsonConfig(fastJsonConfig);
        converters.add(0, converter);

        /* ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        List<MediaType> listTypes = new ArrayList<MediaType>();
        listTypes.add(MediaType.APPLICATION_PDF);
        arrayHttpMessageConverter.setSupportedMediaTypes(listTypes);
        converters.add(1, arrayHttpMessageConverter); */
    }
}
