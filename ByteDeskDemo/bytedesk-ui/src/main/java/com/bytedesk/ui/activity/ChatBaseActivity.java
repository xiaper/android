package com.bytedesk.ui.activity;

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.bytedesk.core.api.BDCoreApi;
import com.bytedesk.core.callback.BaseCallback;
import com.bytedesk.core.event.LongClickEvent;
import com.bytedesk.core.util.BDCoreUtils;
import com.orhanobut.logger.Logger;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

/**
 * @author bytedesk.com on 2019/2/27
 */
public class ChatBaseActivity extends AppCompatActivity {

    /**
     * 监听 EventBus 广播消息: 长按消息
     *
     * @param longClickEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLongClickEvent(LongClickEvent longClickEvent) {
        Logger.i("LongClickEvent");

        new QMUIBottomSheet.BottomListSheetBuilder(this)
                .addItem("复制")
                .addItem("删除")
                .addItem("撤回")
                .setOnSheetItemClickListener(new QMUIBottomSheet.BottomListSheetBuilder.OnSheetItemClickListener() {

                    @Override
                    public void onClick(QMUIBottomSheet dialog, View itemView, int position, String tag) {
                        switch (position) {
                            case 0:
                                String content = longClickEvent.getMessageEntity().getContent();
                                Logger.d("copy:" + content);
                                BDCoreUtils.copy(getBaseContext(), content);

                                break;
                            case 1:
                                String deleteMid = longClickEvent.getMessageEntity().getMid();
                                Logger.d("delete:" + deleteMid);

                                BDCoreApi.markDeletedMessage(getBaseContext(), deleteMid, new BaseCallback() {

                                    @Override
                                    public void onSuccess(JSONObject object) {

                                    }

                                    @Override
                                    public void onError(JSONObject object) {

                                    }
                                });

                                break;
                            case 2:
                                String withDrawMid = longClickEvent.getMessageEntity().getMid();
                                Logger.d("withDraw: " + withDrawMid);

                                BDCoreApi.withdrawMessage(getBaseContext(), withDrawMid, new BaseCallback() {

                                    @Override
                                    public void onSuccess(JSONObject object) {

                                    }

                                    @Override
                                    public void onError(JSONObject object) {

                                    }
                                });

                                break;
                        }
                        dialog.dismiss();
                    }
                })
                .build().show();
    }

}
