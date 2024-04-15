package com.ushareit.query.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author licg Shell 工具类
 * @date 2020/05/11
 */
@Slf4j
public final class ShellUtil {
    private static final String LINE_SEP = System.getProperty("line.separator");

    private ShellUtil() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    /**
     * 执行命令
     *
     * @param command 命令
     * @return CommandResult
     */
    public static CommandResult execCmd(String command) {
        return execCmd(new String[]{command}, true);
    }

    /**
     * 执行命令
     *
     * @param command 命令
     * @param isRoot  是否需要 root 权限执行
     * @return CommandResult
     */
    public static CommandResult execCmd(String command, boolean isRoot) {
        return execCmd(new String[]{command}, isRoot, true);
    }

    /**
     * 执行命令
     *
     * @param commands 多条命令链表
     * @param isRoot   是否需要 root 权限执行
     * @return CommandResult
     */
    public static CommandResult execCmd(List<String> commands, boolean isRoot) {
        return execCmd(commands == null ? null : commands.toArray(new String[]{}), isRoot, true);
    }

    /**
     * 执行命令
     *
     * @param commands 多条命令数组
     * @param isRoot   是否需要 root 权限执行
     * @return CommandResult
     */
    public static CommandResult execCmd(String[] commands, boolean isRoot) {
        return execCmd(commands, isRoot, true);
    }

    /**
     * 执行命令
     *
     * @param command         命令
     * @param isRoot          是否需要 root 权限执行
     * @param isNeedResultMsg 是否需要结果消息
     * @return CommandResult
     */
    public static CommandResult execCmd(String command, boolean isRoot, boolean isNeedResultMsg) {
        return execCmd(new String[]{command}, isRoot, isNeedResultMsg);
    }

    /**
     * 执行命令
     *
     * @param commands        命令链表
     * @param isRoot          是否需要 root 权限执行
     * @param isNeedResultMsg 是否需要结果消息
     * @return CommandResult
     */
    public static CommandResult execCmd(List<String> commands, boolean isRoot, boolean isNeedResultMsg) {
        return execCmd(commands == null ? null : commands.toArray(new String[]{}), isRoot, isNeedResultMsg);
    }

    /**
     * 执行命令
     *
     * @param commands        命令数组
     * @param isRoot          是否需要 root 权限执行
     * @param isNeedResultMsg 是否需要结果消息
     * @return CommandResult
     */
    public static CommandResult execCmd(final String[] commands, final boolean isRoot, final boolean isNeedResultMsg) {
        Long startTime = System.currentTimeMillis();

        Boolean result = false;
        if (commands == null || commands.length == 0) {
            return new CommandResult(result, null, null);
        }
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(isRoot ? "sudo sh" : "sh");
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (StringUtils.isBlank(command)) {
                    continue;
                }
                os.write(command.getBytes());
                os.writeBytes(LINE_SEP);
                os.flush();
            }
            os.writeBytes("exit" + LINE_SEP);
            os.flush();
            result = process.waitFor(5, TimeUnit.MINUTES);
            log.info("result:" + result);
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
                String line;
                if ((line = successResult.readLine()) != null) {
                    successMsg.append(line);
                    while ((line = successResult.readLine()) != null) {
                        successMsg.append(LINE_SEP).append(line);
                    }
                }
                log.info("successMsg:" + successMsg);
                if ((line = errorResult.readLine()) != null) {
                    errorMsg.append(line);
                    while ((line = errorResult.readLine()) != null) {
                        errorMsg.append(LINE_SEP).append(line);
                    }
                }
                log.info("errorMsg:" + errorMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(os, successResult, errorResult, process);
            Long endTime = System.currentTimeMillis();
            log.info(String.format("%s 接口耗时%s ms", "execCmd", endTime - startTime));
        }

        return new CommandResult(
                result,
                successMsg == null ? null : successMsg.toString(),
                errorMsg == null ? null : errorMsg.toString()
        );
    }

    private static void close(Closeable os, Closeable successResult, Closeable errorResult, Process process) {
        try {
            if (os != null) {
                os.close();
            }
            if (successResult != null) {
                successResult.close();
            }
            if (errorResult != null) {
                errorResult.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (process != null) {
            process.destroy();
        }
    }

    /**
     * 返回的命令结果
     */
    public static class CommandResult {
        /**
         * 结果码
         **/
        public Boolean result;
        /**
         * 成功信息
         **/
        public String successMsg;
        /**
         * 错误信息
         **/
        public String errorMsg;

        public CommandResult(Boolean result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
    }
}