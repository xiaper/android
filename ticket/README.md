# 5分钟集成工单

## 准备工作

- 到[萝卜丝官网](https://www.bytedesk.com/admin#/register)注册管理员账号，并登录管理后台。
- 到 所有设置->应用管理->App 添加应用，填写相关信息之后点击确定，在生成记录中可见appkey，后面会用到。
- 到 所有设置->客服管理->客服账号 添加客服账号。注意：生成记录中有一列 ‘唯一ID(uid)’ 会在指定客服接口中使用
- 到 所有设置->客服管理->技能组 添加技能组，并可将客服账号添加到相关技能组。注意：生成记录中有一列 ‘唯一ID（wId）’ 会在工作组会话中用到

## 开始集成

> 第一步：在项目build.gradle的 allprojects -> repositories 添加

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

> 第二步：在module的build.gradle android{}添加

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

> 第三步：在module的build.gradle dependencies{}添加

```java
// 萝卜丝第三步
// 加载萝卜丝核心库
implementation 'com.bytedesk:core:2.0.1'
// 加载萝卜丝默认UI库
implementation 'com.bytedesk:ui:2.0.1'

// 腾讯QMUI界面库
// http://qmuiteam.com/android/page/start.html
// https://bintray.com/chanthuang/qmuirepo
implementation 'com.qmuiteam:qmui:1.4.0'
```

> 第四步：AndroidManifest.xml添加权限

```xml
<!--添加萝卜丝权限-->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

> 第五步：AndroidManifest.xml添加Activity和Service

```xml
<!--萝卜丝bytedesk.com代码 开始-->
<!--提交工单-->
<activity
    android:name="com.bytedesk.ui.activity.TicketActivity"
    android:screenOrientation="portrait"
    android:theme="@style/AppTheme.ByteDesk"/>
<!--./萝卜丝bytedesk.com代码 结束-->
```

> 第六步：在样式文件styles.xml中添加

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

> 第七部：初始化UI和建立长连接

参考demo中MainActivity.java

```java
// 初始化萝卜丝UI界面库
BDUiApi.init(this);
// 具体代码请参考MainActivity.java
anonymousLogin();
```

> 第八步：开始客服会话

```java
// 打开工单页面
// 获取管理员adminUid, 登录后台->所有设置->客服账号->管理员账号(唯一ID(uid))列
BDUiApi.startTicketActivity(context, adminUId);
```

## 集成完毕
