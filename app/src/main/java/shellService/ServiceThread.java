package shellService;

public class ServiceThread extends Thread {
    private static int ShellPORT = 4521;

    @Override
    public void run() {
        System.out.println(">>>>>>Shell服务端程序被调用<<<<<<");
        new Service(new Service.ServiceGetText() {
            @Override
            public String getText(String text) {
                if (text.startsWith("###AreYouOK")){
                    return "###IamOK#";
                }
                try{
                    ServiceShellUtils.ServiceShellCommandResult sr =  ServiceShellUtils.execCommand(text, false);
                    if (sr.result == 0){
                        return "###ShellOK#" + sr.successMsg;
                    } else {
                        return "###ShellError#" + sr.errorMsg;
                    }
                }catch (Exception e){
                    return "###CodeError#" + e.toString();
                }
            }
        }, ShellPORT);
    }
}
