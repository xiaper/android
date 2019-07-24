package com.bytedesk.demo.common;

import android.view.LayoutInflater;
import android.view.View;

import com.bytedesk.core.api.BDConfig;
import com.bytedesk.demo.R;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 配置
 */
public class ServerFragment extends BaseFragment {

    @BindView(R.id.topbar) QMUITopBarLayout mTopBar;
    @BindView(R.id.groupListView) QMUIGroupListView mGroupListView;

    @Override
    protected View onCreateView() {
        View root = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_server, null);
        ButterKnife.bind(this, root);

        initTopBar();
        initGroupListView();

        return root;
    }

    private void initTopBar() {
        //
        mTopBar.addLeftBackImageButton().setOnClickListener(view -> popBackStack());
        //
        mTopBar.addRightImageButton(R.mipmap.icon_topbar_about, R.id.topbar_right_about_button).setOnClickListener(view -> {
            AboutFragment fragment = new AboutFragment();
            startFragment(fragment);
        });
        //
        mTopBar.setTitle(getResources().getString(R.string.bytedesk_self_server));
    }

    private void initGroupListView() {

        QMUICommonListItemView authAddressItem = mGroupListView.createItemView("地址");
        authAddressItem.setDetailText(BDConfig.getInstance(getContext()).getRestApiHost());
        QMUIGroupListView.newSection(getContext())
                .setTitle("REST服务器,注意：以'/'结尾")
                .addItemView(authAddressItem, view -> {

                    // 修改为自己的服务器地址，注意：地址以 http或https开头, '/'结尾
//                    BDConfig.getInstance(getContext()).setRestApiHost("https://api.bytedesk.com/");

                }).addTo(mGroupListView);

        //
        QMUICommonListItemView stunAddressItem = mGroupListView.createItemView("STUN");
        stunAddressItem.setDetailText(BDConfig.getInstance(getContext()).getWebRTCStunServer());
        QMUICommonListItemView turnAddressItem = mGroupListView.createItemView("TURN");
        turnAddressItem.setDetailText(BDConfig.getInstance(getContext()).getWebRTCTurnServer());
        QMUICommonListItemView turnUsernameItem = mGroupListView.createItemView("username");
        turnUsernameItem.setDetailText(BDConfig.getInstance(getContext()).getWebrtcTurnUsername());
        QMUICommonListItemView turnPasswordItem = mGroupListView.createItemView("password");
        turnPasswordItem.setDetailText(BDConfig.getInstance(getContext()).getWebrtcTurnPassword());
        QMUIGroupListView.newSection(getContext())
                .setTitle("STUN/TURN for WebRTC")
                .addItemView(stunAddressItem, view -> {

                })
                .addItemView(turnAddressItem, v -> {

                })
                .addItemView(turnUsernameItem, v -> {

                })
                .addItemView(turnPasswordItem, v -> {

                }).addTo(mGroupListView);

        //
        QMUICommonListItemView mqAddressItem = mGroupListView.createItemView("地址");
        mqAddressItem.setDetailText(BDConfig.getInstance(getContext()).getMqttHost());
        QMUICommonListItemView mqPortItem = mGroupListView.createItemView("端口号");
        mqPortItem.setDetailText(String.valueOf(BDConfig.getInstance(getContext()).getMqttPort()));
        QMUICommonListItemView mqAuthUsername = mGroupListView.createItemView("用户名");
        mqAuthUsername.setDetailText(BDConfig.getInstance(getContext()).getMqttAuthUsername());
        QMUICommonListItemView mqAuthPassword = mGroupListView.createItemView("密码");
        mqAuthPassword.setDetailText(BDConfig.getInstance(getContext()).getMqttAuthPassword());
        QMUIGroupListView.newSection(getContext())
                .setTitle("消息服务器, 注意：地址没有http前缀")
                .addItemView(mqAddressItem, view -> {

//              修改为自己消息服务器地址, 注意：地址没有http前缀
//              BDConfig.getInstance(getContext()).setMqttHost("mq.bytedesk.com");

                }).addItemView(mqPortItem, view -> {

//              修改为自己消息服务器端口号
//             BDConfig.getInstance(getContext()).setMqttPort(1883);

        }).addItemView(mqAuthUsername, view -> {

//              修改为自己消息服务器用户名
//              BDConfig.getInstance(getContext()).setMqttAuthUsername("mqtt_android");

        }).addItemView(mqAuthPassword, view -> {

//              修改为自己消息服务器密码
//              BDConfig.getInstance(getContext()).setMqttAuthPassword("mqtt_android");

        }).addTo(mGroupListView);

        //
        QMUICommonListItemView restoreDefault = mGroupListView.createItemView("恢复默认值");
        QMUIGroupListView.newSection(getContext()).addItemView(restoreDefault, view -> {

            // 恢复默认值
            BDConfig.getInstance(getContext()).restoreDefault();

            //
            authAddressItem.setDetailText(BDConfig.getInstance(getContext()).getRestApiHost());
            mqAddressItem.setDetailText(BDConfig.getInstance(getContext()).getMqttHost());
            mqPortItem.setDetailText(String.valueOf(BDConfig.getInstance(getContext()).getMqttPort()));
            mqAuthUsername.setDetailText(BDConfig.getInstance(getContext()).getMqttAuthUsername());
            mqAuthPassword.setDetailText(BDConfig.getInstance(getContext()).getMqttAuthPassword());

        }).addTo(mGroupListView);

    }



}
