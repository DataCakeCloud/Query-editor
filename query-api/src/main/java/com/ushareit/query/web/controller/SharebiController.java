package com.ushareit.query.web.controller;

import com.ushareit.query.bean.Sharebi;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.SharebiService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * @author: wangsy1
 * @create: 2022-11-30 15:22
 */
@Api(tags = "分享相关")
@RestController
@RequestMapping("/sharebi")
public class SharebiController extends BaseBusinessController<Sharebi>{

    @Autowired
    private SharebiService sharebiService;

    @Override
    public BaseService<Sharebi> getBaseService(){ return sharebiService; }

    @ApiOperation("获取分享权限")
    @GetMapping(value="/shareGrade")
    public BaseResponse<?> getShare(@RequestParam(defaultValue = "") String sharee,
                                    @RequestParam(defaultValue = "0") Integer gradeID) {
        if (0 == gradeID) {
            return BaseResponse.error(BaseResponseCodeEnum.CLI_ID_NOTNULL);
        }
        int grade = sharebiService.getShare(sharee, gradeID);
        if (-1 == grade || -2 == grade) {
            return BaseResponse.error(BaseResponseCodeEnum.DATA_NOT_FOUND);
        }
        HashMap<String, Integer> ret_info = new HashMap<>();
        ret_info.put("grade", grade);
        return BaseResponse.success(ret_info);
    }
}
