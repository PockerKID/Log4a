package me.pqpo.log4a;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.pqpo.librarylog4a.Log4a;
import me.pqpo.librarylog4a.logger.AppenderLogger;
import me.pqpo.librarylog4a.appender.AbsAppender;
import me.pqpo.librarylog4a.appender.AndroidAppender;
import me.pqpo.librarylog4a.appender.Appender;
import me.pqpo.librarylog4a.appender.FileAppender;
import me.pqpo.librarylog4a.formatter.Formatter;
import me.pqpo.librarylog4a.logger.Logger;
import me.pqpo.log4a.append.BufferFileAppender;
import me.pqpo.log4a.append.NoBufferFileAppender;
import me.pqpo.log4a.append.NoBufferInThreadFileAppender;

import static me.pqpo.log4a.LogInit.BUFFER_SIZE;

import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    EditText etContent;
    EditText etThread;
    Button btnWrite;
    Button btnTest;
    TextView tvTest;
    EditText etTimes;

    boolean testing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etThread = findViewById(R.id.et_thread);
        etContent = findViewById(R.id.et_content);
        btnWrite = findViewById(R.id.btn_write);
        btnTest = findViewById(R.id.btn_test);
        tvTest = findViewById(R.id.tv_test);
        etTimes = findViewById(R.id.et_times);

        checkPermissions();

    }

    private void performanceTest(int times) {
        tvTest.append(String.format(Locale.getDefault(),
                "## prints %d logs:\n", times));
        Log4a.release();
        AppenderLogger logger = new AppenderLogger.Builder().create();
        Log4a.setLogger(logger);

        logger.addAppender(createLog4aFileAppender());
        doPerformanceTest("log4a", times);
        Log4a.release();

        Log4a.setLogger(logger);
        logger.addAppender(createAndroidLogAppender());
        doPerformanceTest("android log", times);
        Log4a.release();

        Log4a.setLogger(logger);
        List<String> buffer = new ArrayList<>(times);
        logger.addAppender(createMemAppender(buffer));
        doPerformanceTest("array list log", times);
        buffer.clear();
        Log4a.release();

        Log4a.setLogger(logger);
        logger.addAppender(createNoBufferFileAppender());
        doPerformanceTest("file log(no buffer)", times);
        Log4a.release();

        Log4a.setLogger(logger);
        logger.addAppender(createWithBufferFileAppender());
        doPerformanceTest("file log(with buffer)", times);
        Log4a.release();

        Log4a.setLogger(logger);
        logger.addAppender(createNoBufferInThreadFileAppender());
        doPerformanceTest("file log(no buffer in thread)", times);
        tvTest.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log4a.release();
            }
        }, 1000);

        LogInit.init(this);
        tvTest.append("## end");
    }

    private Appender createNoBufferFileAppender() {
        File logFile = new File(FileUtils.getLogDir(this), "logNoBufferFileTest.txt");
        logFile.delete();
        return new NoBufferFileAppender(logFile);
    }

    private Appender createWithBufferFileAppender() {
        File logFile = new File(FileUtils.getLogDir(this), "logBufferFileTest.txt");
        logFile.delete();
        return new BufferFileAppender(logFile, BUFFER_SIZE);
    }

    private Appender createNoBufferInThreadFileAppender() {
        File logFile = new File(FileUtils.getLogDir(this), "logNoBufferInThreadFileTest.txt");
        logFile.delete();
        return new NoBufferInThreadFileAppender(logFile);
    }

    private Appender createMemAppender(final List<String> buffer) {
        return new AbsAppender() {
            @Override
            protected void doAppend(int logLevel, String tag, String msg) {
                buffer.add(msg);
            }
        };
    }

    private Appender createLog4aFileAppender() {
        File log = FileUtils.getLogDir(this);
        File cacheFile = new File(log, "test.logCache");
        File logFile = new File(log, "log4aTest.txt");
        cacheFile.delete();
        logFile.delete();
        FileAppender.Builder fileBuild = new FileAppender.Builder(this)
                .setLogFilePath(logFile.getAbsolutePath())
                .setBufferSize(BUFFER_SIZE)
                .setFormatter(new Formatter() {
                    @Override
                    public String format(int logLevel, String tag, String msg) {
                        return msg;
                    }
                })
                .setBufferFilePath(cacheFile.getAbsolutePath());
        return fileBuild.create();
    }

    private Appender createAndroidLogAppender() {
        return new AndroidAppender.Builder().create();
    }

    private void doPerformanceTest(String testName, int times) {
        long currentTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < times; i++) {
            Log4a.i(TAG, "log-" + i);
        }
        tvTest.append(String.format(Locale.getDefault(),
                "* %s spend: %d ms\n",
                testName,
                System.currentTimeMillis() - currentTimeMillis));
    }

    public String getLogPath() {
        String logPath = "";
        Logger logger = Log4a.getLogger();
        if (logger instanceof AppenderLogger) {
            List<Appender> appenderList = ((AppenderLogger) logger).getAppenderList();
            for (Appender appender : appenderList) {
                if (appender instanceof FileAppender) {
                    FileAppender fileAppender = (FileAppender) appender;
                    logPath = fileAppender.getLogPath();
                    break;
                }
            }
        }
        return logPath;
    }

    public void changeLogPath() {
        Logger logger = Log4a.getLogger();
        if (logger instanceof AppenderLogger) {
            List<Appender> appenderList = ((AppenderLogger) logger).getAppenderList();
            for (Appender appender : appenderList) {
                if (appender instanceof FileAppender) {
                    FileAppender fileAppender = (FileAppender) appender;
                    File log = FileUtils.getLogDir(this);
                    String time = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date());
                    String logPath = new File(log, time + "-" + System.currentTimeMillis() + ".txt").getAbsolutePath();
                    fileAppender.changeLogPath(logPath);
                    break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log4a.flush();
    }

    private void checkPermissions() {
        if (XXPermissions.isGranted(this, Permission.MANAGE_EXTERNAL_STORAGE)) {
            init();
        } else {
            XXPermissions.with(this).permission(Permission.MANAGE_EXTERNAL_STORAGE)
                    .request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
                                Toast.makeText(getApplicationContext(), "获取存储权限成功", Toast.LENGTH_SHORT).show();
                                init();
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                Toast.makeText(getApplicationContext(), "被永久拒绝授权，请手动授予存储权限", Toast.LENGTH_SHORT).show();
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(MainActivity.this);
                            } else {
                                Toast.makeText(getApplicationContext(), "获取存储权限失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void init(){
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (testing) {
                    Toast.makeText(getApplicationContext(), "testing", Toast.LENGTH_SHORT).show();
                    return;
                }
                int threads = Integer.valueOf(etThread.getText().toString());
                if (threads > 500) {
                    Toast.makeText(getApplicationContext(), "Do not exceed 500 threads", Toast.LENGTH_SHORT).show();
                    return;
                }
                final String str = etContent.getText().toString();
                for (int i = 0; i < threads; i++) {
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            Log4a.i(TAG, str);
                        }
                    }.start();
                }
                tvTest.setText("done!\nlog file path:" + getLogPath());
            }
        });

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!testing) {
                    tvTest.setText("start testing\n");
                    testing = true;
                    int times = Integer.valueOf(etTimes.getText().toString());
                    performanceTest(times);
                    testing = false;
                } else {
                    Toast.makeText(getApplicationContext(), "testing", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_change_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeLogPath();
            }
        });
    }
}
