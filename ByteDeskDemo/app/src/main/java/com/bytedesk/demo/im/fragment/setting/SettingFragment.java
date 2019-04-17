package com.bytedesk.demo.im.fragment.setting;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bytedesk.core.api.BDCoreApi;
import com.bytedesk.core.api.BDMqttApi;
import com.bytedesk.core.callback.BaseCallback;
import com.bytedesk.core.event.ProfileEvent;
import com.bytedesk.core.util.BDPreferenceManager;
import com.bytedesk.demo.R;
import com.bytedesk.demo.common.BDConstants;
import com.bytedesk.demo.common.BaseFragment;
import com.orhanobut.logger.Logger;
import com.qmuiteam.qmui.widget.QMUIRadiusImageView;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author bytedesk.com on 2018/3/26.
 */
public class SettingFragment extends BaseFragment {

    @BindView(R.id.topbar) QMUITopBarLayout mTopBar;
    @BindView(R.id.groupListView) QMUIGroupListView mGroupListView;

    private QMUIRadiusImageView avatarImageView;
    private QMUICommonListItemView profileItem;
    private QMUICommonListItemView autoItem;
    private QMUICommonListItemView acceptStatusItem;

    private BDPreferenceManager mPreferenceManager;

    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_setting, null);
        ButterKnife.bind(this, root);

        EventBus.getDefault().register(this);
        mPreferenceManager = BDPreferenceManager.getInstance(getContext());

        initTopBar();
        initGroupListView();

        return root;
    }

    /**
     *
     */
    protected void initTopBar() {
        //
        mTopBar.addLeftBackImageButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popBackStack();
            }
        });
        mTopBar.setTitle(getResources().getString(R.string.bytedesk_setting));
    }

    /**
     *
     */
    private void initGroupListView() {

        //
        avatarImageView = new QMUIRadiusImageView(getContext());
        Glide.with(getContext()).load(mPreferenceManager.getAvatar()).into(avatarImageView);

        ////
        profileItem = mGroupListView.createItemView(mPreferenceManager.getNickname());
        profileItem.setOrientation(QMUICommonListItemView.VERTICAL);
        profileItem.setDetailText(mPreferenceManager.getDescription());
//        profileItem.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.icon_tabbar_contact));

        // FIXME: 无效果
        profileItem.setImageDrawable(avatarImageView.getDrawable());
        profileItem.setDetailText(mPreferenceManager.getDescription() + "：" + mPreferenceManager.getUid());

        QMUIGroupListView.newSection(getContext()).addItemView(profileItem, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.d("profile item clicked");
                showProfileSheet();
            }
        }).addTo(mGroupListView);

        ///////
        autoItem = mGroupListView.createItemView("自动回复");
        autoItem.setDetailText(mPreferenceManager.getAutoReplyContent());
        autoItem.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        acceptStatusItem = mGroupListView.createItemView("接待状态");
        acceptStatusItem.setDetailText(mPreferenceManager.getAcceptStatus());
        acceptStatusItem.setAccessoryType(QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        QMUIGroupListView.newSection(getContext()).addItemView(autoItem, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.d("autoItem item clicked");
                showAutoReplySheet();
            }
        }).addItemView(acceptStatusItem, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.d("acceptStatusItem item clicked");
                showAcceptStatusSheet();
            }
        }).addTo(mGroupListView);
    }

    /**
     *
     */
    private void showProfileSheet() {

        new QMUIBottomSheet.BottomListSheetBuilder(getActivity())
                .addItem("修改头像TODO")
                .addItem("修改昵称TODO")
                .addItem("修改签名TODO")
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        dialog.dismiss();

                    }
                })
                .build()
                .show();
    }

    /**
     *
     */
    private void showAutoReplySheet() {

        new QMUIBottomSheet.BottomListSheetBuilder(getActivity())
                .addItem(BDConstants.AUTO_REPLY_NO)
                .addItem(BDConstants.AUTO_REPLY_EAT)
                .addItem(BDConstants.AUTO_REPLY_LEAVE)
                .addItem(BDConstants.AUTO_REPLY_BACK)
                .addItem(BDConstants.AUTO_REPLY_PHONE)
                .addItem(BDConstants.AUTO_REPLY_SELF)
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        dialog.dismiss();

                        //
                        boolean isAutoReply = position == 0 ? false : true;
                        final String content = tag;
                        BDCoreApi.updateAutoReply(getContext(), isAutoReply, content, new BaseCallback() {
                            @Override
                            public void onSuccess(JSONObject object) {

                                autoItem.setDetailText(content);
                                mPreferenceManager.setAutoReplyContent(content);
                            }

                            @Override
                            public void onError(JSONObject object) {

                                // TODO: 报错提示
                                Toast.makeText(getActivity(), "设置自动回复错误", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                })
                .build()
                .show();
    }

    /**
     *
     */
    private void showAcceptStatusSheet() {
        new QMUIBottomSheet.BottomListSheetBuilder(getActivity())
                .addItem(BDConstants.STATUS_ONLINE)
                .addItem(BDConstants.STATUS_BUSY)
                .addItem(BDConstants.STATUS_REST)
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {
                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        dialog.dismiss();

                        String status;
                        final String statusText = tag;
                        if (statusText.equals(BDConstants.STATUS_ONLINE)) {
                            status = BDConstants.USER_STATUS_ONLINE;
                        } else if (statusText.equals(BDConstants.STATUS_BUSY)) {
                            status = BDConstants.USER_STATUS_BUSY;
                        } else {
                            status = BDConstants.USER_STATUS_REST;
                        }
                        //
                        BDCoreApi.setAcceptStatus(getContext(), status, new BaseCallback() {
                            @Override
                            public void onSuccess(JSONObject object) {

                                acceptStatusItem.setDetailText(statusText);
                                mPreferenceManager.setAcceptStatus(statusText);
                            }

                            @Override
                            public void onError(JSONObject object) {

                                // TODO: 报错提示
                                Toast.makeText(getActivity(), "设置在线状态错误", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .build()
                .show();
    }

    /**
     * 监听 EventBus 广播消息
     *
     * FIXME: EventBus: No subscribers registered for event class com.bytedesk.core.event.ProfileEvent
     *
     * @param profileEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProfileEvent(ProfileEvent profileEvent) {
        Logger.i("onProfileEvent");

        try {

            JSONObject infoObject = profileEvent.getJsonObject();

            mPreferenceManager.setUid(infoObject.getString("uid"));
            mPreferenceManager.setUsername(infoObject.getString("username"));
            mPreferenceManager.setNickname(infoObject.getString("nickname"));
            mPreferenceManager.setAvatar(infoObject.getString("avatar"));

            // 初始化界面
            profileItem.setText(mPreferenceManager.getNickname());
            profileItem.setDetailText(mPreferenceManager.getDescription());

            // FIXME: 无效果
            Glide.with(getContext()).load(mPreferenceManager.getAvatar()).into(avatarImageView);
            profileItem.setImageDrawable(avatarImageView.getDrawable());

            // 添加订阅主题
            String subDomain = infoObject.getString("subDomain");
            String subDomainTopic = "subDomain/" + subDomain;
            BDMqttApi.subscribeTopic(getActivity(), subDomainTopic);

            String uid = infoObject.getString("uid");
            String userTopic = "user/" + uid;
            BDMqttApi.subscribeTopic(getActivity(), userTopic);

            String contactTopic = "contact/" + uid;
            BDMqttApi.subscribeTopic(getActivity(), contactTopic);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
