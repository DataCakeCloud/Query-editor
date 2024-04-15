package com.ushareit.query.service.impl;

import com.ushareit.query.bean.QueryData;
import com.ushareit.query.mapper.QueryDataMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.QueryDataService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Slf4j
@Service
@Setter
public class QueryDataImpl extends AbstractBaseServiceImpl<QueryData> implements QueryDataService {

    @Resource
    private QueryDataMapper queryDataMapper;

    @Override
    public CrudMapper<QueryData> getBaseMapper() { return queryDataMapper; }
}
