package com.ushareit.query.web.controller;

import com.alibaba.fastjson.JSON;
import com.ushareit.query.bean.AIChat;
import com.ushareit.query.service.BaseService;
import com.ushareit.query.service.AIChatService;
import com.ushareit.query.web.vo.BaseResponse;
import io.swagger.annotations.Api;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "ai chat相关")
@RestController
@Setter
@RequestMapping("/aiService")
public class AIChatController extends BaseBusinessController<AIChat> {

    @Autowired
    private AIChatService aiChatService;

    @Override
    public BaseService<AIChat> getBaseService() { return aiChatService; }

    @PostMapping("/chat")
    public BaseResponse chat(@RequestBody @Valid String params) {
        String name = getCurrentUser().getUserName();
        HashMap<String, String> map = JSON.parseObject(params, HashMap.class);
        String content = map.get("content");
        String uuid = map.get("uuid");
        Map<String, Object> reply = aiChatService.chat(content, uuid, name);
        return BaseResponse.success(reply);
    }


    @PostMapping("/tableInfo")
    public BaseResponse tableInfo(@RequestHeader("Authentication") String token,
                                  @RequestBody @Valid String params) throws ParseException {
        String name = getCurrentUser().getUserName();
        String tenantName = getCurrentUser().getTenantName();
        HashMap<String, Object> map = JSON.parseObject(params, HashMap.class);
        List<Map<String, String>> tables = JSON.parseObject(map.get("info").toString(), List.class);
        String uuid = map.get("uuid").toString();
        Map<String, Object> reply = aiChatService.tableInfo(tables, uuid, name, tenantName,
                getCurrentUser(), token);
        if (null != reply.get("code") && reply.get("code").toString().equals("404")) {
            return BaseResponse.error(reply.get("code").toString(),
                       reply.get("message").toString(),
                       "");
        }
        return BaseResponse.success(reply);
    }
}
