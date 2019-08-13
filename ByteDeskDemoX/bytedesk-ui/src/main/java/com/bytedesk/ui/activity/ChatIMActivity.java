package com.bytedesk.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import androidx.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bytedesk.core.api.BDCoreApi;
import com.bytedesk.core.api.BDMqttApi;
import com.bytedesk.core.callback.BaseCallback;
import com.bytedesk.core.event.KickoffEvent;
import com.bytedesk.core.event.LongClickEvent;
import com.bytedesk.core.event.MessageEvent;
import com.bytedesk.core.event.PreviewEvent;
import com.bytedesk.core.repository.BDRepository;
import com.bytedesk.core.util.BDCoreConstant;
import com.bytedesk.core.util.BDCoreUtils;
import com.bytedesk.core.util.BDFileUtils;
import com.bytedesk.core.util.BDPreferenceManager;
import com.bytedesk.core.viewmodel.MessageViewModel;
import com.bytedesk.ui.R;
import com.bytedesk.ui.adapter.ChatAdapter;
import com.bytedesk.ui.adapter.EmotionViewPagerAdapter;
import com.bytedesk.ui.listener.ChatItemClickListener;
import com.bytedesk.ui.recorder.KFRecorder;
import com.bytedesk.ui.recorder.KFRecorderService;
import com.bytedesk.ui.recorder.KFRemainingTimeCalculator;
import com.bytedesk.ui.util.BDPermissionUtils;
import com.bytedesk.ui.util.BDUiConstant;
import com.bytedesk.ui.util.BDUiUtils;
import com.bytedesk.ui.util.EmotionMaps;
import com.bytedesk.ui.util.ExpressionUtil;
import com.bytedesk.ui.widget.InputAwareLayout;
import com.bytedesk.ui.widget.KeyboardAwareLinearLayout;
import com.orhanobut.logger.Logger;
import com.qmuiteam.qmui.util.QMUIStatusBarHelper;
import com.qmuiteam.qmui.util.QMUIViewHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;
import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFile;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  用途：
 *  1. 1v1、群聊 聊天界面
 *
 *  TODO:
 *    1. 访客关闭会话窗口的时候通知客服
 *    2. 客服端关闭会话之后，禁止访客继续发送消息
 *
 * @author bytedesk.com
 */
