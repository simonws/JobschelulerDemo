package com.sufadi.jobschedulerdemo;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.sufadi.jobschedulerdemo.service.MyJobSchedulerService;
import com.sufadi.jobschedulerdemo.util.Constant;

public class MainActivity extends AppCompatActivity {

    public static String KEY_START_SERVICE = "messenger";

    public static final int MSG_UNCOLOUR_START = 0;
    public static final int MSG_UNCOLOUR_STOP = 1;
    public static final int MSG_SERVICE_OBJ = 2;
    public static final int MSG_FINISH_OBJ = 3;

    private static int kJobId = 0;

    private ComponentName mServiceComponent;
    private MyJobSchedulerService myJobSchedulerService;

    private TextView tv_info;


    Handler mHandle = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SERVICE_OBJ:
                    // 妥妥地拿到JobScheduler的引用
                    myJobSchedulerService = (MyJobSchedulerService) msg.obj;
                    // UI 和 JobScheduler 服务建立了联系了
                    myJobSchedulerService.setUICallBack(MainActivity.this);
                    break;
                case  MSG_UNCOLOUR_START:
                case MSG_UNCOLOUR_STOP:
                case MSG_FINISH_OBJ:
                    tv_info.setText((String) msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initValue();

        startJobSchedulerService();
    }

    private void initValue() {
        mServiceComponent = new ComponentName(this,  MyJobSchedulerService.class);
    }

    private void initView() {
        tv_info = (TextView) findViewById(R.id.tv_info);
    }

    private void startJobSchedulerService() {
        Intent intent = new Intent(this, MyJobSchedulerService.class);
        intent.putExtra(KEY_START_SERVICE, new Messenger(mHandle));
        startService(intent);
    }

    // JobScheduler的onStartJob周期回调的接口
    public void onReceivedStartJob(JobParameters jobParameters) {
        String result = "UI显示 满足预设置条件（充电且网络连接），JobId = " + jobParameters.getJobId();

        Message msg = new Message();
        msg.what = MSG_UNCOLOUR_START;
        msg.obj = result;
        mHandle.sendMessage(msg);
    }

    // JobScheduler的onStopJob周期回调的接口
    public void onReceivedStopJob(JobParameters jobParameters) {
        String result = "不满足预设置条件，停止JobId " + jobParameters.getJobId();

        Message msg = new Message();
        msg.what = MSG_UNCOLOUR_STOP;
        msg.obj = result;
        mHandle.sendMessage(msg);
    }

    // 按钮
    public void scheduleJob(View v) {
        if (!ensureJobSchedulerService()) {
            return;
        }

        JobInfo.Builder builder = new JobInfo.Builder(kJobId++, mServiceComponent);
        /**
         * Specify that this job should be delayed by the provided amount of time.
         * Because it doesn't make sense setting this property on a periodic job, doing so will
         * throw an {@link java.lang.IllegalArgumentException} when
         * {@link android.app.job.JobInfo.Builder#build()} is called.
         * @param minLatencyMillis Milliseconds before which this job will not be considered for
         *                         execution.
         */
        // 设置最小的延迟时间
        // builder.setMinimumLatency(1*1000);
        /**
         * Set deadline which is the maximum scheduling latency. The job will be run by this
         * deadline even if other requirements are not met. Because it doesn't make sense setting
         * this property on a periodic job, doing so will throw an
         * {@link java.lang.IllegalArgumentException} when
         * {@link android.app.job.JobInfo.Builder#build()} is called.
         */
        // 设置最大的延迟时间，一旦设置了这个属性，不管其他条件怎么样，jobinfo到了时间就一定会执行。
        // builder.setOverrideDeadline(5*1000);
        /** Default. 任意网络都可以*/
        // public static final int NETWORK_TYPE_NONE = 0;
        /** This job requires network connectivity. 任意网络都可以*/
        // public static final int NETWORK_TYPE_ANY = 1;
        /** This job requires network connectivity that is unmetered. 无线网络接入*/
        // public static final int NETWORK_TYPE_UNMETERED = 2;
        /** This job requires network connectivity that is not roaming. 非漫游*/
        // public static final int NETWORK_TYPE_NOT_ROAMING = 3;
        /** This job requires metered connectivity such as most cellular data networks. 移动数据网络 */
        // public static final int NETWORK_TYPE_METERED = 4;
        // 设置在什么样的网络下启动jobinfo
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);

        /**
         * Specify that to run, the job needs the device to be in idle mode. This defaults to
         * false.
         * <p>Idle mode is a loose definition provided by the system, which means that the device
         * is not in use, and has not been in use for some time. As such, it is a good time to
         * perform resource heavy jobs. Bear in mind that battery usage will still be attributed
         * to your application, and surfaced to the user in battery stats.</p>
         * @param requiresDeviceIdle Whether or not the device need be within an idle maintenance
         *                           window.
         */
        // 设置设备需要在空闲的时候，是否启动job
        builder.setRequiresDeviceIdle(false);

        /**
         * Specify that to run this job, the device needs to be plugged in. This defaults to
         * false.
         * @param requiresCharging Whether or not the device is plugged in.
         */
        // 设置是否充电情况下调度
        builder.setRequiresCharging(true);
        /**
         * Set whether or not to persist this job across device reboots.
         *
         * @param isPersisted True to indicate that the job will be written to
         *            disk and loaded at boot.
         */
        // 设置是否重启后继续调度，注意设置true是需要添加重启权限
        builder.setPersisted(false);

        /**
         * 我设置了充电下且网络连接时才触发onJobStart()的条件
         * 注意：上述的条件是与的关系，不是或
         */
        Log.d(Constant.TAG, "设置充电下且网络连接的条件");
        tv_info.setText("设置充电下且网络连接的条件");
        myJobSchedulerService.schedulejob(builder.build());
    }

    /**
     * 杀死在这个包里注册的所有的jobinfo
     * @param v
     */
    public void cancelAllJobs(View v) {
        JobScheduler tm = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);

        /**
         * Cancel all jobs that have been registered with the JobScheduler by this package.
         *
         * 备注：这里会回调onStopJob();
         */
        tm.cancelAll();

        tv_info.setText("杀死在这个包里注册的所有的jobinfo");
    }

    /**
     * 结束指定的Job任务（这里取消最早的）
     * @param v
     */
    public void finishJob(View v) {
        if (!ensureJobSchedulerService()) {
            return;
        }

        myJobSchedulerService.callJobFinished();
    }

    private boolean ensureJobSchedulerService() {
        boolean result = false;
        if (myJobSchedulerService != null) {
            result = true;
        } else {
            Log.d(Constant.TAG, "Service null, never got callback?");
        }
        return result;
    }

    public void onReceivedJobFinished(String result) {
        Message msg = new Message();
        msg.what = MSG_FINISH_OBJ;
        msg.obj = result;
        mHandle.sendMessage(msg);
    }
}
