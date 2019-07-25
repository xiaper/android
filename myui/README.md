# 5分钟集成自定义UI

- [Demo](https://github.com/Bytedesk/bytedesk-android/tree/master/Tutorial/myui)

## 准备工作

- 到[萝卜丝官网](https://www.bytedesk.com/admin#/register)注册管理员账号，并登录管理后台。
- 到 所有设置->应用管理->App 添加应用，填写相关信息之后点击确定，在生成记录中可见appkey，后面会用到。
- 到 所有设置->客服管理->客服账号 添加客服账号。注意：生成记录中有一列 ‘唯一ID(uid)’ 会在指定客服接口中使用
- 到 所有设置->客服管理->技能组 添加技能组，并可将客服账号添加到相关技能组。注意：生成记录中有一列 ‘唯一ID（wId）’ 会在工作组会话中用到

## 开始集成

> 第一步：在总项目build.gradle的 allprojects -> repositories 添加

```java
maven {
    url  "https://dl.bintray.com/jackning/maven"
}
```

> 修改完后，效果如下：

```java
allprojects {
    repositories {
        jcenter()
        google()
        maven {
            url  "https://dl.bintray.com/jackning/maven"
        }
    }
}
```

文件末尾添加

```java
ext {
    // Sdk and tools
    minSdkVersion = 18
    targetSdkVersion = 26
    compileSdkVersion = 28
    buildToolsVersion = '28.0.3'
    supportLibVersion = '28.0.0'

    appcompatVersion= '1.1.0-rc01'
    butterknifeVersion = '10.1.0'
    recyleviewxVersion = '1.1.0-beta01'
    stethoVersion = '1.5.1'

    archLifecycleVersion = "2.2.0-alpha02"
    archRoomVersion = "2.1.0"
}
```

> 第二步：复制bytedesk-ui库到自己项目目录

开发者可以参考demo，直接从demo目录中复制到自己项目中

> 第三步：在总项目的settings.gradle末尾添加

```java
include ':bytedesk-ui'
```

添加完毕后，同步 `Sync Now`

> 第四步：在module的build.gradle android{}添加

```java
android {
    ...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    ...
}
```

> 第五步：在module的build.gradle dependencies{}添加

```java
// 萝卜丝第三步
// 加载萝卜丝核心库
implementation 'com.bytedesk:core:2.0.2'
// 加载萝卜丝自定义UI库
implementation project(':bytedesk-ui')

// 腾讯QMUI界面库
// http://qmuiteam.com/android/page/start.html
// https://bintray.com/chanthuang/qmuirepo
implementation 'com.qmuiteam:qmui:1.4.0'
```

> 第六步：AndroidManifest.xml添加权限

```xml
<!--添加萝卜丝权限-->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

> 第七步：AndroidManifest.xml添加Activity和Service

```xml
<!--萝卜丝bytedesk.com代码 开始-->
<!--对话页面-->
<activity
    android:name="com.bytedesk.ui.activity.ChatKFActivity"
    android:screenOrientation="portrait"
    android:theme="@style/AppTheme.ByteDesk"/>
<!--长连接service-->
<service android:name="com.bytedesk.paho.android.service.MqttService"/>
<!--下载录音-->
<service android:name="com.bytedesk.core.service.BDDownloadService"/>
<!--录音以及播放-->
<service android:name="com.bytedesk.ui.recorder.KFRecorderService"/>
<!--./萝卜丝bytedesk.com代码 结束-->
```

> 第八步：在样式文件styles.xml中添加

```xml
<!--添加萝卜丝样式 开始-->
<style name="AppTheme.ByteDesk" parent="QMUI.Compat.NoActionBar">
    <!--导航栏样式-->
    <item name="QMUITopBarStyle">@style/ByteDeskTopBar</item>
</style>
<style name="ByteDeskTopBar" parent="QMUI.TopBar">
    <!--导航栏背景颜色-->
    <item name="qmui_topbar_bg_color">@color/app_color_blue</item>
    <!--导航栏字体颜色-->
    <item name="qmui_topbar_title_color">@color/qmui_config_color_white</item>
    <item name="qmui_topbar_subtitle_color">@color/qmui_config_color_white</item>
    <item name="qmui_topbar_text_btn_color_state_list">@color/qmui_config_color_white</item>
    <!--导航栏高度-->
    <item name="qmui_topbar_height">48dp</item>
    <item name="qmui_topbar_image_btn_height">48dp</item>
</style>
<!--添加萝卜丝样式 结束-->
```

> 第九部：初始化UI和建立长连接

参考demo中MainActivity.java

```java
// 初始化萝卜丝UI界面库
BDUiApi.init(this);
// 具体代码请参考MainActivity.java
anonymousLogin();
```

> 第十步：开始客服会话

```java
// 打开客服对话界面
BDUiApi.startWorkGroupChatActivity(context, wId, "工作组客服");
```

## 集成完毕
