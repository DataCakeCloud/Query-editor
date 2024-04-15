package com.ushareit.query.service;

import com.ushareit.query.bean.DataEngineer;

import java.util.HashMap;
import java.util.List;

/**
 * @author: huyx
 * @create: 2022-01-19 15:24
 */
public interface DataEngineerService extends BaseService<DataEngineer> {
    /**
     * 获取调度信息
     *
     */
    List<DataEngineer> getDE(String uuid);

    /**
     * 处理调度信息并返回
     *
     */
    HashMap<String, String> getInfo(List<DataEngineer> deInfoList);
}
