package com.ushareit.query.constant;

import java.text.MessageFormat;

/**
 * 公共域 服务响应码
 *
 * @author wuyan
 * @date 2020/05/14
 */
public enum BaseResponseCodeEnum {
    /**
     * 系统类响应
     */
    SYS_ERR("系统错误"),
    SYS_DB_CONN("数据库连接失败"),
    SYS_UNA("服务不可用"),
    SYS_DEGRADE("服务降级"),

    /**
     * 客户端类common响应
     */
    CLI_PARAM_ILLEGAL("参数非法"),
    CLI_ID_NOTNULL("ID不能为空"),
    CLI_DELETE_ILLEGAL("无效的删除对象，请继承DeleteEntity"),

    CLI_SAVE_DB_SUCCESS("数据库保存成功"),
    CLI_UPDATE_DB_SUCCESS("数据库更新成功"),
    CLI_SAVE_DB_FAIL("数据库保存失败"),
    CLI_UPDATE_DB_FAIL("数据库更新失败"),
    CLI_PARAM_REQUIRED("参数不能为空"),

    DATA_NOT_FOUND("数据不存在"),
    USER_OR_PASSWORD_ERROR("用户名或密码错误"),
    SQL_DECODE_FAIL("SQL解码失败"),
    NOT_ADMIN("非管理员角色，不允许操作"),
    DELETE_FAIL("删除失败"),


    SUCCESS("成功"),

    REQUEST_ILLEGAL("非法请求"),
    NAME_IS_NOT_UNIQUE("名字重复，请重新更换"),
    NAME_NOT_MATCH("名字不匹配【字母开头，只包含a-z,A-Z,0-9或_或-】，长度2-64"),
    NAME_BLANK("名字不能为空"),
    STATE_PATH_NOT_MATCH("state path不匹配，请参考示例"),

    /**
     * 登录类型
     */
    NO_AUTH("无法更新数据库，租户校验失败"),
    NO_LOGIN("未登录或登录过期，请先登录"),
    NO_RIGHT("无权限，请检查用户所在组"),
    NO_STARTED_JOB("数据库中不存在started的job"),

    /**
     * 集群类响应
     */
    CLUSTER_NOT_AVAIL("集群不可用，请联系管理员"),
    CLUSTER_AVAIL_SLOT_NOT_ENOUGH("集群可用资源量不足"),
    ARTIFACT_TYPE_NOT_MATCH("工件类型不匹配"),
    ARTIFACT_MODE_NOT_MATCH("工件模式不匹配"),
    CLUSTER_TYPE_NOT_MATCH("集群类型不匹配"),
    CLUSTER_ADDRESS_NOT_MATCH("集群地址不匹配"),

    /**
     * 应用类响应
     */
    APP_START_SUCCESS("启动应用成功"),
    APP_START_FAIL("启动任务失败"),
    TASK_CHECK_FAIL("校验任务失败"),
    APP_IS_CANCELING_OR_SUSPENDING("当前应用已触发取消或停止请求"),
    APP_IS_DELETE("当前应用已被删除"),
    APP_IS_TERMINAL("当前应用处于最终状态"),
    APP_IS_RUNNING("当前应用处于RUNNING状态"),
    APP_HAS_ALIVE_JOB("数据库中存在非最终状态的job"),
    APP_SAVE_SUCCESS("应用创建成功"),
    APP_DELETE_SUCCESS("应用删除成功"),
    APP_CANCEL_SUCCESS("应用终止成功"),
    APP_UPDATE_SUCCESS("应用更新成功"),
    APP_TAG_SUCCESS("应用打标签成功"),
    APP_RELEASE_SUCCESS("启动发布成功"),
    MAIN_CLASS_NOT_MATCH("主类不匹配"),
    APP_TYPE_NOT_MATCH("应用类型不匹配"),
    APP_IAM_NO_RIGHT("应用IAM账号权限不足，请联系运维确认"),
    APP_CHECK_NO_RIGHT("应用内容校验未通过，请检查"),
    APP_DEBUG_NO_RIGHT("应用内容调试失败，请检查"),
    APP_DEBUG_NO_RESULT("应用内容调试失败，无返回值"),
    NO_DEBUG("无debug任务"),
    APP_STOPDEBUG_SUCCESS("应用终止调试成功"),
    APP_CLUSTER_VERSION_NO_MATCH("Session独享模式1.11.0版本以上，才支持Sql任务！"),
    APP_ARTIFACTID_ARTIVERSIONID_NO_MATCH("工件ID和工件版本ID未一一对应"),
    GET_TABLES_FAIL("获取sql中source和sink表失败"),
    SINK_NOT_EXIST("sink不存在"),
    TASK_IS_NOT_STREAMING_MODE("非流模式不支持取消"),
    /**
     * Job类响应
     */
    ONLINE_AND_OFFLINE_FAIL("上下线离线任务失败"),
    TASK_INSTANCE_STOP_FAIL("离线任务停止失败"),
    JOB_STOP_WITH_SAVEPOINT_FAIL("停止并触发保存点失败"),


    /**
     * Template类响应
     */
    TEMPLATE_DELETE_SUCCESS("模板删除成功"),
    TEMPLATE_GROUP_NOTNULL("group不能为空"),

    /**
     * OBS类响应
     */
    OBS_COPY_FAIL("从obs上复制jar包失败"),
    OBS_DELETE_FAIL("OBS上删除对象失败"),
    OBS_DOWNLOAD_FAIL("从OBS下载失败"),
    OBS_UPLOAD_FAIL("更新jar包到OBS失败"),
    OBS_CREATED_DIR_FAIL("OBS上创建目录失败"),

    /**
     * AWS类响应
     */
    AWS_DELETE_FAIL("AWS上删除对象失败"),
    AWS_DOWNLOAD_FAIL("从AWS下载失败"),
    AWS_UPLOAD_FAIL("更新文件到AWS失败"),
    AWS_CREATED_DIR_FAIL("AWS上创建目录失败"),
    AWS_URL_NOT_EXIST("AWS路径不存在"),
    AWS_FILE_CONVERSION_FAIL("AWS文件转化失败"),

    /**
     * statistic响应类
     */
    TIMESTAMP_TO_DATE_FAIl("时间戳转换成date失败"),

    /**
     * rest api 相应码
     */
    CREATED("已创建"),
    DELETED ("已删除"),
    UPDATED_ALL("已更新-ALL"),
    UPDATED("已更新"),
    QUERY("查询已返回"),
    UNKOWN("未识别访问类型"),

    FAILED_TO_CLOSE_CONNECTION("关闭连接失败"),

    /**
     * compile
     */
    FILE_COMPILE_FAIL("文件编译失败"),

    /**
     * scheduler
     */
    SCHEDULER_RESQUEST_FAIL("请求调度模块失败"),
    SCHEDULER_RESPONSE_ERR("调度模块响应错误"),

    /**
     * ui
     */
    UI_GET_FAIL("ui服务尚未启动，请稍后再试")
    ;

    private String message;

    BaseResponseCodeEnum(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return MessageFormat.format("ResponseCode:{0},{1}.", this.name(), this.message);
    }

}
