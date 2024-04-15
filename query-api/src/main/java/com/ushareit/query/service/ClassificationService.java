package com.ushareit.query.service;

import com.github.pagehelper.PageInfo;
import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.SavedQuery;

import java.util.List;
import java.util.Map;

/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
public interface ClassificationService extends BaseService<Classification> {

    /**重复校验
     *
     */
    void preCheckCommon(Classification classification, String name);

    String edit(Integer id,String title,String name);

    String del(Integer id,String name);

    void selectLevelChild(Integer id, Integer parentId);

    String move(Integer id,Integer parendId,Integer isQuery,String name);

    List tree(String name);

    void addFirstLevel(String name);
}
