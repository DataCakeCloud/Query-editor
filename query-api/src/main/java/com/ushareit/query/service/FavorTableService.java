package com.ushareit.query.service;

import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.FavorTable;

import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-07 15:22
 */
public interface FavorTableService extends BaseService<FavorTable> {

    void preCheckCommon(FavorTable favorTable, String name);

    String del(Integer id,String name);

    List tableList(String name,String region,String catalog);
}
