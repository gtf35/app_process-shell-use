package shellService;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;


public class ServiceShellUtils {

    public static final String COMMAND_SU = "su"; // 获取root权限的命令
    public static final String COMMAND_SH = "sh"; // 执行sh文件的命令
    public static final String COMMAND_EXIT = "exit\n"; // 退出的命令
    public static final String COMMAND_LINE_END = "\n"; // 执行命令必须加在末尾

    private ServiceShellUtils() {
        throw new AssertionError();
    }
    //检测root状态
    public static boolean checkRootPermission() {
        return execCommand("echo root", true, false).result == 0;
    }
    //执行单行命令,实际还是调用的执行多行 ,传入命令和是否需要root
    public static ServiceShellUtils.ServiceShellCommandResult execCommand(String command, boolean isRoot) {
        return execCommand(new String[]{command}, isRoot, true);
    }
    //执行List<String>中的命令 , 传入List和是否需要root
    public static ServiceShellUtils.ServiceShellCommandResult execCommand(List<String> commands, boolean isRoot) {
        return execCommand(commands == null ? null : commands.toArray(new String[]{}), isRoot, true);
    }
    //执行多行命令
    public static ServiceShellUtils.ServiceShellCommandResult execCommand(String[] commands, boolean isRoot) {
        return execCommand(commands, isRoot, true);
    }

    public static ServiceShellUtils.ServiceShellCommandResult execCommand(String command, boolean isRoot, boolean isNeedResultMsg) {
        return execCommand(new String[]{command}, isRoot, isNeedResultMsg);
    }

    public static ServiceShellUtils.ServiceShellCommandResult execCommand(List<String> commands, boolean isRoot, boolean isNeedResultMsg) {
        return execCommand(commands == null ? null : commands.toArray(new String[]{}), isRoot, isNeedResultMsg);
    }
    //执行命令,获得返回的信息
    public static ServiceShellUtils.ServiceShellCommandResult execCommand(String[] commands, boolean isRoot, boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new ServiceShellUtils.ServiceShellCommandResult(result, null, null);
        }

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(isRoot ? COMMAND_SU : COMMAND_SH);
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) {
                    continue;
                }
                os.write(command.getBytes());
                os.writeBytes(COMMAND_LINE_END);
                os.flush();
            }
            os.writeBytes(COMMAND_EXIT);
            os.flush();

            result = process.waitFor();
            // get command result
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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
        return new ServiceShellUtils.ServiceShellCommandResult(result, successMsg == null ? null : successMsg.toString(), errorMsg == null ? null
                : errorMsg.toString());
    }
    //封装了返回信息
    public static class ServiceShellCommandResult {

        public int result;
        public String successMsg; //成功信息
        public String errorMsg; // 错误信息

        public ServiceShellCommandResult(int result) {
            this.result = result;
        }

        public ServiceShellCommandResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
    }
}
