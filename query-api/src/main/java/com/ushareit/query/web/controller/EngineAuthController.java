package com.ushareit.query.web.controller;

import com.ushareit.query.bean.EngineAuth;
import com.ushareit.query.service.EngineAuthService;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Api(tags = "用户引擎权限配置")
@RestController
@RequestMapping("/engineAuth")
public class EngineAuthController extends BaseBusinessController<EngineAuth> {

    @Autowired
    private EngineAuthService engineAuthService;

    @Override
    public BaseService<EngineAuth> getBaseService() {
        return engineAuthService;
    }

    @ApiOperation("查询可选用户名")
    @GetMapping(value = "/users")
    public BaseResponse<?> getUserAll() {
        return BaseResponse.success(engineAuthService.getUserAll());
    }

    @ApiOperation("查询可选引擎列表")
    @GetMapping(value = "/engines")
    public BaseResponse<?> getEngineAll() {
        return BaseResponse.success(engineAuthService.getEngineAll());
    }

    @ApiOperation(value = "引擎权限列表")
    @GetMapping("/all")
    public BaseResponse getEngineAuth(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "50") Integer pageSize,
                                      @RequestParam String info) {
        return BaseResponse.success(engineAuthService.getEngineAuth(pageNum, pageSize, info));
    }

    @Override
    @ApiOperation(value = "创建用户引擎权限")
    @ApiResponses({
            @ApiResponse(code = 200, response = BaseResponse.class, message = "成功")
    })
    @PostMapping("/add")
    public BaseResponse add(@RequestBody @Valid EngineAuth engineAuth) {
        return super.add(engineAuth);
    }

    @Override
    @ApiOperation(value = "更新用户引擎权限")
    @PutMapping("/update")
    public BaseResponse update(@RequestBody @Valid EngineAuth engineAuth) {
        return super.update(engineAuth);
    }
}
