# B-2 模型源

alpha.20，单位为 Minecraft 格，约等于米；负 Z 朝机头，X 沿翼展，Y 从地面向上。

- 标称尺寸：翼展 52.12、机长 20.9、机高约 5.1。
- 公共剖面与外形：`src/main/java/dev/overprotocol/vehicle/B2Geometry.java`。
- 完整网格及颜色：`src/main/java/dev/overprotocol/client/B2Mesh.java`。
- 开发客户端启动导出 `b2-spirit.obj` 和同目录 `b2-spirit.mtl`，应一起导入建模软件。机体、航灯及各支柱、轮组、舱门单独分组；起落架在此导出文件中展开。
- 游戏网格共 36037 个面，其中机体 30637、起落架与门板 5382、航灯 18。OBJ 保留每面顶点与逐顶点法线，不做顶点焊接。
- 实测尺寸：X 翼展 52.119998、Y 高度 5.095912、Z 机长 20.9。轮胎最低点为 0。测量输出见 `b2-model-check.json`。
- `tools/dev.ps1 exportB2Model` 可直接调用实际网格导出，不启动游戏；`python tools/preview-b2.py` 用 numpy/Pillow 渲染四视图。预览采用中性背景和几何检查光照，不是游戏截图。
- `B2Gear.java` 保存固定铰点及部件运动；机腹裁出真实轮舱，包含向内的舱壁、舱顶与接至舱顶的支柱。主轮向内折叠，轮架保持水平；鼻轮向后折叠，轮胎进入舱内后才关闭门板。展开顺序相反。
- 导出任务另生成 folding/doors/stowed 三种姿态的 OBJ/MTL，检查 41 个姿态、882648 个活动顶点的舱内净空、地面接触和铰点连接；结果见 `b2-gear-check.json`。`python tools/preview-b2.py --gear` 生成 `b2-gear-preview.png`。

此网格由本项目独立构建。alpha.19 主要依据用户本轮提供的四张照片修正轮廓和比例，测量说明见 `references/b2-user-2026-09-29/README.md`。早期布局研究来源为外层参考Mod/B2-FlightGear 的参考包，尺寸为美国空军公开资料；第三方顶点、贴图、动画不进入本模型。完整来源见 `../CREDITS.md`。

游戏模型以 Java 网格为准。OBJ 为检查／编辑交付物，当前没有导入 OBJ 自动替换游戏网格的流程；修改源代码后启动开发客户端会重新导出。
