package com.ushareit.query.service;


import com.ushareit.query.bean.AIChat;
import com.ushareit.query.bean.CurrentUser;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface AIChatService extends BaseService<AIChat> {
    Map<String, Object> chat(String content, String uuid, String user);

    Map<String, Object> tableInfo(List<Map<String, String>> tables, String uuid,
                                  String user, String tenantName,
                                  CurrentUser currentUser, String token) throws ParseException;
}
