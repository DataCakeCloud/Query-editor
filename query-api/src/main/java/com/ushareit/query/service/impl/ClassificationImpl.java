package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ushareit.query.bean.Classification;
import com.ushareit.query.bean.SavedQuery;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.ClassificationMapper;
import com.ushareit.query.mapper.SavedQueryMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.ClassificationService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;


/**
 * @author: wangsy1
 * @create: 2022-11-02 15:22
 */
@Slf4j
@Service
@Setter
public class ClassificationImpl extends AbstractBaseServiceImpl<Classification> implements ClassificationService {

    @Resource
    private ClassificationMapper classificationMapper;

    @Resource
    private SavedQueryMapper savedQueryMapper;

    @Override
    public CrudMapper<Classification> getBaseMapper() { return classificationMapper; }

    public void preCheckCommon(Classification classification, String name) {
        //1. name不重复校验
        String title = classification.getName();
        Integer id = classification.getId();
        Integer active = 1;
        List<String> existQuery = classificationMapper.selectByUsername(title, name, id, active);
//        super.checkOnUpdate(super.getByName(savedQuery.getTitle()), savedQuery);
        if (existQuery.contains(title)) {
            throw new ServiceException(BaseResponseCodeEnum.NAME_IS_NOT_UNIQUE);
        }
    }

    @Override
    public String edit(Integer id,String title,String name){
        LocalDateTime updateTime = LocalDateTime.now();
        boolean result = classificationMapper.edit(id,updateTime,title,name);
        return String.valueOf(result);
    }
    @Override
    public String del(Integer id,String name){
        Integer active = 0;
        LocalDateTime updateTime = LocalDateTime.now();
        boolean result = classificationMapper.del(id,updateTime,active,name);
        return String.valueOf(result);
    }

    @Override
    public void selectLevelChild(Integer id, Integer parentId){
        Integer active = 1;
        //todo：判断id的level以及id下有没有孩子；有孩子的，判断parent_id不能为一级二级；没有孩子的判断parent_id不能为二级
        //要挪动的
        Classification selectLevel = classificationMapper.selectLevel(id);
        //要挪动的有无孩子
        List<Classification> selectChild = classificationMapper.selectChild(id);
        //目标目录
        Classification selectParent = classificationMapper.selectParent(parentId);
        if (selectParent.getLevel()==2){
            throw new ServiceException(BaseResponseCodeEnum.CLI_UPDATE_DB_FAIL);
        }
        if (selectChild.size()!=0 && selectParent.getLevel()==1){
            throw new ServiceException(BaseResponseCodeEnum.CLI_UPDATE_DB_FAIL);
        }
    }

    @Override
    public String move(Integer id,Integer parendId,Integer isQuery,String name){
        LocalDateTime updateTime = LocalDateTime.now();
        String level = new String();
        //判断要移动的是否为二级目录：isQuery = 0 移动二级；isQuery = 1 移动查询
        Classification levelClass = classificationMapper.selectParent(parendId);
        if (levelClass.getLevel().equals(0)){
            level = "1";
        }
        if (levelClass.getLevel().equals(1)){
            level = "2";
        }
        if (isQuery.equals(0)){
            boolean result = classificationMapper.move(id,updateTime,parendId,name,level);
            return String.valueOf(result);
        }
        else {
            boolean result = savedQueryMapper.move(id,updateTime,parendId,name);
            return String.valueOf(result);
        }
    }

    @Override
    public List tree(String name){
        Integer isActive = 1;
        List<Classification> classiByName = classificationMapper.classiByname(name,isActive);
        classiByName.sort(Comparator.comparing(Classification::getName));
        List sourceTree = new ArrayList<>();
        List child = new ArrayList<>();
        for (int i=0;i<classiByName.size();i++){
            Map source = new HashMap();
            source.put("id",classiByName.get(i).getId());
            source.put("name",classiByName.get(i).getName());
            source.put("parent",classiByName.get(i).getParentId());
            source.put("level",classiByName.get(i).getLevel());
            source.put("child",child);
            sourceTree.add(source);
        }
        List tree = generate_tree(sourceTree,null);
        return tree;
    }

    public List generate_tree(List sourceTree,Integer parent){
        List tree = new ArrayList();
        for (int i=0;i<sourceTree.size();i++){
            Map<String,Object> item = (Map<String, Object>) sourceTree.get(i);
//            boolean a = item.get("parent").equals(parent);
            if (parent == null){
                if (item.get("parent")==parent){
                    List child = generate_tree(sourceTree,Integer.parseInt(item.get("id").toString()));
                    item.put("child",child);
                    tree.add(item);
                }
            }else {
                if (item.get("parent")!=null){
                    if (item.get("parent").equals(parent)){
                        List child = generate_tree(sourceTree,Integer.parseInt(item.get("id").toString()));
                        item.put("child",child);
                        tree.add(item);
                    }
                }
            }
        }
        return tree;
    }

    @Override
    public void addFirstLevel(String name){
        Integer active = 1;
        Integer level = 0;
        String title = "全部文件夹";
        Integer parent = null;
        LocalDateTime createTime = LocalDateTime.now();
        List<Classification> selectByName = classificationMapper.selectByNameTitle(name,active,level,title);
        if (selectByName.size()==0){
            boolean addLevelFirst = classificationMapper.addLevelFirst(name,active,level,title,createTime,parent);
        }
    }
}
