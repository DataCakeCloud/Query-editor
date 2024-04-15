package com.ushareit.query.service;

import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.ClassificationDash;

public interface ClassificationDashService extends BaseService<ClassificationDash>{

    void preCheckCommon(ClassificationDash classificationDash, String name);

    void edit(Integer id,String title,String name,String query);

    String delete(Integer id, String name);

    void move(Integer id,Integer parendId,Integer isQuery,String name);

    String selectLevelChild(Integer id, Integer parentId);
}
