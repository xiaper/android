package com.bytedesk.demo.kefu.fragment;

import android.view.LayoutInflater;
import android.view.View;

import com.bytedesk.core.api.BDCoreApi;
import com.bytedesk.core.callback.BaseCallback;
import com.bytedesk.core.room.entity.ArticleEntity;
import com.bytedesk.core.room.entity.CategoryEntity;
import com.bytedesk.demo.R;
import com.bytedesk.demo.common.BaseFragment;
import com.bytedesk.demo.utils.BDDemoConst;
import com.orhanobut.logger.Logger;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * TODO： 待上线
 */
public class SupportApiFragment extends BaseFragment {

    @BindView(R.id.bytedesk_support_api_topbar) QMUITopBarLayout mTopBar;
    @BindView(R.id.pull_to_refresh) QMUIPullRefreshLayout mPullRefreshLayout;
    @BindView(R.id.groupListView) QMUIGroupListView mGroupListView;

    private String mTitle = "帮助中心API";

    private QMUIGroupListView.Section mCategorySection;
    private QMUIGroupListView.Section mArticleSection;

    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_support_api, null);
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
        //
        mPullRefreshLayout.setOnPullListener(pullListener);

        mCategorySection = QMUIGroupListView.newSection(getContext()).setTitle("问题分类");
        mArticleSection = QMUIGroupListView.newSection(getContext()).setTitle("常见问题");
    }

    private void getCategories() {

        BDCoreApi.getSupportCategories(getContext(), BDDemoConst.DEFAULT_TEST_ADMIN_UID, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

                try {

                    JSONArray categoryArray = object.getJSONArray("data");
                    for (int i = 0; i < categoryArray.length(); i++) {
                        CategoryEntity categoryEntity = new CategoryEntity(categoryArray.getJSONObject(i));
                        Logger.i("category:" + categoryEntity.getName());

                        QMUICommonListItemView categoryItem = mGroupListView.createItemView(categoryEntity.getName());
                        mCategorySection.addItemView(categoryItem, v -> {
                            //
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mCategorySection.addTo(mGroupListView);

                // 加载常见问题
                getArticles();

            }

            @Override
            public void onError(JSONObject object) {

            }
        });

    }


    private void getArticles() {

        BDCoreApi.getSupportArticles(getContext(), BDDemoConst.DEFAULT_TEST_ADMIN_UID, new BaseCallback() {

            @Override
            public void onSuccess(JSONObject object) {

                try {

                    JSONArray articleArray = object.getJSONObject("data").getJSONArray("content");
                    for (int i = 0; i < articleArray.length(); i++) {
                        ArticleEntity articleEntity = new ArticleEntity(articleArray.getJSONObject(i));
                        Logger.i("article:" + articleEntity.getTitle());

                        QMUICommonListItemView articleItem = mGroupListView.createItemView(articleEntity.getTitle());
                        mArticleSection.addItemView(articleItem, v -> {
                            //

                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //
                mArticleSection.addTo(mGroupListView);
                //
                mPullRefreshLayout.finishRefresh();
            }

            @Override
            public void onError(JSONObject object) {

            }
        });
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
            //
            mPullRefreshLayout.finishRefresh();
            //
//            getCategories();
//            getArticles();
        }
    };

}
