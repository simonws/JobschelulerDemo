package com.sufadi.jobschedulerdemo.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.sufadi.jobschedulerdemo.MainActivity;
import com.sufadi.jobschedulerdemo.util.Constant;

public class MyJobSchedulerService extends JobService {

    private MainActivity mainActivity;

    // Cancell JobScheduler
    static int mCurrentId = 0;
    private final SparseArray<JobParameters> mJobParametersMap = new SparseArray<>();

    public void setUICallBack(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onCreate() {
        Log.d(Constant.TAG, "onCreate");
        super.onCreate();
    }

    /**
     * Demo中OnCreat 中的startService执行
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constant.TAG, "onStartCommand");
        Message msg = Message.obtain();
        msg.what = MainActivity.MSG_SERVICE_OBJ;
        msg.obj = this;

        Messenger mMessengerCallBack = intent.getParcelableExtra(MainActivity.KEY_START_SERVICE);
        try {
            mMessengerCallBack.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.e(Constant.TAG, "Error passing service object back to activity.");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Override this method with the callback logic for your job. Any such logic needs to be
     * performed on a separate thread, as this function is executed on your application's main
     * thread.
     *
     * @param params Parameters specifying info about this job, including the extras bundle you
     *               optionally provided at job-creation time.
     * @return True if your service needs to process the work (on a separate thread). False if
     * there's no more work to be done for this job.
     */
    /**
     * 满足Job预设置条件下回调
     */
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(Constant.TAG, "当前满足预设置条件，故触发 onStartJob: " + jobParameters.getJobId());

        mCurrentId++;
        mJobParametersMap.put(mCurrentId, jobParameters);

        if (null != mainActivity) {
            mainActivity.onReceivedStartJob(jobParameters);
        }
        return true;
    }

    /**
     * This method is called if the system has determined that you must stop execution of your job
     * even before you've had a chance to call {@link #jobFinished(JobParameters, boolean)}.
     *
     * <p>This will happen if the requirements specified at schedule time are no longer met. For
     * example you may have requested WiFi with
     * {@link android.app.job.JobInfo.Builder#setRequiredNetworkType(int)}, yet while your
     * job was executing the user toggled WiFi. Another example is if you had specified
     * {@link android.app.job.JobInfo.Builder#setRequiresDeviceIdle(boolean)}, and the phone left its
     * idle maintenance window. You are solely responsible for the behaviour of your application
     * upon receipt of this message; your app will likely start to misbehave if you ignore it. One
     * immediate repercussion is that the system will cease holding a wakelock for you.</p>
     *
     * @param params Parameters specifying info about this job.
     * @return True to indicate to the JobManager whether you'd like to reschedule this job based
     * on the retry criteria provided at job creation-time. False to drop the job. Regardless of
     * the value returned, your job must stop executing.
     */
    /**
     * 1.被CancleAll的时候回调
     * 2.不满足预设置条件的情况下回调
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(Constant.TAG, "不满足预设置条件， onStopJob : " + jobParameters.getJobId());

        int id = mJobParametersMap.indexOfValue(jobParameters);
        mJobParametersMap.remove(id);

        if (null != mainActivity) {
            mainActivity.onReceivedStopJob(jobParameters);
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Log.d(Constant.TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * 下发Job参数
     *
     * @param mJobInfo
     */
    public void schedulejob(JobInfo mJobInfo) {
        Log.d(Constant.TAG, "设置详情如下：schedulejob = " + mJobInfo.getId() + ", getNetworkType = " + mJobInfo.getNetworkType()
            + ", isRequireCharging = " + mJobInfo.isRequireCharging() + ", isRequireDeviceIdle = " + mJobInfo.isRequireDeviceIdle() +
            ", isPeriodic = " + mJobInfo.isPeriodic() + ", isPersisted = " + mJobInfo.isPersisted());
        JobScheduler tm = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.schedule(mJobInfo);
    }

    public void callJobFinished() {

        if (mJobParametersMap.size() == 0) {
            return;
        }

        JobParameters params = mJobParametersMap.valueAt(0);
        if (params == null) {
            return;
        } else {
            /**
             * Call this to inform the JobManager you've finished executing. This can be called from any
             * thread, as it will ultimately be run on your application's main thread. When the system
             * receives this message it will release the wakelock being held.
             * <p>
             *     You can specify post-execution behaviour to the scheduler here with
             *     <code>needsReschedule </code>. This will apply a back-off timer to your job based on
             *     the default, or what was set with
             *     {@link android.app.job.JobInfo.Builder#setBackoffCriteria(long, int)}. The original
             *     requirements are always honoured even for a backed-off job. Note that a job running in
             *     idle mode will not be backed-off. Instead what will happen is the job will be re-added
             *     to the queue and re-executed within a future idle maintenance window.
             * </p>
             *
             * @param params Parameters specifying system-provided info about this job, this was given to
             *               your application in {@link #onStartJob(JobParameters)}.
             * @param needsReschedule True if this job should be rescheduled according to the back-off
             *                        criteria specified at schedule-time. False otherwise.
             */

            /**
             * 告诉JobManager 已经完成了工作，如果第二个参数为false，就是不需要重试这个jobinfo，
             *   第二个参数为true,相当于告诉系统任务失败，需要重试，而且与要遵守之前的jobinfo.
             *
             * 备注：这里不会回调onStopJob();
             */
            jobFinished(params, false);

            String result = "callJobFinished getJobId = " + params.getJobId();
            Log.d(Constant.TAG, result);
            if (null != mainActivity) {
                mainActivity.onReceivedJobFinished(result);
            }

            mJobParametersMap.removeAt(0);
        }
    }
}
