package com.ushareit.query.service.impl;

import com.ushareit.query.bean.ShareGrade;
import com.ushareit.query.bean.Sharebi;
import com.ushareit.query.mapper.SharebiMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.SharebiService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
@Setter
public class SharebiServiceImpl extends AbstractBaseServiceImpl<Sharebi> implements SharebiService {

    @Resource
    private SharebiMapper sharebiMapper;

    @Override
    public CrudMapper<Sharebi> getBaseMapper() { return sharebiMapper; }

    @Override
    public int getShare(String sharee, Integer gradeID){
        Sharebi sg = sharebiMapper.selectByGradeId(gradeID,sharee);
        if (null == sg) {
            return -1;
        }

        return sg.getGrade();
    }
}
