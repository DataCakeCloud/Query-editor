package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.ushareit.query.bean.ClassificationDash;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.ClassificationDashService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Api(tags = "看板目录树相关")
@RestController
@RequestMapping("/classdash")
public class ClassificationDashController extends BaseBusinessController<ClassificationDash>{

    @Autowired
    private ClassificationDashService classificationDashService;

    @Override
    public BaseService<ClassificationDash> getBaseService(){ return classificationDashService; }

    @PostMapping("/add")
    public BaseResponse add (@RequestBody @Valid ClassificationDash classificationDash){
        String name = getCurrentUser().getUserName();
        classificationDashService.preCheckCommon(classificationDash, name);
        return super.add(classificationDash);
    }

    @PostMapping("/edit")
    public BaseResponse edit(@RequestBody @Valid String params){
        String name = getCurrentUser().getUserName();
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        classificationDashService.edit(Integer.parseInt(map.get("id")),map.get("name"),name,map.get("is_query"));
        return BaseResponse.success();
    }

    @PostMapping("/delete")
    public BaseResponse delete(@RequestBody @Valid String params){
        String name = getCurrentUser().getUserName();
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        String result = classificationDashService.delete(Integer.parseInt(map.get("id")),name);
        if (result.contains("不可删除")){
            return BaseResponse.error("500",result);
        }else {
            return BaseResponse.success();
        }
    }

    @PostMapping("/move")
    public BaseResponse move(@RequestBody @Valid String params){
        String name = getCurrentUser().getUserName();
        HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
        if (Integer.parseInt(map.get("is_query"))==0){
            String result = classificationDashService.selectLevelChild(Integer.parseInt(map.get("id")),Integer.parseInt(map.get("parent_id")));
            if (result.equals("目标移动目录不能为二级") || result.equals("该目录下有二级目录，不可往一级目录下移动")){
                return BaseResponse.error("1",result);
            }
        }
        classificationDashService.move(Integer.parseInt(map.get("id")),Integer.parseInt(map.get("parent_id")),
                Integer.parseInt(map.get("is_query")), name);
        return BaseResponse.success();
    }
}
