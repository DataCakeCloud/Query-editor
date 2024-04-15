package com.ushareit.query.service.impl;

import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.FavorTable;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.FavorTableMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.FavorTableService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
@Slf4j
@Service
@Setter
public class FavorTableServiceImpl extends AbstractBaseServiceImpl<FavorTable> implements FavorTableService {

    @Resource
    private FavorTableMapper favorTableMapper;

    @Override
    public CrudMapper<FavorTable> getBaseMapper() { return favorTableMapper; }

    public void preCheckCommon(FavorTable favorTable, String name) {
        //1. name不重复校验
        String title = favorTable.getName();
        Integer id = favorTable.getId();
        Integer active = 1;
        List<String> existQuery = favorTableMapper.selectByUsername(title, name, id, active,favorTable);
//        super.checkOnUpdate(super.getByName(savedQuery.getTitle()), savedQuery);
        if (existQuery.contains(title)) {
            throw new ServiceException(BaseResponseCodeEnum.NAME_IS_NOT_UNIQUE);
        }
    }

    @Override
    public String del(Integer id,String name){
        Integer active = 0;
        LocalDateTime updateTime = LocalDateTime.now();
        boolean result = favorTableMapper.del(id,updateTime,active,name);
        return String.valueOf(result);
    }

    @Override
    public List tableList(String name,String region,String catalog){
        Integer active = 1;
        List<FavorTable> tableList = favorTableMapper.tableList(name,active,region,catalog);
        List<Map> tableMap = new ArrayList<>();
        for (FavorTable dbTable:tableList){
            Map dbTableMap = new HashMap();
            dbTableMap.put(new String("db"),dbTable.getDb());
            dbTableMap.put(new String("table"),dbTable.getName());
            dbTableMap.put(new String("id"),dbTable.getId());
            tableMap.add(dbTableMap);
        }
        return tableMap;
    }
}
