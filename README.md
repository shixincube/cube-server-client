# Cube - 时信魔方

**Cube** **时信魔方** 是面向开发者的实时协作开发框架。帮助开发者快速、高效的在项目中集成实时协作能力。

支持的操作系统有：Windows、Linux 、macOS 、Android、iOS 等，支持的浏览器有：Chrome、Firefox、Safari 等。

## 功能列表

Cube 包含以下协作功能：

* 即时消息（Instant Messaging / IM）。支持卡片消息、通知消息、文件消息和自定义消息等。
* 实时多人语音/多人视频（Multipoint RTC）。支持自适应码率、超低延迟等，支持实时图像识别等。
* 超大规模(100+)视频会议 （Video Conference）。支持会议控制、演讲模式，自定义 MCU 和 SFU 布局等。
* 群组管理（Group management）。支持集成式管理和扩展组织架构等。
* 共享桌面（Remote Desktop Sharing）。支持无缝集成白板等。
* 云端文件存储（Cloud File Storage）。支持无缝集成文档在线协作等。
* 实时白板（Realtime Whiteboard）。支持集成媒体回放、远程桌面和文档分享等。
* 视频直播（Live video）。支持第三方推流和 CDN ，无缝支持会议直播和回放等。
* 互动课堂（Online Classroom）。支持实时课堂互动和在线习题、考试。
* 电子邮件管理与代收发（Email management）。
* 在线文档协作（Online Document Collaboration）。支持 Word、PowerPoint、Excel 等主流格式文多人在写协作。
* 安全与运维管理（Operation and Maintenance management）。所有数据通道支持加密，可支持国密算法等。
* 风控管理（Risk Management）。对系统内所有文本、图片、视频、文件等内容进行包括 NLP、OCR、IR 等技术手段的风险控制和预警等。


## 功能展示

