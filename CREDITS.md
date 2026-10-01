# 来源与原创说明

- Gradle Wrapper 来自本地 NeoForgeMDKs/MDK-1.21.1-ModDevGradle，保留 TEMPLATE_LICENSE.txt，发行版固定为 8.10.2。
- API 用法参考 NeoForge 官方 1.21.1 文档与本地 Minecraft/NeoForge 源码。
- 已阅读 C:\MCModBench\神圣的北极星标准\必须提前阅读这个md文件.md；未复制该模组代码、贴图、模型或音效。
- 礼宾地毯金色纹样由本项目独立设计，以 art/carpet-pattern.json 保存；其 16 色版本只替换布料颜色。配色见 art/dye-palette.json。
- 四向桌子和烛台几何、连接与交互逻辑由本项目编写；布面、布裙、木纹、金色材质由脚本生成。
- 烛台上的蜡烛引用 Minecraft 原版蜡烛纹理；纹理坐标、烛芯比例参考 Minecraft 1.21.1 template_candle 模型，未打包原版纹理副本。
- 外观参考：[2015 年中方访美国宴官方介绍](https://obamawhitehouse.archives.gov/blog/2015/09/24/follow-along-official-china-state-visit)、[官方国宴资料](https://obamawhitehouse.archives.gov/sites/default/files/image/wh_state_dinner_china_2015.pdf)、[国宴照片与报道](https://www.chinausfocus.com/news/white-house-hosts-state-dinner-for-president-xi)。照片用于观察正式餐桌陈设和金色烛台，未作为游戏纹理使用，未随发行 JAR 打包。整面桌布及红色外围布裙遵从用户设计要求。
- 仪仗队雕像改用原版玩家模型渲染，皮肤档案的解析与同步参照原版 `SkullBlockEntity` 的公开做法
  （`ResolvableProfile` + `resolve()` + `getUpdateTag`/`getUpdatePacket`，客户端走 `SkinManager.getInsecureSkin`），未复制其代码。
- 内置仪仗队礼服皮肤与物品图标由 `tools/generate-guard-skin.ps1` 逐像素生成，无第三方素材。
  雕像不再自带枪械或旗帜模型，手中的物品一律使用物品本身（含原版旗帜）的模型。
  玩家皮肤由客户端在运行时从 Mojang 会话服务取得，不随本模组打包。
- 双格方块的掉落与同步遵循原版 `DoublePlantBlock` 的做法（掉落表挂下半格、`playerWillDestroy` 显式掉落、`playerDestroy` 置空），未复制其代码。
- 开发客户端另外安装了 TaCZ（Timeless & Classics Guns: Zero，1.21.1 NeoForge 移植版，Modrinth）与 JEI，二者均为独立第三方模组，
  不打包进本模组的发行 JAR，也不属于本项目成果。
- 未引入第三方模组玩法实现或美术素材。项目沿用仓库所有者选定的 MIT，见根目录 LICENSE。


## 2026 年 9 月访美参考与 TaCZ API

用户明确了日期：目标是 2026 年 9 月中方访美。早期 2015 年材料记录为历史调研，不再作为当前场景依据。最新官方照片与来源见 art/references/2026-09-state-visit/README.md；图片不进入游戏资源或发行 JAR。

TaCZ 握枪 API 参考本地 C:\MCModBench\参考Mod\TACZ-1.21.1 和实际安装的 1.1.8-hotfix-r6 JAR，核对 ThirdPersonManager、IThirdPersonAnimation、TimelessAPI 及 GunDisplayInstance。只调用 API，枪模型、纹理和配件仍由独立 TaCZ 模组在运行时提供；没有复制或重新分发其素材。源码项目：[TaCZ NeoForge 1.21.1 移植](https://github.com/MUKSC/TACZ-1.21.1)。

## B-2 载具研究（2026-09-29）

- 在线查阅 [Immersive Aircraft](https://github.com/Luke100000/ImmersiveAircraft/tree/1.21.1)，并阅读本地参考库中的 VehicleEntity、AircraftEntity、AirplaneEntity：研究飞行与地面运动的区分、发动机目标、第三人称旋转、乘客位置和实体插值。该项目为 GPL-3.0；本模组独立实现简化游戏物理与服务端键输入同步，没有复制实现、模型或纹理，也没有增加该模组为运行依赖。
- 查阅 [Immersive Vehicles / Minecraft Transport Simulator](https://github.com/DonBruce64/MinecraftTransportSimulator) 的项目介绍，了解复杂载具与模型包分离的方向；本首版不采用其包系统或代码。
- 外形比例依据 [美国空军 B-2 Spirit 公共资料](https://www.af.mil/About-Us/Fact-Sheets/Display/Article/104482/b-2-spirit/) 和 [美国空军国家博物馆 B-2 介绍](https://www.nationalmuseum.af.mil/Visit/Museum-Exhibits/Fact-Sheets/Display/Article/195832/USAFmuseum/northrop-b-2-spirit/)。alpha.18 按翼展 52.12 米、长 20.9 米、高 5.1 米，以 1 格约 1 米独立建模；数值物理不模拟真实飞机。
- 根据用户授权，下载 [FlightGear B-2 参考包](https://mirrors.ibiblio.org/flightgear/ftp/Aircraft-2024/B-2.zip)，原作者为包内署名的 Markus Zojer，研究 AC3D 分组、机身曲面与起落架部件布局。参考包保存在工程外层的参考Mod/B2-FlightGear，不打包进游戏资源或发行 JAR；没有导入其顶点、纹理或动画。
- alpha.18 另研究 [Automobility](https://github.com/FoundationGames/Automobility) 的加速视野，以及本地 Minecraft/NeoForge Camera 的角度、第三人称距离与障碍裁切事件。镜头由本项目使用公开事件独立实现；蓝冰船速度从本版本原版 Boat 与 Blocks 源码计算，未复制载具 Mod 的实现。
- B2Mesh.java 为本项目原创曲面网格，B2Geometry.java 保存独立剖面；物品图标由 tools/generate-b2-art.ps1 根据轮廓生成，单色中性纹理配合顶点颜色完成机体。开发导出的 OBJ/MTL 与游戏网格相同，不包含第三方模型。

alpha.19 的主要视觉依据是用户本轮提供的四张公开 B-2 照片，另以用户的 alpha.18 截图定位比例错误。原文件仅保存于 art/references/b2-user-2026-09-29，未修改成纹理或放入发行 JAR；摄影者未从用户附件确认。俯视图用于记录轮廓折点，斜视与侧面用于观察座舱、发动机和机翼的连续过渡。另检索核对了 [Northrop Grumman B-2 官方图库](https://www.northropgrumman.com/what-we-do/aircraft/b-2-stealth-bomber/media-gallery) 的公开图片来源入口，没有将其图片加入模型。

## B-2 鼠标驾驶研究（2026-09-30）

阅读 [Do a Barrel Roll / 滚筒飞行](https://github.com/enjarai/do-a-barrel-roll) 的 README、MouseMixin 和 RotationModifiers，研究鼠标输入、镜头平滑、倾斜反馈和回正。该项目已迁移到 [Codeberg](https://codeberg.org/enjarai/do-a-barrel-roll)；研究用源码与原 GPL-3.0 许可证保存在工程外层参考Mod/DoABarrelRoll，提交和研究范围见旁边的 DoABarrelRoll-NOTES.md。

alpha.20 根据用户选择，独立编写鼠标转向/俯仰、键盘油门、回中输入、有限角度的倾斜转弯和跟随镜头阻尼；没有复制参考项目代码或资产。仍使用本项目的服务端游戏飞行计算，并非移植鞘翅物理。鼠标采样时机、灵敏度和相机事件另核对本地 Minecraft 1.21.1 / NeoForge 21.1.197 源码。

## alpha.21：速度参考与重绘材质（2026-09-30）

[美国空军学院 B-2A 资料](https://www.usafa.af.mil/News/Features/Display/Article/797445/the-contrails-aircraft-weapons-systems-b-2a-spirit/) 公开速度范围为 Mach 0.85–0.95；制造商与空军主资料将最高速度描述为高亚音速。由于马赫数随气温/高度换算，游戏采用高空约 1010 km/h（约 280.56 米/秒）的公开范围近似值，以一格一米、20 tick/秒换算，未声明这是无条件的精确实机指标。地面和放轮速度限制为游戏操控设定。

五种新材质由内置 image_gen 生成：中性细织桌布、垂直布褶、胡桃木、光滑金色与纯红绒面。源图已放入 art/material-sources，完整提示词和最终路径见 art/materials-alpha21.json；只将 64×64 游戏材质打包。颜色变化通过模型与物品染色实现，保留已有桌布配色。新红毯卷网格、层纹和消耗/铺设逻辑由本项目独立编写。
