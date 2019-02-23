package top.gtf35.shellapplicatontest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;


public class SocketClient {

    private String TAG = "SocketClient";

    private String HOST = "127.0.0.1";
    PrintWriter printWriter;//发送用的
    onServiceSend mOnServiceSend;
    String cmd;
    BufferedReader bufferedReader;
    int port = 4521;

    public SocketClient(String commod, onServiceSend onServiceSend) {
        cmd = commod;
        mOnServiceSend = onServiceSend;
        try {
            Log.d(TAG, "与service进行socket通讯,地址=" + HOST + ":" + port);
            /** 创建Socket*/
            // 创建一个流套接字并将其连接到指定 IP 地址的指定端口号(本处是本机)
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(HOST, port), 3000);//设置连接请求超时时间3 s
            // 接收3s超时
            socket.setSoTimeout(3000);
            Log.d(TAG, "与service进行socket通讯,超时为：" + 3000);
            /** 发送客户端准备传输的信息 */
            // 由Socket对象得到输出流，并构造PrintWriter对象
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            /** 用于获取服务端传输来的信息 */
            // 由Socket对象得到输入流，并构造相应的BufferedReader对象
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new CreateServerThread(socket);
            send(cmd);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "与service进行socket通讯发生错误" + e);
            mOnServiceSend.getSend("###ShellRunError:" + e.toString());
        }
    }

    //线程类
    class CreateServerThread extends Thread {
        Socket socket;
        InputStreamReader inputStreamReader;
        BufferedReader reader;
        public CreateServerThread(Socket s) throws IOException {
            Log.d(TAG, "创建了一个新的连接线程");
            socket = s;
            start();
        }

        public void run() {
            try {
                // 打印读入一字符串并回调
                try {
                    inputStreamReader = new InputStreamReader(socket.getInputStream());
                    reader = new BufferedReader(inputStreamReader);
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        if (line != null)mOnServiceSend.getSend(line);
                    }
                    Log.d(TAG, "客户端接收解析服务器的while循环结束");
                } catch (Exception e){
                    e.printStackTrace();
                    Log.d(TAG, "客户端接收解析服务器的Threadcatch块执行：" + e.toString());
                }
                bufferedReader.close();
                printWriter.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "socket 接收线程发生错误：" + e.toString());
            }
        }
    }


    public void send(String cmd){
        printWriter.println(cmd );
        // 刷新输出流，使Server马上收到该字符串
        printWriter.flush();
    }

    public interface onServiceSend{
        void getSend(String result);
    }
}