| 即时消息 |
|:----:|
|![IM](https://static.shixincube.com/cube/assets/showcase/im.gif)|

| 视频聊天(1) | 视频聊天(2) |
|:----:|:----:|
|![VideoChat1](https://static.shixincube.com/cube/assets/showcase/videochat_1.gif)|![VideoChat2](https://static.shixincube.com/cube/assets/showcase/videochat_2.gif)|

| 多人视频聊天(1) | 多人视频聊天(2) |
|:----:|:----:|
|![VideoChat3](https://static.shixincube.com/cube/assets/showcase/videochat_3.gif)|![VideoChat4](https://static.shixincube.com/cube/assets/showcase/videochat_4.gif)|

| 会议 |
|:----:|
|![Conf100](https://static.shixincube.com/cube/assets/showcase/screen_conference.jpg)|
|![ConfTile](https://static.shixincube.com/cube/assets/showcase/screen_conference_tile.jpg)|
|![StartConf](https://static.shixincube.com/cube/assets/showcase/start_conference.gif)|

| 共享桌面 |
|:----:|
|![ScreenSharing](https://static.shixincube.com/cube/assets/showcase/screen_sharing.gif)|

| 云端文件存储 |
|:----:|
|![CFS](https://static.shixincube.com/cube/assets/showcase/cloud_file.gif)|

| 白板 |
|:----:|
|![Whiteboard](https://static.shixincube.com/cube/assets/showcase/whiteboard.gif)|

| 直播 |
|:----:|
|![Live](https://static.shixincube.com/cube/assets/showcase/live.gif)|

| 在线课堂 |
|:----:|
|![OnlineClassroom](https://static.shixincube.com/cube/assets/showcase/online_classroom.gif)|

| 文档协作 |
|:----:|
|![DocCollaboration](https://static.shixincube.com/cube/assets/showcase/doc_collaboration_excel.gif)|
|![DocCollaboration](https://static.shixincube.com/cube/assets/showcase/doc_collaboration.gif)|


## 技术特征

* 网络层、逻辑层、缓存层、存储层采用分层设计。服务器端使用“**单元隔离**”原则，各个功能单元可以实现 **“微服务”方式部署** ，**“宏服务”方式管理** 。
* 采用 **SHM（Simple Hybrid Messaging，简单混合消息传送）** 协议。可在同一条链路上混合传输封包和流数据，且都能进行压缩和加密。
* 信令、负载和流媒体传输均采用加密方式。长连接信令支持全信道压缩（采用 ZIP 算法）。
* 兼容行业内主流协议，例如：**SIP**（RFC 3261）、**WebRTC**（RFC 7742、RFC 7874、RFC 7875）等。
* 支持 H.264，VP8，VP9 等视频编解码器，支持 G711、G722、iLBC、ISAC、OPUS 等音频编解码器。
* 支持 **MCU （Multipoint Conferencing Unit）** 与 **SFU （Selective Forwarding Unit）** 模式。
* 采用矢量图元传输与渲染。
* 支持文本内容、文件内容、图片内容、图像内容的识别，内置 NLP、Face Recognition、Super Resolution 等数据处理技术，**提供对接第三方机器学习平台接口**。
* 支持的文档格式有：**pdf**、**doc**、docm、**docx**、dotm、dotx、ett、**xls**、xlsm、**xlsx**、xlt、dpt、ppsm、ppsx、pot、potm、potx、pps、**ppt**、pptm、**pptx** 等。
* 采用针对实时协作场景的集群控制策略和时序存储，兼容 Kafka、RabbitMQ、MongoDB 等第三方软件。
* 支持**插件式开发**，客户端与服务器端均支持插件热部署。
* 支持按域管理与数据隔离，支持服务内混合域。
* 核心协议自研，可用于对“自主可控”有严格要求的项目。


## 项目目标

“**Cube**” 是 **Cooperative** ，**Ultrafast** ，**Best-practice** 和 **Efficient** 的首字母缩写，意在为开发者和用户提供专业、可靠的协作产品和技术支持，帮助客户快速部署和构建在线协作场景。

* <span style="font-size:27px;display:inline;"><b>C</b></span> - <b>Cooperative</b> 时信魔方是为在线协同提供快速能力整合的框架。
* <span style="font-size:27px;display:inline;"><b>U</b></span> - <b>Ultrafast</b> 时信魔方为客户打造极速解决方案，极速开发、极速运行和极速服务响应。
* <span style="font-size:27px;display:inline;"><b>B</b></span> - <b>Best-practice</b> 时信魔方提供实时在线协作的最佳实践方式，优秀的用户体验。
* <span style="font-size:27px;display:inline;"><b>E</b></span> - <b>Efficient</b> 时信魔方积累多年行业场景方案，直击痛点，有效、高效。

时信魔方以开发者为核心，以技术驱动开发，以创新驱动开源，回馈开源社区，提供优质的协作开源产品，时信魔方的所有功能都经过商业项目的验证。

## 视觉设计

Cube 的官方 Logo 如下表所示：

| 规格 | 默认效果 | 单色效果 | 反色效果 |
| ---- | ---- | ---- | ---- |
| 256x256 | ![Cube](https://static.shixincube.com/cube/assets/images/logo/cube_256.png) | ![Cube](https://static.shixincube.com/cube/assets/images/logo/cube_mono_256.png) | ![Cube](https://static.shixincube.com/cube/assets/images/logo/cube_inverse_256.png)
| 512x512 | ![Cube](https://static.shixincube.com/cube/assets/images/logo/cube_512.png) | ![Cube](https://static.shixincube.com/cube/assets/images/logo/cube_mono_512.png) | ![Cube](https://static.shixincube.com/cube/assets/images/logo/cube_inverse_512.png)

****

Cube 应用程序图标如下表所示：

| 分类 | 图标 32x32 | 图标 48x48 | 图标 256x256 | 图标 1024x1024 |
| ---- | ---- | ---- | ---- | ---- |
| 安装包 | / | ![Cube](https://static.shixincube.com/cube/assets/images/icon/cube_install_48.png) | ![Cube](https://static.shixincube.com/cube/assets/images/icon/cube_install_256.png) | ![Cube](https://static.shixincube.com/cube/assets/images/icon/cube_install_1024.png) |
| 卸载包 | ![Cube](https://static.shixincube.com/cube/assets/images/icon/cube_uninstall_32.png) | / | / | ![Cube](https://static.shixincube.com/cube/assets/images/icon/cube_uninstall_1024.png) |
| 托盘 | ![Cube](https://static.shixincube.com/cube/assets/images/icon/cube_tray_32.png) ![Cube](https://static.shixincube.com/cube/assets/images/icon/cube_tray_active_32.png) | / | / | / |


## 获得帮助

您可以访问 [时信魔方官网](https://www.shixincube.com/) 获得更多信息。如果您在使用 Cube 的过程中需要帮助可以发送邮件到 [cube@spap.com](mailto:cube@spap.com) 。
