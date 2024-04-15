package com.ushareit.query.web.controller;

import com.ushareit.query.bean.DataEngineer;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.DataEngineerService;
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
 * @author: huyx
 * @create: 2022-02-08 15:24
 */
@Api(tags = "调度信息")
@RestController
@RequestMapping("/dataEngineer")
public class DataEngineerController extends BaseBusinessController<DataEngineer> {
    @Autowired
    private DataEngineerService dataEngineerService;

    @Override
    public BaseService<DataEngineer> getBaseService() {
        return dataEngineerService;
    }

    @Override
    @ApiOperation(value = "保存调度信息")
    @ApiResponses({
            @ApiResponse(code = 200, response = BaseResponse.class, message = "成功")
    })
    @PostMapping("/add")
    public BaseResponse add(@RequestBody @Valid DataEngineer dataEngineer) {
        String uuid = dataEngineer.getUuid();
        List<DataEngineer> deInfoList = dataEngineerService.getDE(uuid);
        String engine = dataEngineer.getEngine();
        String querySql = dataEngineer.getQuerySql();
        if (deInfoList.size() > 0) {
            DataEngineer dataEngineerUpdate = deInfoList.get(0);
            dataEngineerUpdate.setEngine(engine);
            dataEngineerUpdate.setQuerySql(querySql);
            return super.update(dataEngineerUpdate);
        } else {
            return super.add(dataEngineer);
        }
    }

    @ApiOperation(value = "获取调度信息")
    @GetMapping("/getInfo")
    public BaseResponse getDataEngineerInfo(@RequestParam String uuid) {
        List<DataEngineer> deInfoList = dataEngineerService.getDE(uuid);
        HashMap<String, String> data = dataEngineerService.getInfo(deInfoList);
        return BaseResponse.success(data);
    }
}
