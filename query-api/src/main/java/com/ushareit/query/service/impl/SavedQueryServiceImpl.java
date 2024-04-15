package com.ushareit.query.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import com.ushareit.query.bean.Meta;
import com.ushareit.query.bean.SavedQuery;
import com.ushareit.query.bean.ShareGrade;
import com.ushareit.query.constant.BaseResponseCodeEnum;
import com.ushareit.query.exception.ServiceException;
import com.ushareit.query.mapper.SavedQueryMapper;
import com.ushareit.query.mapper.ShareGradeMapper;
import com.ushareit.query.mapper.MetaMapper;
import com.ushareit.query.repositry.mapper.CrudMapper;
import com.ushareit.query.service.SavedQueryService;
import com.ushareit.query.web.utils.CommonUtil;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class SavedQueryServiceImpl extends AbstractBaseServiceImpl<SavedQuery> implements SavedQueryService {

    @Value("${share_email.host}")
    private String emailServerHost;

    @Value("${share_email.protocol}")
    private String emailPro;

    @Value("${share_email.username}")
    private String emailNickName;

    @Value("${share_email.email}")
    private String emailName;

    @Value("${share_email.password}")
    private String emailPass;

    @Resource
    private SavedQueryMapper savedQueryMapper;

    @Resource
    private MetaMapper metaMapper;

    @Resource
    private ShareGradeMapper sgMapper;

    @Override
    public CrudMapper<SavedQuery> getBaseMapper() { return savedQueryMapper; }

    @Override
    public PageInfo<SavedQuery> getSavedQuery(int pageNum, int pageSize,
            String title,
            String query_sql,
            String engine,
            Integer folderID,
            String region,
    		String info, String name, String userGroup) {
        PageHelper.startPage(pageNum, pageSize);
        List<SavedQuery> pageRecord = null;
        if (null != title && title.trim().length() > 0 ||
        		null != query_sql && query_sql.trim().length() > 0 ||
        		null != engine && engine.trim().length() > 0 ||
        		null != folderID) {
        	pageRecord = savedQueryMapper.listByTitleSqlEngineFolder(title,
        			query_sql, engine, folderID, region, userGroup, name);
        } else {
        	pageRecord = savedQueryMapper.listByInfo(info, name);
        }
        return new PageInfo<>(pageRecord);
    }

    @Override
    public Map<String, Object> setParam(PageInfo<SavedQuery> savedQueryList) {
        ArrayList<Object> savedQuery = new ArrayList<>();
        List<SavedQuery> pageList = savedQueryList.getList();
        Map<String, Object> pageObject = JSON.parseObject(JSON.toJSONString(savedQueryList));

        for (int i = 0; i < pageList.size(); i++) {
            Map<String, Object > data = JSON.parseObject(JSON.toJSONString(pageList.get(i)));
            if (data.get("param") != null) {
                data.put("param", JSONObject.parseObject(data.get("param").toString()));
            }
            savedQuery.add(data);
        }
        pageObject.put("list", savedQuery);
        return pageObject;
    }

    @Override
    public Object save(SavedQuery savedQuery) {
        //1. 校验用户名是否唯一
//        preCheckCommon(savedQuery);
        //2. 保存用户配置
        String engine_key = savedQuery.getEngine();
        if (null != engine_key && engine_key.trim().length() > 0) {
            String engine_label = engine_key;
            if (engine_label.startsWith("presto")) {
                engine_label = "Ares";
            }
            Meta meta = metaMapper.listByKey(engine_key);
            if (null != meta) {
                engine_label = meta.getEngineName();  // 获取引擎标签
            }
            savedQuery.setEngineZh(engine_label);
            // savedQuery.setRegion(meta.getRegion());
        }
        super.save(savedQuery);

        return savedQuery;
    }

    @Override
    public void update(SavedQuery savedQuery) {
        //1. 校验用户名是否唯一
//        preCheckCommon(testFromWeb);
        //2. 保存用户配置
        String engine_key = savedQuery.getEngine();
        if (null != engine_key && engine_key.trim().length() > 0) {
            String engine_label = engine_key;
            if (engine_label.startsWith("presto")) {
                engine_label = "Ares";
            }
            Meta meta = metaMapper.listByKey(engine_key);
            if (null != meta) {
                engine_label = meta.getEngineName();  // 获取引擎标签
            }
            savedQuery.setEngineZh(engine_label);
            // savedQuery.setRegion(meta.getRegion());
        }
        Integer id = savedQuery.getId();
        SavedQuery existSavedQuery = savedQueryMapper.selectById(id);
        if (existSavedQuery != null) {
            super.update(savedQuery);
        } else {
            savedQuery.setCreateBy(savedQuery.getUpdateBy());
            savedQuery.setCreateTime(savedQuery.getUpdateTime());
            super.save(savedQuery);
        }
    }

    public void preCheckCommon(SavedQuery savedQuery, String name) {
        //1. name不重复校验
        String title = savedQuery.getTitle();
        Integer id = savedQuery.getId();
        List<String> existQuery = savedQueryMapper.selectByUsername(title, name, id);
//        super.checkOnUpdate(super.getByName(savedQuery.getTitle()), savedQuery);
        if (existQuery.contains(title)) {
            throw new ServiceException(BaseResponseCodeEnum.NAME_IS_NOT_UNIQUE);
        }
    }

    public String deleteBatch(String id) {
        String delimeter = ",";
        String[] idList = id.split(delimeter);
        for(int i =0; i < idList.length ; i++){
            super.delete(idList[i]);
        }
        return "success";
    }
    
    @Override
    public int addShareGrade(ShareGrade sg, String shareeEmail) {
    	try {
        	sgMapper.insertUseGeneratedKeys(sg);
        	int gradeID = sg.getId();
        	
    		String emailHost = emailServerHost;       //发送邮件的主机
    	    String transportType = emailPro;           //邮件发送的协议
    	    String fromUser = emailNickName;           //发件人名称
    	    String fromEmail = emailName;  //发件人邮箱
    	    String authCode = emailPass;    //发件人邮箱授权码
    	    // String toEmail = sg.getSharee() + "@ushareit.com";   //收件人邮箱
    	    String subject = sg.getSharer() + "通过Datacake平台分享给您一条数据查询";           //主题信息
    		//初始化默认参数
            Properties props = new Properties();
            props.setProperty("mail.transport.protocol", transportType);
            props.setProperty("mail.host", emailHost);
            props.setProperty("mail.user", fromUser);
            props.setProperty("mail.from", fromEmail);
            //获取Session对象
            Session session = Session.getInstance(props, null);
            //开启后有调试信息
            session.setDebug(true);

            //通过MimeMessage来创建Message接口的子类
            MimeMessage message = new MimeMessage(session);
            //下面是对邮件的基本设置
            //设置发件人：
            //设置发件人第一种方式：直接显示：antladdie <antladdie@163.com>
            //InternetAddress from = new InternetAddress(sender_username);
            //设置发件人第二种方式：发件人信息拼接显示：蚂蚁小哥 <antladdie@163.com>
            String formName = MimeUtility.encodeWord(fromUser) + " <" + fromEmail + ">";
            InternetAddress from = new InternetAddress(formName);
            message.setFrom(from);

            //设置收件人：
            InternetAddress to = new InternetAddress(shareeEmail);
            message.setRecipient(Message.RecipientType.TO, to);

            //设置邮件主题
            message.setSubject(subject);

            //设置邮件内容,这里我使用html格式，其实也可以使用纯文本；纯文本"text/plain"
            String link = sg.getShareUrl() + "&gradeID=" + String.valueOf(gradeID);
            String content = sg.getSharer() + "通过Datacake平台分享给您一条数据查询 ，点击链接即可快速访问";
            content += "<a href=\"" + link + "\">" + link + "</a>";
            message.setContent(content, "text/html;charset=UTF-8");

            //保存上面设置的邮件内容
            message.saveChanges();

            //获取Transport对象
            Transport transport = session.getTransport();
            //smtp验证，就是你用来发邮件的邮箱用户名密码（若在之前的properties中指定默认值，这里可以不用再次设置）
            transport.connect(emailHost, fromEmail, authCode);
            //发送邮件
            transport.sendMessage(message, message.getAllRecipients()); // 发送
            transport.close();
    	} catch (Exception e) {
            log.error(String.format("There is a stack err when %s share to %s : %s",
            		sg.getSharer(), sg.getSharee(), CommonUtil.printStackTraceToString(e)));
            return -1;
    	}
    	return sg.getId();
    }
}


