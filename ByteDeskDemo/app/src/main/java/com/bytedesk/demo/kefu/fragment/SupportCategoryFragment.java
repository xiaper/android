package com.bytedesk.demo.kefu.fragment;

import android.view.LayoutInflater;
import android.view.View;

import com.bytedesk.core.api.BDCoreApi;
import com.bytedesk.core.callback.BaseCallback;
import com.bytedesk.demo.R;
import com.bytedesk.demo.common.BaseFragment;
import com.bytedesk.demo.utils.BDDemoConst;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * TODO： 待上线
 */
public class SupportCategoryFragment extends BaseFragment {

    @BindView(R.id.topbar)
    QMUITopBarLayout mTopBar;
    @BindView(R.id.groupListView)
    QMUIGroupListView mGroupListView;

    private String mTitle = "分类详情";

    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_support_category, null);
        ButterKnife.bind(this, root);

        initTopBar();
        initGroupListView();

        // 加载分类
        getCategories();

        return root;
    }

    private void initTopBar() {
        mTopBar.addLeftBackImageButton().setOnClickListener(v -> popBackStack());
        mTopBar.setTitle(mTitle);
    }

    private void initGroupListView() {


    }


    private void getCategories() {

        BDCoreApi.getSupportCategories(getContext(), BDDemoConst.DEFAULT_TEST_ADMIN_UID, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

            }

            @Override
            public void onError(JSONObject object) {

            }
        });

    }



}
