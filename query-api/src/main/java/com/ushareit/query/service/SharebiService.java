package com.ushareit.query.service;

import com.ushareit.query.bean.Sharebi;


/**
 * @author: wangsy1
 * @create: 2022-11-30 15:22
 */
public interface SharebiService extends BaseService<Sharebi>{

    int getShare(String sharee, Integer gradeID);
}
