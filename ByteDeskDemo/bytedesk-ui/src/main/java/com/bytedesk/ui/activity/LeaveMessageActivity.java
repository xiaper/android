package com.bytedesk.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;

import com.bytedesk.core.api.BDCoreApi;
import com.bytedesk.core.callback.BaseCallback;
import com.bytedesk.ui.R;
import com.bytedesk.ui.util.BDUiConstant;
import com.qmuiteam.qmui.util.QMUIStatusBarHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import org.json.JSONException;
import org.json.JSONObject;


public class LeaveMessageActivity extends AppCompatActivity {

    QMUITopBarLayout mTopBar;
    Button mSubmitButton;

    private String workGroupWid;
    private String type;
    private String agentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bytedesk_activity_leave_message);

        workGroupWid = getIntent().getStringExtra(BDUiConstant.EXTRA_WID);
        type = getIntent().getStringExtra(BDUiConstant.EXTRA_REQUEST_TYPE);
        agentUid = getIntent().getStringExtra(BDUiConstant.EXTRA_AID);

        initTopBar();
        initView();
    }

    private void initTopBar() {
        //
        mTopBar = findViewById(R.id.bytedesk_leave_msg_topbarlayout);
        mTopBar.setTitle("留言");
        mTopBar.setBackgroundColor(getResources().getColor(R.color.albumColorPrimary));
        mTopBar.addLeftBackImageButton().setOnClickListener(view -> finish());
        //
        QMUIStatusBarHelper.translucent(this);
    }

    private void initView() {
        //
        mSubmitButton = findViewById(R.id.bytedesk_leave_msg_submit_button);
        mSubmitButton.setOnClickListener(view -> submit());
    }


    /**
     * TODO: 待完善UI
     * 保存留言
     */
    private void submit() {
        //
        final Context context = this;
        BDCoreApi.leaveMessageDxz(this, type, workGroupWid, agentUid,
                "手机", "邮箱", "昵称", "所属区域", "意向国家", "content",
                new BaseCallback() {
                    @Override
                    public void onSuccess(JSONObject object) {

                        try {

                            int status_code = object.getInt("status_code");
                            String message = object.getString("message");
                            if (status_code == 200) {

                                // 留言成功
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show();

                            } else {

                                // 留言失败
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(JSONObject object) {

                        Toast.makeText(context, "留言失败", Toast.LENGTH_LONG).show();
                    }
                });
    }







}
