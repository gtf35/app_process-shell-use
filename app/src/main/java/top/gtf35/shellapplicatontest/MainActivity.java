package top.gtf35.shellapplicatontest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText mCmdInputEt;
    private Button mRunShellBtn;
    private TextView mOutputTv;

    private void initView(){
        mCmdInputEt = findViewById(R.id.et_cmd);
        mRunShellBtn = findViewById(R.id.btn_runshell);
        mOutputTv = findViewById(R.id.tv_output);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mRunShellBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cmd = mCmdInputEt.getText().toString();
                if (TextUtils.isEmpty(cmd)) {
                    Toast.makeText(MainActivity.this, "输入内容为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                runShell(cmd);
            }
        });
    }

    private void runShell(final String cmd){
        if (TextUtils.isEmpty(cmd)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
              new SocketClient(cmd, new SocketClient.onServiceSend() {
                  @Override
                  public void getSend(String result) {
                      showTextOnTextView(result);
                  }
              });
            }
        }).start();
    }

    private void showTextOnTextView(final String text){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(mOutputTv.getText())) {
                    mOutputTv.setText(text);
                } else {
                    mOutputTv.setText(mOutputTv.getText() + "\n" + text);
                }
            }
        });
    }
}
