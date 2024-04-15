package com.ushareit.query.web.controller;

import com.ushareit.query.bean.BaseEntity;
import com.ushareit.query.bean.DataEntity;
import com.ushareit.query.bean.DeleteEntity;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.trace.holder.InfTraceContextHolder;
import com.ushareit.query.web.vo.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.Map;

/**
 * @author zhaopan
 * @date 2018/10/30
 **/
@Slf4j
public abstract class BaseBusinessController<T extends BaseEntity> extends BaseController {

    /**
     * get base service
     *
     * @return base service
     */
    public abstract BaseService<T> getBaseService();

    @GetMapping("/list")
    public BaseResponse list(T t) {
        // 添加信息
        if (t instanceof DeleteEntity) {
            DeleteEntity deleteEntity = (DeleteEntity)t;
            deleteEntity.setDeleteStatus(0);
        }
        return BaseResponse.success(getBaseService().listByExample(t));
    }

    @GetMapping("/page")
    public BaseResponse page(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "50") Integer pageSize,
                             @RequestParam Map<String, String> paramMap) {
        if (!InfTraceContextHolder.get().getAdmin()) {
            paramMap.put("reg", getCurrentUser().getGroupName());
        }
        return BaseResponse.success(getBaseService().listByPage(pageNum, pageSize, paramMap));
    }

    @GetMapping("/get")
    public BaseResponse getById(@RequestParam Object id) {
        if (getBaseService().getById(id) == null) {
            return BaseResponse.error(BaseResponseCodeEnum.DATA_NOT_FOUND);
        }

        return BaseResponse.success(getBaseService().getById(id));
    }

    @PostMapping("/add")
    public BaseResponse add(@RequestBody @Valid T t) {
        // 添加user信息
        if (t instanceof DataEntity) {
            DataEntity dataEntity = (DataEntity) t;
            dataEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
            dataEntity.setCreateBy(getCurrentUser().getUserName());
            dataEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            dataEntity.setUpdateBy(getCurrentUser().getUserName());

        }
        getBaseService().save(t);
        return BaseResponse.success();
    }

    @PutMapping("/update")
    public BaseResponse update(@RequestBody @Valid T t) {
        // 添加user信息
        if (t instanceof DataEntity) {
            DataEntity dataEntity = (DataEntity) t;
            dataEntity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            dataEntity.setUpdateBy(super.getCurrentUser().getUserName());
        }

        try {
            getBaseService().update(t);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return BaseResponse.error(BaseResponseCodeEnum.CLI_UPDATE_DB_FAIL, e);
        }


        return BaseResponse.success(BaseResponseCodeEnum.SUCCESS);
    }

    @DeleteMapping("/delete")
    public BaseResponse delete(@RequestParam @Valid Object id) {
        try {
            getBaseService().delete(id);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            BaseResponse.error(BaseResponseCodeEnum.CLI_DELETE_ILLEGAL, e);
        }
        return BaseResponse.success();
    }
}
