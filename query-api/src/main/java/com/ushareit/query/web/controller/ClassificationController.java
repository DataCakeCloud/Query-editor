package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.QueryHistory;
import com.ushareit.query.bean.SavedQuery;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.ClassificationService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
@Api(tags = "文件夹相关")
@RestController
@RequestMapping("/classification")
public class ClassificationController extends BaseBusinessController<Classification>{

    @Autowired
    private ClassificationService classificationService;

    @Override
    public BaseService<Classification> getBaseService() {
        return classificationService;
    }

    @PostMapping("/add")
    public BaseResponse add(@RequestBody @Valid Classification classification){
        String name = getCurrentUser().getUserName();
        classificationService.preCheckCommon(classification, name);
        return super.add(classification);
    }

    @PostMapping("/edit")
    public BaseResponse edit(@RequestBody @Valid String params){
        try {
            HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            String result = classificationService.edit(Integer.parseInt(map.get("id")),map.get("name"),name);
            return BaseResponse.success(result);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.CLI_UPDATE_DB_FAIL, e);
        }
    }

    @PostMapping("/delete")
    public BaseResponse delete(@RequestBody @Valid String params){
        try {
            HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            String result = classificationService.del(Integer.parseInt(map.get("id")),name);
            return BaseResponse.success(result);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.DELETE_FAIL,e);
        }
    }
    @PostMapping("move")
    public BaseResponse move(@RequestBody @Valid String params){
        try {
            HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            if (Integer.parseInt(map.get("is_query"))==0){
                classificationService.selectLevelChild(Integer.parseInt(map.get("id")),Integer.parseInt(map.get("parent_id")));
            }
            String result = classificationService.move(Integer.parseInt(map.get("id")),
                    Integer.parseInt(map.get("parent_id")),Integer.parseInt(map.get("is_query")),name);
            return BaseResponse.success(result);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.CLI_UPDATE_DB_FAIL, e);
        }
    }
    @GetMapping("tree")
    public BaseResponse tree(){
        try {
            //目录树结构
            String name = getCurrentUser().getUserName();
            List tree = classificationService.tree(name);
            return BaseResponse.success(tree);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.DATA_NOT_FOUND,e);
        }
    }
}
