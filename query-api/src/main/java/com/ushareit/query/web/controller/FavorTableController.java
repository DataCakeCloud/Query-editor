package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.FavorTable;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.FavorTableService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-07 15:22
 */
@Api(tags = "收藏数据表相关")
@RestController
@RequestMapping("/favortable")
public class FavorTableController extends BaseBusinessController<FavorTable> {

    @Autowired
    private FavorTableService favorTableService;

    @Override
    public BaseService<FavorTable> getBaseService() {
        return favorTableService;
    }

    @PostMapping("/add")
    public BaseResponse add(@RequestBody @Valid FavorTable favorTable){
        String name = getCurrentUser().getUserName();
        favorTableService.preCheckCommon(favorTable, name);
        return super.add(favorTable);
    }

    @PostMapping("/delete")
    public BaseResponse delete(@RequestBody @Valid String params){
        try {
            HashMap<String, String > map = JSON.parseObject(params, HashMap.class);
            String name = getCurrentUser().getUserName();
            String result = favorTableService.del(Integer.parseInt(map.get("id")),name);
            return BaseResponse.success(result);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.DELETE_FAIL,e);
        }
    }

    @GetMapping("/favortablelist")
    public BaseResponse favortablelist(@RequestParam String region,@RequestParam String catalog){
        try {
            String name = getCurrentUser().getUserName();
//            String name = "ext.huangkai";
            List tableList = favorTableService.tableList(name,region,catalog);
            return BaseResponse.success(tableList);
        }catch (Exception e){
            return BaseResponse.error(BaseResponseCodeEnum.DATA_NOT_FOUND,e);
        }
    }
}