public class ChatIMActivity extends ChatBaseActivity
        implements ChatItemClickListener,
        View.OnClickListener,
        View.OnTouchListener,
        ViewPager.OnPageChangeListener,
        AdapterView.OnItemClickListener,
        View.OnFocusChangeListener, KFRecorder.OnStateChangedListener,
        KeyboardAwareLinearLayout.OnKeyboardShownListener,
        KeyboardAwareLinearLayout.OnKeyboardHiddenListener {

    private InputAwareLayout mInputAwaireLayout;
    private QMUITopBarLayout mTopBar;
    private QMUIPullRefreshLayout mPullRefreshLayout;
    private RecyclerView mRecyclerView;
    private ChatAdapter mChatAdapter;

    // 是否是机器人会话
    private boolean mIsRobot = false;
    // 切换文字、录音按钮
    private Button mVoiceButton;
    // 按住说话
    private Button mRecordVoiceButton;
    // 显示表情
    private Button mEmotionButton;
    // 显示扩展按钮
    private Button mPlusButton;
    // 发送文本消息按钮
    private Button mSendButton;
    // 输入框
    private EditText mInputEditText;

    private boolean mIsRecording = false;
    // 录音整体UI
    private LinearLayout mRecordVoiceLayout;
    // 正在录音
    private LinearLayout mRecordVoiceHintLayout;
    private LinearLayout mRecordVoiceCancelHintLayout;
    // 取消录音
    private LinearLayout mRecordVoiceTextLayout;
    private LinearLayout mRecordVoiceCancelTextLayout;
    // 音量
    private ImageView mRecordVoiceHintAMPImageView;
    // ////////////////////录音机////////////////////////
    private boolean m_voiceRecordRequestCanBeChanged = false;
    private KFRecorder m_voiceRecorder;
    private RecorderReceiver m_voiceRecordReceiver;
    private boolean m_voiceRecordShowFinishButton = false;
    // 设置为-1，表示大小无限制
    private long m_voiceRecordMaxFileSize = -1;
    private KFRemainingTimeCalculator m_voiceRecordRemainingTimeCalculator;
    private String m_voiceRecordingVoiceFileName;
    private String m_imageCaptureFileName;
    // 录音开始和结束时间戳
    private long m_startRecordingTimestamp, m_endRecordingTimestamp;
    private int m_recordedVoiceLength;
    private Handler m_voiceRecordHandler = new Handler();
    private static final int VOICE_RECORDING_REFRESH_AMP_INTERVAL = 100;
    private static final int CHECK_RECORD_AUDIO_PERMISSION = 5;

    // 表情
    public RelativeLayout mEmotionLayout;
    private ViewPager mEmotionViewPager;
    private EmotionViewPagerAdapter mEmotionViewPagerAdapter;
    private EmotionMaps mEmotionMaps;

    // 表情pager indicator
    private int mCurrentEmotionViewPagerIndex;
    private ImageView mEmotionViewPagerIndicator1;
    private ImageView mEmotionViewPagerIndicator2;
    private ImageView mEmotionViewPagerIndicator3;
    private ImageView mEmotionViewPagerIndicator4;
    private ImageView mEmotionViewPagerIndicator5;

    // 扩展
    public LinearLayout mExtensionLayout;

    // Model
    private MessageViewModel mMessageViewModel;

//    private String mImageCaptureFileName;
//    private String mPicturePath;
//    private Uri mPhotoUri;

//    private Point mScreenSize;
//    private ScaleImageView imagePreview;

    // 根据会话类型不同所代表意义不同：
    private String mUid;
    // 工作组wid
    private String mWorkGroupWid;
    // 客服会话代表会话tid，一对一会话代表uid，群组会话代表gid
    private String mTidOrUidOrGid;
    // 指定坐席uid
    private String mAgentUid;
    private String mTitle;
    // 是否访客端调用接口
    private boolean mIsVisitor;
    // 区分客服会话thread、同事会话contact、群组会话group
    private String mThreadType;
    // 区分工作组会话、指定客服会话
    private String mRequestType;
    // 分页加载聊天记录
    private int mPage = 0;
    private int mSize = 20;
    // 本地存储信息
    private BDPreferenceManager mPreferenceManager;
    private BDRepository mRepository;
    private final Handler mHandler = new Handler();
    //
    private String mCustom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bytedesk_activity_chat_im);

        //
        if (null != getIntent()) {
            //
            mIsVisitor = getIntent().getBooleanExtra(BDUiConstant.EXTRA_VISITOR, true);
            mThreadType = getIntent().getStringExtra(BDUiConstant.EXTRA_THREAD_TYPE);
            mCustom = getIntent().getStringExtra(BDUiConstant.EXTRA_CUSTOM);
            //
            mPreferenceManager = BDPreferenceManager.getInstance(this);
            mPreferenceManager.setVisitor(mIsVisitor);
            mRepository = BDRepository.getInstance(this);
            //
            if (mIsVisitor) {
                Logger.i("访客会话");
                //
                mWorkGroupWid = getIntent().getStringExtra(BDUiConstant.EXTRA_WID);
                mRequestType = getIntent().getStringExtra(BDUiConstant.EXTRA_REQUEST_TYPE);
                // 判断是否指定客服会话
                if (mRequestType.equals(BDCoreConstant.THREAD_REQUEST_TYPE_APPOINTED)) {
                    // 指定客服会话
                    mAgentUid = getIntent().getStringExtra(BDUiConstant.EXTRA_AID);
                } else {
                    // 工作组会话
                    mAgentUid = "";
                }
            } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_THREAD)) {
                Logger.i("客服会话");

                mTidOrUidOrGid = getIntent().getStringExtra(BDUiConstant.EXTRA_TID);
            } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_CONTACT)) {
                Logger.i("一对一会话");

                mTidOrUidOrGid = getIntent().getStringExtra(BDUiConstant.EXTRA_UID);
                if (mCustom != null && mCustom.trim().length() > 0) {
                    sendCommodityMessage(mCustom);
                }
            } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_GROUP)) {
                Logger.i("群组会话");

                mTidOrUidOrGid = getIntent().getStringExtra(BDUiConstant.EXTRA_UID);
                if (mCustom != null && mCustom.trim().length() > 0) {
                    sendCommodityMessage(mCustom);
                }
            }
            //
            mUid = getIntent().getStringExtra(BDUiConstant.EXTRA_UID);
            mTitle = getIntent().getStringExtra(BDUiConstant.EXTRA_TITLE);
        }

        //
        initTopBar();
        initView();
        initModel();
        initRecorder(savedInstanceState);

        // 访客端请求会话
        if (mIsVisitor) {
            requestThread();
        }
        // 从服务器端加载聊天记录，默认暂不加载
        // getMessages();
    }

    @Override
    public void onStart() {
        super.onStart();

        //
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        iFilter.addDataScheme("file");
        registerReceiver(m_voiceRecordSDCardMountEventReceiver, iFilter);

        // 注册监听
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ///////////////////////////////////////////////////
        if (m_voiceRecordRequestCanBeChanged) {
            m_voiceRecorder.reset();
        }

        m_voiceRecordRequestCanBeChanged = false;
        if (!m_voiceRecorder.syncStateWithService()) {
            m_voiceRecorder.reset();
        }

        if (m_voiceRecorder.state() == KFRecorder.RECORDING_STATE) {
            if (!m_voiceRecorder.sampleFile().getName()
                    .endsWith(BDCoreConstant.EXT_AMR)) {
                m_voiceRecorder.reset();
            } else {
                m_voiceRecordRemainingTimeCalculator
                        .setBitRate(BDCoreConstant.BITRATE_AMR);
            }
        } else {
            File file = m_voiceRecorder.sampleFile();
            if (file != null && !file.exists()) {
                m_voiceRecorder.reset();
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(KFRecorderService.RECORDER_SERVICE_BROADCAST_NAME);
        registerReceiver(m_voiceRecordReceiver, filter);

        if (KFRecorderService.isRecording()) {
            Intent intent = new Intent(this, KFRecorderService.class);
            intent.putExtra(KFRecorderService.ACTION_NAME,
                    KFRecorderService.ACTION_DISABLE_MONITOR_REMAIN_TIME);
            startService(intent);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (m_voiceRecorder.state() != KFRecorder.RECORDING_STATE
                || m_voiceRecordShowFinishButton
                || m_voiceRecordMaxFileSize != -1) {
            m_voiceRecorder.stop();
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .cancel(KFRecorderService.NOTIFICATION_ID);
        }

        if (m_voiceRecordReceiver != null) {
            unregisterReceiver(m_voiceRecordReceiver);
        }

        m_voiceRecordRequestCanBeChanged = true;
        if (KFRecorderService.isRecording()) {
            Intent intent = new Intent(this, KFRecorderService.class);
            intent.putExtra(KFRecorderService.ACTION_NAME, KFRecorderService.ACTION_ENABLE_MONITOR_REMAIN_TIME);
            startService(intent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // 销毁监听
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO: 清理
        if (m_voiceRecordSDCardMountEventReceiver != null) {
            unregisterReceiver(m_voiceRecordSDCardMountEventReceiver);
            m_voiceRecordSDCardMountEventReceiver = null;
        }
        //主动回收
        Runtime.getRuntime().gc();
    }

    /**
     * TODO: 客服端输入框显示常用回复按钮
     * @param view
     */
    @Override
    public void onClick(View view) {
        //
        if (view.getId() == R.id.bytedesk_chat_input_send_button) {
            //
            final String content = mInputEditText.getText().toString();
            if (content.trim().length() > 0) {
                String textContent = ExpressionUtil.faceToCN(this, content);
//                Logger.i("faceToCn: " + textContent);

                // TODO: 访客端客服会话：无客服在线时，发送消息会返回机器人答案

                // TODO: 收到客服关闭会话 或者 自动关闭会话消息之后，禁止访客发送消息

                sendTextMessage(textContent);

                mInputEditText.setText(null);
            }
        }
        else if (view.getId() == R.id.bytedesk_chat_input_plus_button) {
            BDUiUtils.showSysSoftKeybord(this, false);
            if (mExtensionLayout.getVisibility() == View.VISIBLE) {
                mExtensionLayout.setVisibility(View.GONE);
            } else {
                mExtensionLayout.setVisibility(View.VISIBLE);
                mEmotionLayout.setVisibility(View.GONE);
            }

        } else if (view.getId() == R.id.bytedesk_chat_input_emotion_button) {
            BDUiUtils.showSysSoftKeybord(this, false);
            if (mEmotionLayout.getVisibility() == View.VISIBLE) {
                mEmotionLayout.setVisibility(View.GONE);
            } else {
                mEmotionLayout.setVisibility(View.VISIBLE);
                mExtensionLayout.setVisibility(View.GONE);
                mRecordVoiceButton.setVisibility(View.GONE);
                mInputEditText.setVisibility(View.VISIBLE);
            }

        } else if (view.getId() == R.id.bytedesk_chat_input_voice_button) {
            BDUiUtils.showSysSoftKeybord(this, false);
            if (mRecordVoiceButton.getVisibility() == View.VISIBLE) {
                mRecordVoiceButton.setVisibility(View.GONE);
                mInputEditText.setVisibility(View.VISIBLE);
            } else {
                mRecordVoiceButton.setVisibility(View.VISIBLE);
                mInputEditText.setVisibility(View.GONE);
            }
            mEmotionLayout.setVisibility(View.GONE);
            mExtensionLayout.setVisibility(View.GONE);

        } else if (view.getId() == R.id.appkefu_plus_pick_picture_btn) {

            // TODO: 收到客服关闭会话 或者 自动关闭会话消息之后，禁止访客发送消息

            pickImageFromAlbum();

        } else if (view.getId() == R.id.appkefu_plus_take_picture_btn) {

            // TODO: 收到客服关闭会话 或者 自动关闭会话消息之后，禁止访客发送消息

            takeCameraImage();

        } else if (view.getId() == R.id.appkefu_plus_show_red_packet_btn) {

            final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(ChatIMActivity.this);
            builder.setTitle("发送红包")
                    .setPlaceholder("在此输入金额")
                    .setInputType(InputType.TYPE_CLASS_TEXT)
                    .addAction("取消", new QMUIDialogAction.ActionListener() {
                        @Override
                        public void onClick(QMUIDialog dialog, int index) {dialog.dismiss();
                        }
                    })
                    .addAction("确定", new QMUIDialogAction.ActionListener() {
                        @Override
                        public void onClick(QMUIDialog dialog, int index) {
                            final CharSequence text = builder.getEditText().getText();
                            if (text != null && text.length() > 0) {

                                // TODO: 检查是否有效数字？
                                sendRedPacketMessage(text.toString());

                                dialog.dismiss();
                            } else {
                                Toast.makeText(ChatIMActivity.this, "请填入金额", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).show();


        } else if (view.getId() == R.id.appkefu_plus_file_btn) {

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "选择文件"), BDUiConstant.SELECT_FILE);

        } else if (view.getId() == R.id.appkefu_read_destroy_btn) {

            toggleDestroyAfterReading();

        } else if (view.getId() == R.id.appkefu_plus_shop_btn) {

            Toast.makeText(this, "自定义跳转页面，选择商品，发送", Toast.LENGTH_LONG).show();

            // TODO: 自定义跳转页面，选择商品，发送
//            JsonCustom jsonCustom = new JsonCustom();
//            jsonCustom.setType(BDCoreConstant.MESSAGE_TYPE_COMMODITY);
//            jsonCustom.setTitle("商品标题");
//            jsonCustom.setContent("商品详情");
//            jsonCustom.setPrice("9.99");
//            jsonCustom.setUrl("https://item.m.jd.com/product/12172344.html");
//            jsonCustom.setImageUrl("https://m.360buyimg.com/mobilecms/s750x750_jfs/t4483/332/2284794111/122812/4bf353/58ed7f42Nf16d6b20.jpg!q80.dpg");
//            jsonCustom.setId(100121);
//            jsonCustom.setCategoryCode("100010003");
//            //
//            String custom = new Gson().toJson(jsonCustom);
//            sendCommodityMessage(custom);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if (view.getId() == R.id.bytedesk_chat_input_record_voice_button) {
            //
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (!mIsRecording)
                    return false;

                mRecordVoiceHintLayout.setVisibility(View.GONE);
                m_endRecordingTimestamp = System.currentTimeMillis();
                stopVoiceRecording();

                // 发送录音
                if (motionEvent.getY() >= 0) {

                    m_recordedVoiceLength = (int) (m_endRecordingTimestamp - m_startRecordingTimestamp) / 1000;
                    if (m_recordedVoiceLength < 1) {
                        Toast.makeText(ChatIMActivity.this, R.string.kfds_record_voice_too_short, Toast.LENGTH_LONG).show();
                    } else if (m_recordedVoiceLength > 60) {
                        Toast.makeText(ChatIMActivity.this, R.string.kfds_record_voice_too_long, Toast.LENGTH_LONG).show();
                    } else {
                        //
                        if (mIsRobot) {
                            Toast.makeText(ChatIMActivity.this, R.string.kfds_robot_cannot_send_voice, Toast.LENGTH_LONG).show();
                            return false;
                        }
                        //
                        String filePath = BDFileUtils.getVoiceWritePath(m_voiceRecordingVoiceFileName + BDCoreConstant.EXT_AMR);
                        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                        Logger.i("filePath:" + filePath + " fileName: " + fileName);

                        // TODO: 上传语音
                        uploadVoice(filePath, fileName, m_recordedVoiceLength);
                    }

                }

            } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {

                // android 6.0动态授权机制
                // http://jijiaxin89.com/2015/08/30/Android-s-Runtime-Permission/
                // http://inthecheesefactory.com/blog/things-you-need-to-know-about-android-m-permission-developer-edition/en
                if (Build.VERSION.SDK_INT >= 23) {
//					int checkRecordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
//					int checkRecordAudioPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        new AlertDialog.Builder(ChatIMActivity.this)
                                .setMessage(getString(R.string.kfds_record_permission_tip))
                                .setPositiveButton(
                                        getString(R.string.kfds_ok),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                ActivityCompat.requestPermissions(ChatIMActivity.this,
                                                        new String[] { Manifest.permission.RECORD_AUDIO,
                                                                Manifest.permission.WRITE_EXTERNAL_STORAGE },
                                                        CHECK_RECORD_AUDIO_PERMISSION);
                                            }
                                        })
                                .setNegativeButton(getString(R.string.kfds_cancel), null)
                                .create().show();
                    } else {
                        startVoiceRecording();
                    }
                } else {
                    startVoiceRecording();
                }
            }
        }

        // 切换效果：按住录音按钮，然后上滑出按钮，然后退回按住录音按钮
        if (motionEvent.getY() < 0) {
            mRecordVoiceHintLayout.setVisibility(View.GONE);
            mRecordVoiceCancelHintLayout.setVisibility(View.VISIBLE);
            mRecordVoiceTextLayout.setVisibility(View.GONE);
            mRecordVoiceCancelTextLayout.setVisibility(View.VISIBLE);
        } else {
            mRecordVoiceHintLayout.setVisibility(View.VISIBLE);
            mRecordVoiceCancelHintLayout.setVisibility(View.GONE);
            mRecordVoiceTextLayout.setVisibility(View.VISIBLE);
            mRecordVoiceCancelTextLayout.setVisibility(View.GONE);
        }

        return false;
    }


    /**
     * 顶部topbar初始化
     */
    private void initTopBar() {
        //
        mTopBar = findViewById(R.id.bytedesk_chat_topbarlayout);
        if (!BDMqttApi.isConnected(this)) {
            mTopBar.setTitle(mTitle+"(未连接)");
        } else {
            mTopBar.setTitle(mTitle);
        }
        mTopBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 客服会话
        if (!mIsVisitor && mThreadType.equals(BDCoreConstant.MESSAGE_SESSION_TYPE_THREAD)) {
            // 客服会话
            mTopBar.addRightImageButton(R.mipmap.icon_topbar_overflow, QMUIViewHelper.generateViewId())
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showTopRightSheet();
                        }
                    });
        } else if (mThreadType.equals(BDCoreConstant.MESSAGE_SESSION_TYPE_CONTACT)) {
            // 一对一会话
            mTopBar.addRightImageButton(R.mipmap.icon_topbar_overflow, QMUIViewHelper.generateViewId())
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //
                            Intent intent = new Intent(ChatIMActivity.this, ContactProfileActivity.class);
                            intent.putExtra(BDUiConstant.EXTRA_UID, mUid);
                            startActivity(intent);
                        }
                    });

        } else if (mThreadType.equals(BDCoreConstant.MESSAGE_SESSION_TYPE_GROUP)) {
            // 群组
            mTopBar.addRightImageButton(R.mipmap.icon_topbar_overflow, QMUIViewHelper.generateViewId())
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //
                            Intent intent = new Intent(ChatIMActivity.this, GroupProfileActivity.class);
                            intent.putExtra(BDUiConstant.EXTRA_UID, mUid);
                            startActivity(intent);
                        }
                    });
        }
        QMUIStatusBarHelper.translucent(this);
    }

    /**
     * 界面初始化
     */
    private void initView () {
        //
        mInputAwaireLayout = findViewById(R.id.bytedesk_activity_chat_im);
        mInputAwaireLayout.addOnKeyboardShownListener(this);
        mInputAwaireLayout.addOnKeyboardHiddenListener(this);
        //
        mPullRefreshLayout = findViewById(R.id.bytedesk_chat_pulltorefresh);
        mPullRefreshLayout.setOnPullListener(pullListener);

        // TODO: 增加点击聊天界面，去除输入框焦点，让其缩回底部
        // 初始化
        mRecyclerView = findViewById(R.id.bytedesk_chat_fragment_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // 设置适配器adapter
        mChatAdapter = new ChatAdapter(this, this);
        mRecyclerView.setAdapter(mChatAdapter);

        // 录音HUD
        mRecordVoiceLayout = findViewById(R.id.bytedesk_chat_wx_voice_record);
        mRecordVoiceHintLayout = findViewById(R.id.appkefu_voice_record_hint_layout);
        mRecordVoiceCancelHintLayout = findViewById(R.id.appkefu_voice_record_hint_cancel_layout);
        mRecordVoiceTextLayout = findViewById(R.id.appkefu_voice_record_hint_text_record_layout);
        mRecordVoiceCancelTextLayout = findViewById(R.id.appkefu_voice_record_hint_text_cancel_layout);
        mRecordVoiceHintAMPImageView = findViewById(R.id.appkefu_voice_record_hint_amp);

        // 语音
        mVoiceButton = findViewById(R.id.bytedesk_chat_input_voice_button);
        mVoiceButton.setOnClickListener(this);

        // 按住录音
        mRecordVoiceButton = findViewById(R.id.bytedesk_chat_input_record_voice_button);
        mRecordVoiceButton.setOnTouchListener(this);

        // 表情
        mEmotionButton = findViewById(R.id.bytedesk_chat_input_emotion_button);
        mEmotionButton.setOnClickListener(this);

        // 选择图片、拍照
        mPlusButton = findViewById(R.id.bytedesk_chat_input_plus_button);
        mPlusButton.setOnClickListener(this);

        // 发送文本消息
        mSendButton = findViewById(R.id.bytedesk_chat_input_send_button);
        mSendButton.setOnClickListener(this);

        // 输入框
        mInputEditText = findViewById(R.id.bytedesk_chat_fragment_input);
        mInputEditText.addTextChangedListener(inputTextWatcher);
        mInputEditText.setOnFocusChangeListener(this);

        // 图片大图预览
//        imagePreview = findViewById(R.id.bytedesk_image_preivew);
//        mScreenSize = ImageViewerUtil.getScreenSize(this);
//        imagePreview.setDefSize(mScreenSize.x, mScreenSize.y);
//        imagePreview.setImageDraggerType(ImageDraggerType.DRAG_TYPE_WX);

        // 表情
        mEmotionLayout = findViewById(R.id.bytedesk_chat_emotion);
        mEmotionMaps = new EmotionMaps(this);
        mEmotionViewPagerAdapter = new EmotionViewPagerAdapter(mEmotionMaps.getGridViewArrayList());
        mEmotionViewPager = findViewById(R.id.appkefu_emotion_viewpager);
        mEmotionViewPager.setAdapter(mEmotionViewPagerAdapter);
        mEmotionViewPager.addOnPageChangeListener(this);
        
        // 
        mEmotionViewPagerIndicator1 = findViewById(R.id.appkefu_emotionview_pageindicator_imageview_1);
        mEmotionViewPagerIndicator2 = findViewById(R.id.appkefu_emotionview_pageindicator_imageview_2);
        mEmotionViewPagerIndicator3 = findViewById(R.id.appkefu_emotionview_pageindicator_imageview_3);
        mEmotionViewPagerIndicator4 = findViewById(R.id.appkefu_emotionview_pageindicator_imageview_4);
        mEmotionViewPagerIndicator5 = findViewById(R.id.appkefu_emotionview_pageindicator_imageview_5);

        // 扩展
        mExtensionLayout = findViewById(R.id.bytedesk_chat_extension);
        // 照片相册
        findViewById(R.id.appkefu_plus_pick_picture_btn).setOnClickListener(this);
        // 拍照
        findViewById(R.id.appkefu_plus_take_picture_btn).setOnClickListener(this);
        // 红包
        findViewById(R.id.appkefu_plus_show_red_packet_btn).setOnClickListener(this);
        // 文件
        findViewById(R.id.appkefu_plus_file_btn).setOnClickListener(this);
        // 阅后即焚
        findViewById(R.id.appkefu_read_destroy_btn).setOnClickListener(this);
//        // 仅有一对一单聊支持阅后即焚，客服会话和群聊不支持
//        if (!mThreadType.equals(BDCoreConstant.THREAD_TYPE_CONTACT)) {
//            findViewById(R.id.appkefu_read_destroy_btn).setVisibility(View.GONE);
//        }
        // 商品
        findViewById(R.id.appkefu_plus_shop_btn).setOnClickListener(this);
    }

    /**
     * 初始化ModelView
     *
     * TODO: 完善收发消息界面出现闪动的情况
     */
    private void initModel () {
        //
        mMessageViewModel = ViewModelProviders.of(this).get(MessageViewModel.class);

        // FIXME: 当工作组设置有值班工作组的情况下，则界面无法显示值班工作组新消息

        if (mIsVisitor) {

            // 判断是否指定客服会话
            if (mRequestType.equals(BDCoreConstant.THREAD_REQUEST_TYPE_APPOINTED)) {
                Logger.i("访客会话: 指定客服聊天记录");
                // 指定客服聊天记录
                mMessageViewModel.getThreadMessages(mTidOrUidOrGid).observe(this, messageEntities -> {
                    mChatAdapter.setMessages(messageEntities);
                    mRecyclerView.scrollToPosition(messageEntities.size() - 1);
                });
            } else {
                Logger.i("访客会话: 工作组聊天记录");
                // 工作组聊天记录, TODO: 是否沿用此方式待定，转接会话聊天
                mMessageViewModel.getWorkGroupMessages(mWorkGroupWid).observe(this, messageEntities -> {
                    mChatAdapter.setMessages(messageEntities);
                    mRecyclerView.scrollToPosition(messageEntities.size() - 1);
                });
            }

        } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_THREAD)){
            Logger.i("客服端：客服会话");

            mMessageViewModel.getVisitorMessages(mUid).observe(this, messageEntities -> {
                mChatAdapter.setMessages(messageEntities);
                mRecyclerView.scrollToPosition(messageEntities.size() - 1);
            });
            // 设置当前会话
            updateCurrentThread();
        } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_CONTACT)) {
            Logger.i("客服端：一对一会话");

            mMessageViewModel.getContactMessages(mUid).observe(this, messageEntities -> {
                mChatAdapter.setMessages(messageEntities);
                mRecyclerView.scrollToPosition(messageEntities.size() - 1);
            });

        } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_GROUP)) {
            Logger.i("客服端：群组会话");

            mMessageViewModel.getGroupMessages(mUid).observe(this, messageEntities -> {
                mChatAdapter.setMessages(messageEntities);
                mRecyclerView.scrollToPosition(messageEntities.size() - 1);
            });
        }
    }


    /**
     * 请求会话
     * 请求工作组会话和指定客服会话统一接口
     */
    private void requestThread() {

        BDCoreApi.requestThread(this, mWorkGroupWid, mRequestType, mAgentUid, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

                dealWithThread(object);
            }

            @Override
            public void onError(JSONObject object) {
                try {
                    Logger.d("request thread message: " + object.get("message")
                            + " status_code:" + object.get("status_code")
                            + " data:" + object.get("data"));
                    Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 更新当前会话
     */
    private void updateCurrentThread() {

        String preTid = mPreferenceManager.getCurrentTid();
        BDCoreApi.updateCurrentThread(this, preTid, mTidOrUidOrGid, new BaseCallback() {
            @Override
            public void onSuccess(JSONObject object) {
                // 设置当前tid
                mPreferenceManager.setCurrentTid(mTidOrUidOrGid);
            }

            @Override
            public void onError(JSONObject object) {
                Logger.e("更新当前会话失败");
                try {
                    Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 从服务器加载聊天记录
     */
    private void getMessages() {

        if (mIsVisitor) {
            Logger.i("访客端");

            //
            BDCoreApi.getMessagesWithUser(getBaseContext(), mPage, mSize, new BaseCallback() {

                @Override
                public void onSuccess(JSONObject object) {

                    try {
                        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("content");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            mMessageViewModel.insertMessageJson(jsonArray.getJSONObject(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mPullRefreshLayout.finishRefresh();
                    mPage++;
                }

                @Override
                public void onError(JSONObject object) {

                    mPullRefreshLayout.finishRefresh();

                    try {
                        Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        }  else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_THREAD)){
            Logger.i("客服端：客服会话 uid:" + mUid);

            // 客服端接口
            BDCoreApi.getMessagesWithUser(getBaseContext(), mUid, mPage, mSize, new BaseCallback() {

                @Override
                public void onSuccess(JSONObject object) {

                    try {
                        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("content");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            mMessageViewModel.insertMessageJson(jsonArray.getJSONObject(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mPullRefreshLayout.finishRefresh();
                    mPage++;
                }

                @Override
                public void onError(JSONObject object) {

                    mPullRefreshLayout.finishRefresh();

                    try {
                        Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_CONTACT)) {
            Logger.i("一对一会话 cid: " + mUid);

            BDCoreApi.getMessagesWithContact(getBaseContext(), mUid, mPage, mSize, new BaseCallback() {

                @Override
                public void onSuccess(JSONObject object) {

                    try {
                        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("content");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            mMessageViewModel.insertMessageJson(jsonArray.getJSONObject(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mPullRefreshLayout.finishRefresh();
                    mPage++;
                }

                @Override
                public void onError(JSONObject object) {

                    mPullRefreshLayout.finishRefresh();

                    try {
                        Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        } else if (mThreadType.equals(BDCoreConstant.THREAD_TYPE_GROUP)) {
            Logger.i("群组会话 gid: " + mUid);

            BDCoreApi.getMessagesWithGroup(getBaseContext(), mUid, mPage, mSize, new BaseCallback() {
                @Override
                public void onSuccess(JSONObject object) {

                    try {
                        JSONArray jsonArray = object.getJSONObject("data").getJSONArray("content");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            mMessageViewModel.insertMessageJson(jsonArray.getJSONObject(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    mPullRefreshLayout.finishRefresh();
                    mPage++;
                }

                @Override
                public void onError(JSONObject object) {

                    mPullRefreshLayout.finishRefresh();

                    try {
                        Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    /**
     * 处理thread返回结果
     *
     * @param object
     */
    private void dealWithThread(JSONObject object) {
        //
        try {
            Logger.d("request thread success message: " + object.get("message")
                    + " status_code:" + object.get("status_code"));

            int status_code = object.getInt("status_code");
            if (status_code == 200 || status_code == 201) {
                // 创建新会话

                JSONObject message = object.getJSONObject("data");
                mMessageViewModel.insertMessageJson(message);

                mTidOrUidOrGid = message.getJSONObject("thread").getString("tid");
                Logger.i("mTidOrUidOrGid:" + mTidOrUidOrGid);

                String threadTopic = "thread/" + mTidOrUidOrGid;
                BDMqttApi.subscribeTopic(ChatIMActivity.this, threadTopic);

                if (mCustom != null && mCustom.trim().length() > 0) {
                    sendCommodityMessage(mCustom);
                }

            } else if (status_code == 202) {
                // 提示排队中

                JSONObject message = object.getJSONObject("data");
                mMessageViewModel.insertMessageJson(message);

                mTidOrUidOrGid = message.getJSONObject("thread").getString("tid");
                String threadTopic = "thread/" + mTidOrUidOrGid;
                BDMqttApi.subscribeTopic(ChatIMActivity.this, threadTopic);

                if (mCustom != null && mCustom.trim().length() > 0) {
                    sendCommodityMessage(mCustom);
                }

            } else if (status_code == 203) {
                // 当前非工作时间，请自助查询或留言

                JSONObject message = object.getJSONObject("data");
                mMessageViewModel.insertMessageJson(message);

                mTidOrUidOrGid = message.getJSONObject("thread").getString("tid");
                String threadTopic = "thread/" + mTidOrUidOrGid;
                BDMqttApi.subscribeTopic(ChatIMActivity.this, threadTopic);

            } else if (status_code == 204) {
                // 当前无客服在线，请自助查询或留言

                JSONObject message = object.getJSONObject("data");
                mMessageViewModel.insertMessageJson(message);

                mTidOrUidOrGid = message.getJSONObject("thread").getString("tid");
                String threadTopic = "thread/" + mTidOrUidOrGid;
                BDMqttApi.subscribeTopic(ChatIMActivity.this, threadTopic);

            } else if (status_code == 205) {
                // TODO: 咨询前问卷

                String title = "";
                JSONObject message = object.getJSONObject("data");
                mTidOrUidOrGid = message.getJSONObject("thread").getString("tid");
                // 存储key/value: content/qid
                final Map<String, String> questionMap = new HashMap<>();
                // 存储key/value: content/workGroups
                final Map<String, JSONArray> workGroupsMap = new HashMap<>();
                //
                List<String> questionContents = new ArrayList<>();

                if (!message.isNull("questionnaire")) {
                    //
                    JSONObject questionnaireObject = message.getJSONObject("questionnaire");
                    if (!questionnaireObject.isNull("questionnaireItems")) {
                        //
                        JSONArray questionItems = questionnaireObject.getJSONArray("questionnaireItems");
                        //
                        for (int i = 0; i < questionItems.length(); i++) {
                            // TODO: 一个questionItem作为一条消息插入
                            JSONObject questionItem = questionItems.getJSONObject(i);
                            title = questionItem.getString("title");

                            JSONArray questionnaireItemItems = questionItem.getJSONArray("questionnaireItemItems");
                            for (int j = 0; j < questionnaireItemItems.length(); j++) {
                                JSONObject questionnaireItemItem = questionnaireItemItems.getJSONObject(j);
                                //
//                                Logger.i("content " + questionnaireItemItem.getString("content"));
                                questionMap.put(questionnaireItemItem.getString("content"), questionnaireItemItem.getString("qid"));
                                workGroupsMap.put(questionnaireItemItem.getString("content"), questionnaireItemItem.getJSONArray("workGroups"));
                                questionContents.add(questionnaireItemItem.getString("content"));
                            }
                        }
                    }
                }

                // 1. 弹窗选择列表：类型、工作组
                final String[] items = questionContents.toArray(new String[0]);
                new QMUIDialog.MenuDialogBuilder(ChatIMActivity.this)
                        .setTitle(title)
                        .addItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String questionnaireItemItemQid = questionMap.get(items[which]);
                                Logger.i("qid:" + questionnaireItemItemQid + " content:" + items[which]);
                                // 留学: 意向国家 qid = '201810061551181'
                                // 移民：意向国家 qid = '201810061551183'
                                // 语培：意向类别 qid = '201810061551182'
                                // 其他：意向类别 qid = '201810061551184'
                                // 院校：意向院校 qid = '201810061551185'

                                if (questionnaireItemItemQid.equals("201810061551181")) {
                                    // 单独处理 留学: 意向国家 qid = '201810061551181'
//                                requestQuestionnaire(questionnaireItemItemQid);
                                    showWorkGroupDialog(workGroupsMap.get(items[which]), true);
                                } else {
                                    //
                                    showWorkGroupDialog(workGroupsMap.get(items[which]), false);
                                }

                                dialog.dismiss();
                            }
                        }).show();

            } else if (status_code == 206) {




            } else {
                // 请求会话失败
                String message = object.getString("message");
                Toast.makeText(ChatIMActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            //
            if (mRequestType.equals(BDCoreConstant.THREAD_REQUEST_TYPE_APPOINTED)) {
                Logger.i("重新加载 指定客服聊天记录");
                initModel();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 选择问卷答案
     *
     * @param questionnaireItemItemQid qid
     */
//    private void requestQuestionnaire(String questionnaireItemItemQid) {
//
//        BDCoreApi.requestQuestionnaire(this, mTidOrUidOrGid, questionnaireItemItemQid, new BaseCallback() {
//
//            @Override
//            public void onSuccess(JSONObject object) {
//
//                try {
//
//                    JSONObject message = object.getJSONObject("data");
//
//                    int status_code = object.getInt("status_code");
//                    if (status_code == 200) {
//
////                      String  title = message.getString("content");
//                        if (!message.isNull("workGroups")) {
//
//                            JSONArray workGroupsArray = message.getJSONArray("workGroups");
//                            showWorkGroupDialog(workGroupsArray);
//                        }
//
//                    } else {
//
//                        //
//                        String toast = object.getString("message");
//                        Toast.makeText(ChatIMActivity.this, toast, Toast.LENGTH_LONG).show();
//                    }
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onError(JSONObject object) {
//                try {
//                    Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }

    /**
     * 显示选择工作组提示框
     *
     * @param workGroupsArray
     */
    private void showWorkGroupDialog(JSONArray workGroupsArray, boolean isLiuXue) {

        try {

            final Map<String, String> workGroupMap = new HashMap<>();
            List<String> workGroupNames = new ArrayList<>();

            for (int i = 0; i < workGroupsArray.length(); i++) {

                JSONObject workGroupObject = workGroupsArray.getJSONObject(i);
                workGroupMap.put(workGroupObject.getString("nickname"), workGroupObject.getString("wid"));
                workGroupNames.add(workGroupObject.getString("nickname"));
            }

            // 1. 弹窗选择列表：工作组
            final String[] items = workGroupNames.toArray(new String[0]);
            new QMUIDialog.MenuDialogBuilder(ChatIMActivity.this)
                    .setTitle("请选择")
                    .addItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //
                            String workGroupNickname = items[which];
                            String workGroupWid = workGroupMap.get(items[which]);
                            Logger.i("nickname:" + items[which] + " workGroupWid:" + workGroupWid);
                            //
                            if (isLiuXue) {
                                chooseWorkGroupLiuXue(workGroupWid, workGroupNickname);
                            } else {
                                chooseWorkGroup(workGroupWid);
                            }

                            dialog.dismiss();
                        }
                    }).show();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 问卷答案中选择工作组
     *
     * @param workGroupWid
     */
    private void chooseWorkGroup(final String workGroupWid) {

        BDCoreApi.chooseWorkGroup(this, workGroupWid,  new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {
                // 重新选择工作组成功 old wid:201807171659201 new wid:201810201758121
                Logger.i("重新选择工作组成功 old wid:" + mWorkGroupWid + " new wid:" + workGroupWid);
                // 重新初始化model，根据新的wid加载聊天记录
                mWorkGroupWid = workGroupWid;
                Logger.i("mWorkGroupWid:" + mWorkGroupWid);

                initModel();
                //
                dealWithThread(object);
            }

            @Override
            public void onError(JSONObject object) {
                try {
                    Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 留学，针对大学长定制
     *
     * @param workGroupWid
     * @param workGroupNickname
     */
    private void chooseWorkGroupLiuXue(final String workGroupWid, String workGroupNickname) {

        BDCoreApi.chooseWorkGroupLiuXue(this, workGroupWid, workGroupNickname,  new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {
                // 重新选择工作组成功 old wid:201807171659201 new wid:201810201758121
                Logger.i("重新选择工作组成功 old wid:" + mWorkGroupWid + " new wid:" + workGroupWid);
                // 重新初始化model，根据新的wid加载聊天记录
                mWorkGroupWid = workGroupWid;
                Logger.i("mWorkGroupWid:" + mWorkGroupWid);

                initModel();
                //
                dealWithThread(object);
            }

            @Override
            public void onError(JSONObject object) {
                try {
                    Toast.makeText(ChatIMActivity.this, object.getString("message"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 从相册中选择图片
     *
     * FIXME: 当前版本，禁止选择视频文件
     *
     */
    private void pickImageFromAlbum() {

        // 目前仅允许一次选一张图片
        Album.image(this)
                .singleChoice()
                .camera(false)
                .onResult(new Action<ArrayList<AlbumFile>>() {
                    @Override
                    public void onAction(@NonNull ArrayList<AlbumFile> result) {

                        if (result.size() > 0) {
                            AlbumFile albumFile = result.get(0);

                            String imageName = mPreferenceManager.getUsername() + "_" + BDCoreUtils.getPictureTimestamp();
                            uploadImage(albumFile.getPath(), imageName);
                        }
                    }
                })
                .onCancel(new Action<String>() {
                    @Override
                    public void onAction(@NonNull String result) {
                        Toast.makeText(ChatIMActivity.this, "取消发送图片", Toast.LENGTH_LONG).show();
                    }
                })
                .start();

        // TODO: 待删除
//        Intent intent;
//        if (Build.VERSION.SDK_INT < 19) {
//            intent = new Intent(Intent.ACTION_GET_CONTENT);
//            intent.setType("image/*");
//        } else {
//            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        }
//        startActivityForResult(intent, BDUiConstant.SELECT_PIC_BY_PICK_PHOTO);
    }


    /**
     * 摄像头拍摄图片
     *
     * FIXME: 当前版本，禁止拍摄视频，仅支持拍照
     */
    private void takeCameraImage() {

        // TODO: 判断是否模拟器，如果是，则弹出tip提示，并返回

        // 调用第三方库album
        Album.camera(this)
                .image()
                .onResult(new Action<String>() {
                    @Override
                    public void onAction(@NonNull String result) {

                        String imageFileName = mPreferenceManager.getUsername() + "_" + BDCoreUtils.getPictureTimestamp();
                        uploadImage(result, imageFileName);
                    }
                })
                .onCancel(new Action<String>() {
                    @Override
                    public void onAction(@NonNull String result) {
                        Toast.makeText(ChatIMActivity.this, "取消拍照", Toast.LENGTH_LONG).show();
                    }
                })
                .start();

        // TODO: 待删除
        //
//        if (BDCoreUtils.isSDCardExist()) {
//            //
//            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//            mImageCaptureFileName = mPreferenceManager.getUsername() + "_" + BDCoreUtils.getPictureTimestamp();
//            mPhotoUri = BDCoreUtils.getUri(BDCoreUtils.getTempImage(mImageCaptureFileName), this);
//            intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
//            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mPhotoUri);
//            startActivityForResult(intent, BDUiConstant.SELECT_PIC_BY_TAKE_PHOTO);
//        }
//        else {
//            Toast.makeText(this, "SD卡不存在，不能拍照", Toast.LENGTH_SHORT).show();
//        }
    }

    /**
     *
     */
    private void showTopRightSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(this)
                .addItem("关闭会话")
//                .addItem("访客资料") // TODO: 查看访客资料
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        dialog.dismiss();
                        //
                        BDCoreApi.agentCloseThread(getApplication(), mTidOrUidOrGid, new BaseCallback() {

                            @Override
                            public void onSuccess(JSONObject object) {
                                // 关闭页面
                                finish();
                            }

                            @Override
                            public void onError(JSONObject object) {
                                Toast.makeText(getApplication(), "关闭会话错误", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .build()
                .show();
    }


    /**
     * 请求相册读取权限
     */
    private void requestAlbumPermission() {

        // android 6.0动态授权机制
        // http://jijiaxin89.com/2015/08/30/Android-s-Runtime-Permission/
        // http://inthecheesefactory.com/blog/things-you-need-to-know-about-android-m-permission-developer-edition/en
        if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // 首先提示用户，待确认之后，请求用户授权
                new QMUIDialog.MessageDialogBuilder(this)
                        .setTitle("请求授权")
                        .setMessage("相册需要授权，请授权")
                        .addAction("取消", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                            }
                        })
                        .addAction("确定", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                                // 请求授权
                                ActivityCompat.requestPermissions(ChatIMActivity.this,
                                        new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        BDUiConstant.PERMISSION_REQUEST_ALBUM);
                            }
                        }).show();
            }
            else {
                pickImageFromAlbum();
            }
        }
        else {
            pickImageFromAlbum();
        }
    }

    /**
     * 请求摄像头权限
     */
    private void requestCameraPermission() {

        // android 6.0动态授权机制
        // http://jijiaxin89.com/2015/08/30/Android-s-Runtime-Permission/
        // http://inthecheesefactory.com/blog/things-you-need-to-know-about-android-m-permission-developer-edition/en
        if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

                // 首先提示用户，待确认之后，请求用户授权
                new QMUIDialog.MessageDialogBuilder(this)
                        .setTitle("请求授权")
                        .setMessage("拍照需要授权，请授权")
                        .addAction("取消", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                            }
                        })
                        .addAction("确定", new QMUIDialogAction.ActionListener() {
                            @Override
                            public void onClick(QMUIDialog dialog, int index) {
                                dialog.dismiss();
                                // 请求授权
                                ActivityCompat.requestPermissions(ChatIMActivity.this,
                                        new String[] { Manifest.permission.CAMERA,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        BDUiConstant.PERMISSION_REQUEST_CAMERA);
                            }
                        }).show();
            }
            else {
                takeCameraImage();
            }
        }
        else {
            takeCameraImage();
        }
    }

    /**
     * 点击图片消息回调
     */
    @Override
    public void onMessageImageItemClick(String imageUrl) {
        Logger.d("imageUrl:" + imageUrl);

//        Glide.with(this).load(imageUrl).into(new SimpleTarget<Drawable>() {
//            @Override
//            public void onLoadCleared(@Nullable Drawable placeholder) {
//                super.onLoadCleared(placeholder);
//                imagePreview.getImageView().setImageDrawable(placeholder);
//            }
//
//            @Override
//            public void onLoadStarted(@Nullable Drawable placeholder) {
//                super.onLoadStarted(placeholder);
//                imagePreview.showProgess();
//                imagePreview.getImageView().setImageDrawable(placeholder);
//            }
//
//            @Override
//            public void onLoadFailed(@Nullable Drawable errorDrawable) {
//                super.onLoadFailed(errorDrawable);
//                imagePreview.hideProgress();
//                imagePreview.getImageView().setImageDrawable(errorDrawable);
//            }
//
//            @Override
//            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
//                if (resource != null) {
//                    imagePreview.hideProgress();
//                    imagePreview.getImageView().setImageDrawable(resource);
////                    mViewData.setImageWidth(resource.getIntrinsicWidth());
////                    mViewData.setImageHeight(resource.getIntrinsicHeight());
//                }
//            }
//        });
//        imagePreview.setViewData(viewData);
//        imagePreview.start();

        Intent intent = new Intent(this, BigImageViewActivity.class);
        intent.putExtra("image_url", imageUrl);
        startActivity(intent);
    }

    /**
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case BDUiConstant.PERMISSION_REQUEST_RECORD_AUDIO:
                if (BDPermissionUtils.verifyPermissions(grantResults)) {
                    // Permission Granted, 录音
                }
                else {
                    // Permission Denied
                    Toast.makeText(this, "录音授权失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case BDUiConstant.PERMISSION_REQUEST_CAMERA:
                if (BDPermissionUtils.verifyPermissions(grantResults)) {
                    // Permission Granted
                    takeCameraImage();
                } else {
                    // Permission Denied
                    Toast.makeText(this, "拍照授权失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case BDUiConstant.PERMISSION_REQUEST_ALBUM:
                if (BDPermissionUtils.verifyPermissions(grantResults)) {
                    // Permission Granted
                    pickImageFromAlbum();
                } else {
                    // Permission Denied
                    Toast.makeText(this, "相册授权失败", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * 监听 EventBus 广播消息
     * TODO: 收到消息之后，如果消息属于当前页面，可处理阅后即焚消息
     *
     * @param messageEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent messageEvent) {
//        Logger.i("会话页面 MessageEvent");

        // TODO: 检查是否当前页面消息，如果是，则发送已读消息回执

        // TODO: 判断是否阅后即焚消息，如果是，则倒计时销毁

    }

    /**
     * TODO: 区分是否当前会话
     *
     * @param previewEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPreviewEvent(PreviewEvent previewEvent) {
        Logger.i("onPreviewEvent");

        if (previewEvent.getContent().trim().length() == 0) {
            return;
        }

        if (mIsVisitor) {
            mTopBar.setTitle("对方正在输入...");
        } else {
            mTopBar.setTitle("对方正在输入:" + previewEvent.getContent());
        }
        //
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mTopBar.setTitle(mTitle);
            }
        }, 3000);
    }

    /**
     * 账号异地登录通知提示，开发者可自行决定是否退出当前账号登录
     *
     * @param kickoffEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onKickoffEvent(KickoffEvent kickoffEvent) {

        String content = kickoffEvent.getContent();
        Logger.w("onKickoffEvent: " + content);

        // 弹窗提示
        new QMUIDialog.MessageDialogBuilder(this)
                .setTitle("异地登录提示")
                .setMessage(content)
                .addAction("确定", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();

                        // TODO: 开发者可自行决定是否退出登录

                    }
                }).show();
    }


    /**
     * 下拉刷新
     */
    private QMUIPullRefreshLayout.OnPullListener pullListener = new QMUIPullRefreshLayout.OnPullListener() {

        @Override
        public void onMoveTarget(int offset) {

        }

        @Override
        public void onMoveRefreshView(int offset) {

        }

        @Override
        public void onRefresh() {
            Logger.i("refresh");
            getMessages();
        }
    };

    /**
     * 监听输入框
     */
    private TextWatcher inputTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String content = charSequence.toString();
            Logger.i("input content: ", content);

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String content = charSequence.toString();
            Logger.i("input content: ", content);

            // 切换扩展按钮和发送按钮
            if (content.length() > 0) {
                mPlusButton.setVisibility(View.GONE);
                mSendButton.setVisibility(View.VISIBLE);
            } else {
                mPlusButton.setVisibility(View.VISIBLE);
                mSendButton.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
            Logger.i("afterTextChanged");

            String content = editable.toString();
            if (content != null) {
                // 输入框文字变化时，发送消息输入状态消息
                BDMqttApi.sendPreviewMessage(ChatIMActivity.this, mTidOrUidOrGid, content, mThreadType);
            }
        }
    };


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mCurrentEmotionViewPagerIndex = position;
        switch (position) {
            case 0:
                mEmotionViewPagerIndicator1.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_active));
                mEmotionViewPagerIndicator2.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator3.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator4.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator5.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                break;
            case 1:
                mEmotionViewPagerIndicator1.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator2.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_active));
                mEmotionViewPagerIndicator3.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator4.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator5.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                break;
            case 2:
                mEmotionViewPagerIndicator1.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator2.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator3.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_active));
                mEmotionViewPagerIndicator4.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator5.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                break;
            case 3:
                mEmotionViewPagerIndicator1.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator2.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator3.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator4.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_active));
                mEmotionViewPagerIndicator5.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                break;
            case 4:
                mEmotionViewPagerIndicator1.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator2.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator3.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator4.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator5.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_active));
                break;
            default:
                mEmotionViewPagerIndicator1.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_active));
                mEmotionViewPagerIndicator2.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator3.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator4.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                mEmotionViewPagerIndicator5.setImageDrawable(getResources().getDrawable(R.drawable.appkefu_page_normal));
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int arg2, long arg3) {
        Logger.i("on item clicked:" + arg2);

        int emotionIndex = mCurrentEmotionViewPagerIndex * 21 + arg2;
        if ((emotionIndex + 1) % 21 == 0) {
            int index = mInputEditText.getSelectionStart();
            Editable editable = mInputEditText.getText();

            if (index >= 0 && (index - 12) >= 0) {
                char[] dest = new char[12];
                editable.getChars(index - 12, index, dest, 0);
                if (String.valueOf(dest).startsWith("appkefu_")) {
                    editable.delete(index - 12, index);
                } else {
                    editable.delete(index - 1, index);
                }
            } else if (index > 0) {
                editable.delete(index - 1, index);
            }
        } else {

            String emotionName;
            if (emotionIndex < 9) {
                emotionName = "appkefu_f00" + (emotionIndex + 1);
            } else if (emotionIndex < 99) {
                emotionName = "appkefu_f0" + (emotionIndex + 1);
            } else {
                emotionName = "appkefu_f" + (emotionIndex + 1);
            }

            int emotionImageResId = mEmotionMaps.kfEmotionIdsForGridView[emotionIndex];
            Bitmap emotionBitmap = BitmapFactory.decodeResource(getResources(), emotionImageResId);
            ImageSpan imageSpan = new ImageSpan(getApplicationContext(), emotionBitmap);
            SpannableString spannableString = new SpannableString(emotionName);
            spannableString.setSpan(imageSpan, 0, emotionName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mInputEditText.append(spannableString);
        }
    }

    /**
     * 仅监听输入框mInputEditText焦点事件
     *
     * @param view
     * @param hasFocus
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {

        // 隐藏表情和类型扩展
        if (hasFocus) {
            mEmotionLayout.setVisibility(View.GONE);
            mExtensionLayout.setVisibility(View.GONE);
        }
    }
    

    /**
     * 录音相关
     */
    private void initRecorder(Bundle savedInstanceState) {
        // /////////////////////////////////////////////////////////////////////////
        m_voiceRecorder = new KFRecorder(this);
        m_voiceRecorder.setOnStateChangedListener(this);
        m_voiceRecordReceiver = new RecorderReceiver();
        m_voiceRecordRemainingTimeCalculator = new KFRemainingTimeCalculator();

        if (savedInstanceState != null) {
            Bundle recorderState = savedInstanceState.getBundle(BDCoreConstant.RECORDER_STATE_KEY);
            if (recorderState != null) {
                m_voiceRecorder.restoreState(recorderState);
                m_voiceRecordMaxFileSize = recorderState.getLong(BDCoreConstant.MAX_FILE_SIZE_KEY, -1);
            }
        }
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // //////////////////////////////////////////////////////////////////////////
    }


    private BroadcastReceiver m_voiceRecordSDCardMountEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            m_voiceRecorder.reset();
        }
    };

    private class RecorderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(KFRecorderService.RECORDER_SERVICE_BROADCAST_STATE)) {
                boolean isRecording = intent.getBooleanExtra(KFRecorderService.RECORDER_SERVICE_BROADCAST_STATE, false);
                m_voiceRecorder.setState(isRecording ? KFRecorder.RECORDING_STATE : KFRecorder.IDLE_STATE);

            } else if (intent.hasExtra(KFRecorderService.RECORDER_SERVICE_BROADCAST_ERROR)) {
                int error = intent.getIntExtra(KFRecorderService.RECORDER_SERVICE_BROADCAST_ERROR, 0);
                m_voiceRecorder.setError(error);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (m_voiceRecorder.sampleLength() == 0)
            return;

        Bundle recorderState = new Bundle();
        if (m_voiceRecorder.state() != KFRecorder.RECORDING_STATE) {
            m_voiceRecorder.saveState(recorderState);
        }

        recorderState.putLong(BDCoreConstant.MAX_FILE_SIZE_KEY, m_voiceRecordMaxFileSize);
        outState.putBundle(BDCoreConstant.RECORDER_STATE_KEY, recorderState);
    }

    private void stopAudioPlayback() {
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        sendBroadcast(i);
    }

    @SuppressLint("InlinedApi")
    private void startRecording(String voiceName) {

        m_voiceRecordRemainingTimeCalculator.reset();
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Logger.i("appkefu_record_voice_insert_sdcard");
            
        } else if (!m_voiceRecordRemainingTimeCalculator.diskSpaceAvailable()) {
            Logger.i("appkefu_record_voice_sdcard_full");
            
        } else {
            stopAudioPlayback();
            m_voiceRecordRemainingTimeCalculator.setBitRate(BDCoreConstant.BITRATE_AMR);
            m_voiceRecorder.startRecording(MediaRecorder.OutputFormat.AMR_NB, voiceName,
                    BDCoreConstant.EXT_AMR, false, m_voiceRecordMaxFileSize);

            if (m_voiceRecordMaxFileSize != -1) {
                m_voiceRecordRemainingTimeCalculator.setFileSizeLimit(
                        m_voiceRecorder.sampleFile(), m_voiceRecordMaxFileSize);
            }
        }

    }

    private Runnable voiceRecordingRefreshAMPTaskThread = new Runnable() {
        public void run() {
            updateRecordVoiceAMP();
            m_voiceRecordHandler.postDelayed(
                    voiceRecordingRefreshAMPTaskThread,
                    VOICE_RECORDING_REFRESH_AMP_INTERVAL);
        }
    };

    private void startVoiceRecording() {
        mIsRecording = true;
        mRecordVoiceLayout.setVisibility(View.VISIBLE);
        m_startRecordingTimestamp = System.currentTimeMillis();
        m_voiceRecordingVoiceFileName = BDCoreUtils.uuid();

        startRecording(m_voiceRecordingVoiceFileName);
        m_voiceRecordHandler.postDelayed(voiceRecordingRefreshAMPTaskThread, VOICE_RECORDING_REFRESH_AMP_INTERVAL);
    }

    private void stopVoiceRecording() {
        mIsRecording = false;
        mRecordVoiceLayout.setVisibility(View.GONE);
        m_voiceRecordHandler.removeCallbacks(voiceRecordingRefreshAMPTaskThread);
        m_voiceRecorder.stop();
        m_voiceRecorder.reset();
        mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp1);
    }

    private void updateRecordVoiceAMP() {
        double amp = m_voiceRecorder.getAmplitude();
        switch ((int) amp) {
            case 0:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp1);
                break;
            case 1:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp2);
                break;
            case 2:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp3);
                break;
            case 3:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp4);
                break;
            case 4:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp5);
                break;
            case 5:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp6);
                break;
            case 6:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp7);
                break;
            default:
                mRecordVoiceHintAMPImageView.setImageResource(R.drawable.appkefu_voice_rcd_hint_amp7);
                break;
        }
    }

    /**
     * KFRecorder.OnStateChangedListener
     */
    @Override
    public void onStateChanged(int state) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onError(int error) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == BDUiConstant.SELECT_FILE) {
            //
            if (resultCode == Activity.RESULT_OK) {

                String filePath;

                Uri uri = data.getData();
                if ("file".equalsIgnoreCase(uri.getScheme())){//使用第三方应用打开
                    filePath = uri.getPath();
                    Toast.makeText(this,filePath,Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                    filePath = BDFileUtils.getPath(ChatIMActivity.this, uri);
                    Toast.makeText(this, filePath, Toast.LENGTH_SHORT).show();
                } else {//4.4以下下系统调用方法
                    filePath = BDFileUtils.getRealPathFromURI(ChatIMActivity.this, uri);
                    Toast.makeText(ChatIMActivity.this, filePath, Toast.LENGTH_SHORT).show();
                }

                Logger.i("filePath:" + filePath);

                // 上传、发送文件
                uploadFile(filePath, BDCoreUtils.uuid());
            }
        }
    }

    /**
     * 发送文本消息
     *
     * @param content
     */
    private void sendTextMessage(String content) {

        if (!BDMqttApi.isConnected(this)) {
            Toast.makeText(this, "网络断开，请稍后重试", Toast.LENGTH_LONG).show();
            return;
        }

        // 自定义本地消息id，用于判断消息发送状态. 消息通知或者回调接口中会返回此id
        final String localId = BDCoreUtils.uuid();

        // 插入本地消息
        mRepository.insertTextMessageLocal(mTidOrUidOrGid, mWorkGroupWid, content, localId, mThreadType);

        // 1. 异步发送文字消息
        BDMqttApi.sendTextMessage(this, mTidOrUidOrGid, content, localId, mThreadType);

        // 同步发送消息(推荐)
//        BDCoreApi.sendTextMessage(this, mTidOrUidOrGid, content, localId, mThreadType, new BaseCallback() {
//
//            @Override
//            public void onSuccess(JSONObject object) {
//                //
//                try {
//
//                    int status_code = object.getInt("status_code");
//                    if (status_code == 200) {
//
//                        String localId = object.getJSONObject("data").getString("localId");
//                        Logger.i("callback localId: " + localId);
//
//                        // TODO: 更新消息发送状态为成功
//                        mRepository.updateMessageStatusSuccess(localId);
//
//                    } else {
//
//                        // 修改本地消息发送状态为error
//                        mRepository.updateMessageStatusError(localId);
//
//                        // 发送消息失败
//                        String message = object.getString("message");
//                        Toast.makeText(ChatIMActivity.this, message, Toast.LENGTH_LONG).show();
//                    }
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onError(JSONObject object) {
//                // 发送消息失败
//                Toast.makeText(ChatIMActivity.this, "发送消息失败", Toast.LENGTH_LONG).show();
//            }
//        });
    }

    /**
     * 上传并发送图片
     *
     *  {"message":"upload image",
     *  "status_code":200,
     *  "data":"http://chainsnow.oss-cn-shenzhen.aliyuncs.com/images/201808281417141_20180829105542.jpg"}
     *
     * @param filePath
     * @param fileName
     */
    private void uploadImage(String filePath, String fileName) {

        BDCoreApi.uploadImage(this, filePath, fileName, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

                try {

                    // TODO: 无客服在线时，禁止发送图片

                    // TODO: 收到客服关闭会话 或者 自动关闭会话消息之后，禁止访客发送消息

                    // 自定义本地消息id，用于判断消息发送状态。消息通知或者回调接口中会返回此id
                    final String localId = BDCoreUtils.uuid();

                    String imageUrl = object.getString("data");

                    // 插入本地消息
                    mRepository.insertImageMessageLocal(mTidOrUidOrGid, mWorkGroupWid, imageUrl, localId, mThreadType);

                    // 发送消息方式有两种：1. 异步发送消息，通过监听通知来判断是否发送成功，2. 同步发送消息，通过回调判断消息是否发送成功
                    // 1. 异步发送图片消息
                    // BDMqttApi.sendImageMessage(ChatIMActivity.this, mTidOrUidOrGid, image_url, localId, mThreadType);

                    // 2. 同步发送图片消息(推荐)
                    BDCoreApi.sendImageMessage(ChatIMActivity.this, mTidOrUidOrGid, imageUrl, localId, mThreadType, new BaseCallback() {

                        @Override
                        public void onSuccess(JSONObject object) {
                            //
                            try {

                                int status_code = object.getInt("status_code");
                                if (status_code == 200) {

                                    String localId = object.getJSONObject("data").getString("localId");
                                    Logger.i("callback localId: " + localId);

                                    // TODO: 更新消息发送状态为成功
                                    mRepository.updateMessageStatusSuccess(localId);

                                    // 发送成功
                                } else {

                                    // 修改本地消息发送状态为error
                                    mRepository.updateMessageStatusError(localId);

                                    // 发送消息失败
                                    String message = object.getString("message");
                                    Toast.makeText(ChatIMActivity.this, message, Toast.LENGTH_LONG).show();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(JSONObject object) {
                            // 发送消息失败
                            Toast.makeText(ChatIMActivity.this, "发送消息失败", Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(JSONObject object) {
                Toast.makeText(getApplicationContext(), "上传图片失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 上传并发送语音
     *
     * @param filePath 路径
     * @param fileName 文件名
     */
    private void uploadVoice(String filePath, String fileName, final int voiceLength) {

        BDCoreApi.uploadVoice(this, filePath, fileName, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

                try {

                    // TODO: 无客服在线时，禁止发送语音

                    // TODO: 收到客服关闭会话 或者 自动关闭会话消息之后，禁止访客发送消息

                    // 自定义本地消息id，用于判断消息发送状态。消息通知或者回调接口中会返回此id
                    final String localId = BDCoreUtils.uuid();
                    String voiceUrl = object.getString("data");

                    // 插入本地消息
                    mRepository.insertVoiceMessageLocal(mTidOrUidOrGid, mWorkGroupWid, voiceUrl, localId, mThreadType, voiceLength);

                    // TODO: 2. 同步发送消息(推荐)
                    BDCoreApi.sendVoiceMessage(ChatIMActivity.this, mTidOrUidOrGid, voiceUrl, localId, mThreadType, voiceLength, new BaseCallback() {

                        @Override
                        public void onSuccess(JSONObject object) {
                            //
                            try {

                                int status_code = object.getInt("status_code");
                                if (status_code == 200) {

                                    String localId = object.getJSONObject("data").getString("localId");
                                    Logger.i("callback localId: " + localId);

                                    // TODO: 更新消息发送状态为成功
                                    mRepository.updateMessageStatusSuccess(localId);

                                    // 发送成功
                                } else {

                                    // 修改本地消息发送状态为error
                                    mRepository.updateMessageStatusError(localId);

                                    // 发送消息失败
                                    String message = object.getString("message");
                                    Toast.makeText(ChatIMActivity.this, message, Toast.LENGTH_LONG).show();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(JSONObject object) {
                            // 发送消息失败
                            Toast.makeText(ChatIMActivity.this, "发送消息失败", Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(JSONObject object) {

                Toast.makeText(ChatIMActivity.this, "上传语音失败", Toast.LENGTH_LONG).show();
            }

        });
    }


    /**
     * 上传并发送文件
     *
     * @param filePath
     * @param fileName
     */
    private void uploadFile(String filePath, String fileName) {

        BDCoreApi.uploadFile(this, filePath, fileName, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

                try {
                    // 自定义本地消息id，用于判断消息发送状态。消息通知或者回调接口中会返回此id
                    final String localId = BDCoreUtils.uuid();
                    String fileUrl  = object.getString("data");

                    // 插入本地消息
                    mRepository.insertFileMessageLocal(mTidOrUidOrGid, mWorkGroupWid, fileUrl, localId, mThreadType, "doc", "fileName", "fileSize");

                    // 同步发送文件消息
                    BDCoreApi.sendFileMessage(ChatIMActivity.this, mTidOrUidOrGid, fileUrl,  localId,  mThreadType, "doc", "fileName", "fileSize", new BaseCallback() {

                        @Override
                        public void onSuccess(JSONObject object) {
                            //
                            try {

                                int status_code = object.getInt("status_code");
                                if (status_code == 200) {

                                    String localId = object.getJSONObject("data").getString("localId");
                                    Logger.i("callback localId: " + localId);

                                    // TODO: 更新消息发送状态为成功
                                    mRepository.updateMessageStatusSuccess(localId);


                                } else {
                                    // 修改本地消息发送状态为error
                                    mRepository.updateMessageStatusError(localId);

                                    // 发送消息失败
                                    String message = object.getString("message");
                                    Toast.makeText(ChatIMActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(JSONObject object) {
                            // 发送消息失败
                            Toast.makeText(ChatIMActivity.this, "发送消息失败", Toast.LENGTH_LONG).show();
                        }
                    });


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(JSONObject object) {

            }
        });
    }


    /**
     * 发送红包消息
     * @param money 金额
     */
    private void sendRedPacketMessage(String money) {

        // 自定义本地消息id，用于判断消息发送状态. 消息通知或者回调接口中会返回此id
        final String localId = BDCoreUtils.uuid();

        // 插入本地消息
        mRepository.insertRedPacketMessageLocal(mTidOrUidOrGid, mWorkGroupWid, money, localId, mThreadType);

        //
        BDCoreApi.sendRedPacketMessage(this, mTidOrUidOrGid, money, localId, mThreadType, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {
                //
                try {

                    int status_code = object.getInt("status_code");
                    if (status_code == 200) {

                        String localId = object.getJSONObject("data").getString("localId");
                        Logger.i("callback localId: " + localId);

                        // TODO: 更新消息发送状态为成功
                        mRepository.updateMessageStatusSuccess(localId);

                        // 发送成功
                    } else {

                        // 修改本地消息发送状态为error
                        mRepository.updateMessageStatusError(localId);

                        // 发送消息失败
                        String message = object.getString("message");
                        Toast.makeText(ChatIMActivity.this, message, Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(JSONObject object) {
                // 发送消息失败
                Toast.makeText(ChatIMActivity.this, "发送消息失败", Toast.LENGTH_LONG).show();
            }

        });
    }

    /**
     * 发送商品消息等
     * @param custom
     */
    private void sendCommodityMessage(String custom) {

        // 自定义本地消息id，用于判断消息发送状态. 消息通知或者回调接口中会返回此id
        final String localId = BDCoreUtils.uuid();

        // 插入本地消息
        mRepository.insertCommodityMessageLocal(mTidOrUidOrGid, mWorkGroupWid, custom, localId, mThreadType);

        // 发送商品
        BDCoreApi.sendCommodityMessage(this, mTidOrUidOrGid, custom, localId, mThreadType, new BaseCallback() {
            @Override
            public void onSuccess(JSONObject object) {
                //
                try {

                    int status_code = object.getInt("status_code");
                    if (status_code == 200) {

                        String localId = object.getJSONObject("data").getString("localId");
                        Logger.i("callback localId: " + localId);

                        // TODO: 更新消息发送状态为成功
                        mRepository.updateMessageStatusSuccess(localId);

                        // 发送成功
                    } else {

                        // 修改本地消息发送状态为error
                        mRepository.updateMessageStatusError(localId);

                        // 发送消息失败
                        String message = object.getString("message");
                        Toast.makeText(ChatIMActivity.this, message, Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(JSONObject object) {
                // 发送消息失败
                Toast.makeText(ChatIMActivity.this, "发送消息失败", Toast.LENGTH_LONG).show();
            }
        });
    }


    /**
     * 开启、关闭发送阅后即焚消息
     */
    private void toggleDestroyAfterReading() {

        boolean isDestroyAfterReadingEnabled = mPreferenceManager.getDestroyAfterReading(mTidOrUidOrGid, mThreadType);
        int destroyAfterLength = mPreferenceManager.getDestroyAfterLength(mTidOrUidOrGid, mThreadType);
        final String[] items = new String[]{isDestroyAfterReadingEnabled ? "开启("+destroyAfterLength+"秒)" : "开启", "关闭"};
        final int checkedIndex = isDestroyAfterReadingEnabled ? 0 : 1;
        new QMUIDialog.CheckableDialogBuilder(this)
                .setTitle("阅后即焚")
                .setCheckedIndex(checkedIndex)
                .addItems(items, (dialogInterface, which) -> {
                    Toast.makeText(ChatIMActivity.this,  items[which] + "阅后即焚", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();

                    boolean enabled = which == 0 ? true : false;
                    mPreferenceManager.setDestroyAfterReading(mTidOrUidOrGid, mThreadType, enabled);

                    if (enabled) {
                        // 设置长度
                        setDestroyAfterLength();
                    }

                }).show();
    }

    private void setDestroyAfterLength() {

        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(ChatIMActivity.this);
        builder.setTitle("阅后即焚")
                .setPlaceholder("输入时长(秒)")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", (dialog, index) -> {})
                .addAction("确定", (dialog, index) -> {
                    final CharSequence text = builder.getEditText().getText();
                    if (text != null && text.length() > 0) {

                        // 检查是否有效数字 且 大于0
                        if (BDCoreUtils.isNumeric(text.toString()) && Integer.valueOf(text.toString()) > 0) {
                            mPreferenceManager.setDestroyAfterLength(mTidOrUidOrGid, mThreadType, Integer.valueOf(text.toString()));
                            dialog.dismiss();

                            // TODO: 设置保存到服务器端，并通知对方，在聊天界面显示通知

                        }

                    } else {
                        Toast.makeText(ChatIMActivity.this, "请填入时长", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }


    @Override
    public void onKeyboardHidden() {
        Logger.i("onKeyboardHidden");

    }

    @Override
    public void onKeyboardShown() {
        Logger.i("onKeyboardShown");
        mRecyclerView.scrollToPosition(mChatAdapter.getItemCount() - 1);
        mExtensionLayout.setVisibility(View.GONE);
        mEmotionLayout.setVisibility(View.GONE);
    }

}


