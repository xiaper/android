package com.bytedesk.ui.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.bytedesk.core.api.BDCoreApi;
import com.bytedesk.core.callback.BaseCallback;
import com.bytedesk.ui.R;
import com.bytedesk.ui.util.BDUiConstant;
import com.qmuiteam.qmui.util.QMUIStatusBarHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import org.json.JSONObject;

/**
 *
 * 帮助中心
 *
 * @author bytedesk.com
 */
public class SupportCategoryActivity extends AppCompatActivity {

    private QMUITopBarLayout mTopBar;
    private QMUIGroupListView mGroupListView;

    private String mTitle = "分类详情";
    private String mUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bytedesk_activity_support_category);
        //
        mTopBar = findViewById(R.id.bytedesk_support_category_topbar);
        mGroupListView = findViewById(R.id.bytedesk_support_category_groupListView);
        //
        mUid = getIntent().getStringExtra(BDUiConstant.EXTRA_UID);
        QMUIStatusBarHelper.translucent(this);

        initTopBar();
        initGroupListView();

        // 加载分类
        getCategories();
    }

    private void initTopBar() {
        mTopBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        mTopBar.setTitle(mTitle);
    }

    private void initGroupListView() {


    }

    private void getCategories() {

        BDCoreApi.getSupportCategories(this, mUid, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

            }

            @Override
            public void onError(JSONObject object) {

            }
        });

    }

}
