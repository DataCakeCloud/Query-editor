package com.ushareit.query.service.impl;

import com.ushareit.query.bean.DataEngineer;
import com.ushareit.query.mapper.DataEngineerMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.DataEngineerService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;


/**
 * @author: huyx
 * @create: 2022-02-08 15:24
 */
@Slf4j
@Service
@Setter
public class DataEngineerServiceImpl extends AbstractBaseServiceImpl<DataEngineer> implements DataEngineerService {
    @Resource
    private DataEngineerMapper dataEngineerMapper;

    @Override
    public CrudMapper<DataEngineer> getBaseMapper() { return dataEngineerMapper; }

    @Override
    public List<DataEngineer> getDE(String uuid) {
        return dataEngineerMapper.getByUuid(uuid);
    }

    @Override
    public HashMap<String, String> getInfo(List<DataEngineer> deInfoList) {
        HashMap<String, String> data = new HashMap<>();
        String region = "";
        String engine = "";
        String sql = "";
        if (deInfoList.size() > 0) {
            engine = deInfoList.get(0).getEngine();
            sql = deInfoList.get(0).getQuerySql();
            if (engine.equals("spark-submit-sql-3_aws_us-east-1")) {
                region = "ue1";
            } else if (engine.equals("spark-submit-sql-3_aws_ap-southeast-1")) {
                region = "sg1";
            } else {
                region = "sg2";
            }
        }
        data.put("region", region);
        data.put("sql", sql);
        return data;
    }
}
