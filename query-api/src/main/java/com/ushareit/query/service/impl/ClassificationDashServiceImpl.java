package com.ushareit.query.service.impl;

import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.ClassificationDash;
import com.ushareit.query.bean.Dashboard;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.ClassificationDashMapper;
import com.ushareit.query.mapper.DashboardMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.ClassificationDashService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: wangsy1
 * @create: 2022-11-25 15:22
 */
@Slf4j
@Service
@Setter
public class ClassificationDashServiceImpl extends AbstractBaseServiceImpl<ClassificationDash> implements ClassificationDashService {

    @Resource
    private ClassificationDashMapper classificationDashMapper;

    @Resource
    private DashboardMapper dashboardMapper;

    @Override
    public CrudMapper<ClassificationDash> getBaseMapper() { return classificationDashMapper; }

    public void preCheckCommon(ClassificationDash classificationDash, String name) {
        //1. name不重复校验
        String title = classificationDash.getName();
        Integer id = classificationDash.getId();
        Integer active = 1;
        List<String> existQuery = classificationDashMapper.selectByUsername(title, name, id, active);
//        super.checkOnUpdate(super.getByName(savedQuery.getTitle()), savedQuery);
        if (existQuery.contains(title)) {
            throw new ServiceException(BaseResponseCodeEnum.NAME_IS_NOT_UNIQUE);
        }
    }

    @Override
    public void edit(Integer id,String title,String name,String query){
        LocalDateTime updateTime = LocalDateTime.now();
        if (query.equals("0")){
            classificationDashMapper.edit(id,updateTime,title,name);
        }
        if (query.equals("1")){
            dashboardMapper.editName(id,updateTime,title,name);
        }
    }

    @Override
    public String delete(Integer id, String name){
        LocalDateTime updateTime = LocalDateTime.now();
        //判断这个文件夹下是否有看板，没有看板才可以删除
        Integer level = classificationDashMapper.getLevel(id,1);
        if (level.equals(2)){
            List<Dashboard> dashboard = dashboardMapper.selectDashByClassid(id,1);
            if (dashboard.size()!=0){
                return "该文件夹下存在看板，不可删除";
            }else{
                classificationDashMapper.deleteById(id,0,updateTime);
            }
        }
        if (level.equals(1)){
            List<ClassificationDash> child = classificationDashMapper.selectChild(id,1);
            if (child.size()!=0){
                return "该文件夹下存在其他文件夹，不可删除";
            }else {
                List<Dashboard> dashboard = dashboardMapper.selectDashByClassid(id,1);
                if (dashboard.size()!=0){
                    return "该文件夹下存在看板，不可删除";
                }else{
                    classificationDashMapper.deleteById(id,0,updateTime);
                }
            }
        }
        return "success";
    }

    @Override
    public void move(Integer id,Integer parendId,Integer isQuery,String name){
        try {
            LocalDateTime updateTime = LocalDateTime.now();
            String level = new String();
            ClassificationDash levelClass = classificationDashMapper.selectParent(parendId);
            if (levelClass.getLevel().equals(0)){
                level = "1";
            }
            if (levelClass.getLevel().equals(1)){
                level = "2";
            }
            if (isQuery.equals(1)){
                dashboardMapper.moveClassId(id, parendId, name, updateTime);
            }
            //挪文件夹
            if (isQuery.equals(0)){
                classificationDashMapper.move(id,updateTime,parendId,name,level);
            }
        }catch (Exception e){
            throw new ServiceException(BaseResponseCodeEnum.valueOf("移动目录失败"));
        }
    }

    @Override
    public String selectLevelChild(Integer id, Integer parentId){
        //parent_id的level不能是2级
        //parent_id的level如果是1级，判断id的level如果是一级不可以有二级孩子
        ClassificationDash selectParent = classificationDashMapper.selectParent(parentId);
        if (selectParent.getLevel()==2){
            return "目标移动目录不能为二级";
        }
        if (selectParent.getLevel()==1){
            ClassificationDash selectLevel = classificationDashMapper.selectParent(id);
            List<ClassificationDash> selectChild = classificationDashMapper.selectChild(id,1);
            if (selectLevel.getLevel()==1 && selectChild.size()!=0){
                return "该目录下有二级目录，不可往一级目录下移动";
            }
        }
        return "success";
    }
}
