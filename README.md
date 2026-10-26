# 待课

安卓学习提醒工具，原名“雨课作业提醒”。当前公开源码版本：**0.9.31**。

- 自动检查长江雨课堂未完成作业，按截止时间展示并通知提醒。
- 支持华中农业大学学校账号导入课表、查询已公布考试，以及多张周课表截图本机识别。
- 上课、扫码和考试提醒时间可设置；课程可手动编辑、长按多选删除。
- 桌面小组件支持作业或课表优先，列表上下滑动、单日课表格和后台检查作业；作业可淡化并从组件隐藏。
- 各手机独立保存账号会话、课表和设置，不提供跨设备同步。

本项目为非官方应用。登录、验证码由使用者在官方页面完成。截图OCR在手机本机运行；查询作业、课表、考试和应用更新需要联网。

## 安装

在 [Releases](https://github.com/yzhao2864-boop/rain-reminder/releases) 下载 APK。已有用户可在“我的 → 应用更新”检查新版，直接覆盖安装。

APK沿用 `RainReminder-版本-test.apk` 文件名以兼容旧版更新器；安装后的应用名称为“待课”。

## 源码结构

- `app/src/cn/local/rainreminder/`：安卓 Java 源码。
- `app/res/`、`app/AndroidManifest.xml`：界面资源及应用配置。
- `app/assets/`：网页读取脚本、离线识别模型与字典。
- `app/libs/`、`app/native/`：构建所需第三方 Java 和 ARM64/x86_64 运行库。
- `app/licenses/`、`app/THIRD_PARTY_NOTICES.md`：第三方依赖来源及许可证。
- `update.json`：供已安装应用查询版本的更新信息。

源码随附离线模型及运行库，总体约67 MB。GitHub自动生成的源码压缩包从本次源码提交起包含上述文件；此前发布标签仅含当时的仓库内容，不代表旧版完整App源码。

## Windows构建

本项目使用自定义PowerShell构建脚本，未使用Gradle。

准备JDK（建议17）和官方Android SDK：安装Android SDK Platform 35、Build Tools 35.0.0；将JDK的`java`、`javac`、`jar`、`keytool`加入PATH。

在仓库根目录运行：

```powershell
$env:ANDROID_SDK_ROOT = 'C:\Android\Sdk'
powershell -File .\app\build.ps1 -Abi arm64-v8a
```

默认在被Git忽略的 `.local/` 下生成开发签名密钥、临时构建文件和APK。`-Abi all` 同时包含x86_64模拟器运行库；`-Abi arm64-v8a`生成常见安卓手机使用的ARM64版本。

也可显式传入路径：

```powershell
powershell -File .\app\build.ps1 -Abi arm64-v8a -SdkRoot 'C:\Android\Sdk' -OutputApk "$PWD\.local\待课.apk"
```

开发签名密码为Android常用测试值`android`，仅用于本地开发。公开仓库不包含已发布应用的签名私钥，自行构建的开发包不能直接覆盖现有发布包。最低Android 9（API28），目标API35。下载更新和学校登录受各自服务及网络可用性影响。

## 版本与验证

0.9.31将组件课表统一为周课表表格和每日列表，菜单独占组件避免点穿，放大菜单与日期触摸区域。26项原生组件宿主模拟器触摸检查通过，保留0.9.30的更新检测缓存修复；模拟器结果不代表所有实体设备均已验证。

## 许可证

第三方组件按各自许可证分发，详见 [第三方说明](app/THIRD_PARTY_NOTICES.md) 和 [许可证目录](app/licenses/)。本项目自有代码目前未另行指定开源许可证；源码公开不等同于授予任意用途的授权。
