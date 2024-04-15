package com.ushareit.query.web.controller;

import com.ushareit.query.service.UserService;
import com.ushareit.query.constant.CommonConstant;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
@Api(tags = "用户配置")
@RestController
@RequestMapping("/configuration")
public class UserController extends BaseBusinessController {

    @Autowired
    private UserService userService;

    @Override
    public BaseService getBaseService() {
        return userService;
    }

//    @ApiOperation("查询当前user的group")
//    @GetMapping(value = "/group")
//    @Cacheable(cacheNames = {"group"}, key = "#name")
//    public BaseResponse<?> getUserGroup(@RequestParam("name") String name) {
//        System.out.println("Not use cache to get user group");
////        String name = getCurrentUser().getUserName();
//        return BaseResponse.success(userService.getUserGroup(name));
//    }

    @ApiOperation("查询当前user的engine")
    @GetMapping(value = "/engine")
    public BaseResponse<?> getUserEngine(@RequestParam(defaultValue = "") String region) {
        String name = getCurrentUser().getUserName();
        return BaseResponse.success(userService.getUserEngine(name, region));
    }
    
    @ApiOperation("查询当前所有region")
    @GetMapping(value = "/regions")
    public BaseResponse<?> getRegions() {
    	int tenantId = getCurrentUser().getTenantId();
        return BaseResponse.success(userService.getRegions(getUserInfo(), tenantId));
    }
}
