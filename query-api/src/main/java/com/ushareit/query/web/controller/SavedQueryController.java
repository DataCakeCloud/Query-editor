package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.SavedQuery;
import com.ushareit.query.bean.ShareGrade;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.ClassificationService;
import com.ushareit.query.service.SavedQueryService;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: huyx
 * @create: 2022-02-08 15:24
 */
@Api(tags = "保存的查询")
@RestController
@RequestMapping("/savedQuery")
public class SavedQueryController extends BaseBusinessController<SavedQuery> {

    @Autowired
    private SavedQueryService savedQueryService;

    @Autowired
    private ClassificationService classificationService;

    @Override
    public BaseService<SavedQuery> getBaseService() {
        return savedQueryService;
    }

    @ApiOperation(value = "保存查询列表")
    @GetMapping("/all")
    public BaseResponse getSavedQuery(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "50") Integer pageSize,
                                      @RequestParam(defaultValue = "",required=false) String title,
                                      @RequestParam(defaultValue = "",required=false) String query_sql,
                                      @RequestParam(defaultValue = "",required=false) String engine,
                                      @RequestParam(defaultValue = "0",required=false) Integer folderID,
                                      @RequestParam(defaultValue = "",required=false)String region,
                                      @RequestParam String info) {
        if (info.contains("%5B")) {
            info = info.replace("%5B", "[");
        }
        if (info.contains("%5D")) {
            info = info.replace("%5D", "]");
        }
        if (info.contains("%7B")) {
            info = info.replace("%7B", "{");
        }
        if (info.contains("%7D")) {
            info = info.replace("%7D", "}");
        }
        if (info.contains("%5C")) {
            info = info.replace("%5C", "\\");
        }
        String name = getCurrentUser().getUserName();
        classificationService.addFirstLevel(name);
        PageInfo<SavedQuery> savedQueryList = savedQueryService.getSavedQuery(pageNum, pageSize,
        		title, query_sql, engine, folderID, region,
        		info, name, getCurrentUser().getGroupUuid());
        Map<String, Object> savedQueryPageInfo = savedQueryService.setParam(savedQueryList);
        return BaseResponse.success(savedQueryPageInfo);
    }

    @Override
    @ApiOperation(value = "创建保存查询")
    @ApiResponses({
            @ApiResponse(code = 200, response = BaseResponse.class, message = "成功")
    })
    @PostMapping("/add")
    public BaseResponse add(@RequestBody @Valid SavedQuery savedQuery) {
        String name = getCurrentUser().getUserName();
        savedQueryService.preCheckCommon(savedQuery, name);
        savedQuery.setUserGroup(getCurrentUser().getGroupUuid());
        return super.add(savedQuery);
    }

    @Override
    @ApiOperation(value = "更新保存查询")
    @ApiResponses({
            @ApiResponse(code = 200, response = BaseResponse.class, message = "成功")
    })
    @PutMapping("/update")
    public BaseResponse update(@RequestBody @Valid SavedQuery savedQuery) {
        String name = getCurrentUser().getUserName();
        savedQueryService.preCheckCommon(savedQuery, name);
        return super.update(savedQuery);
    }

    @ApiOperation(value = "批量删除保存查询")
    @DeleteMapping("/deleteBatch")
    public BaseResponse deleteBatch(@RequestParam String id) {
        return BaseResponse.success(savedQueryService.deleteBatch(id));
    }
    
    @ApiOperation(value = "创建分享")
    @PostMapping("/addShare")
    public BaseResponse add(@RequestBody @Valid  String params) {
    	ShareGrade sg = new ShareGrade();
        HashMap<String, Object> map = JSON.parseObject(params, HashMap.class);
        String sharer = map.get("sharer").toString();
        if (sharer.equalsIgnoreCase("admin")) {
        	sharer = getCurrentUser().getUserName();
        }
        String sql_name = "";
        if (null != map.get("name")) {
            sql_name = map.get("name").toString();
        }
        sg.setSharer(sharer);
        sg.setSharee(map.get("sharee").toString());
        sg.setGrade(Integer.parseInt(map.get("grade").toString()));
        sg.setShareUrl(map.get("shareUrl").toString());
        sg.setSqlName(sql_name);
    	sg.setCreateTime(new Timestamp(System.currentTimeMillis()));
    	int id = savedQueryService.addShareGrade(sg, map.get("shareeEmail").toString());
    	if (-1 == id) {
    		return BaseResponse.error(BaseResponseCodeEnum.SYS_ERR);
    	}
    	HashMap<String, Integer> ret_info = new HashMap<>();
    	ret_info.put("gradeID", id);
        return BaseResponse.success(ret_info);
    }
}
