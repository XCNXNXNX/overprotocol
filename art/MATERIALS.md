# alpha.21 材质

2026-09-30，使用内置 `image_gen` 重绘；最终提示词、生成路径、项目源图和游戏路径全部记录在 [materials-alpha21.json](materials-alpha21.json)。原图已复制到本项目的 `material-sources`，不依赖 Codex 默认生成目录。

| 用途 | 游戏材质 |
| --- | --- |
| 16 色桌布表面 | `src/main/resources/assets/overprotocol/textures/block/table_linen.png` |
| 16 色桌布外围布裙 | `src/main/resources/assets/overprotocol/textures/block/table_pleats.png` |
| 桌架 | `src/main/resources/assets/overprotocol/textures/block/table_wood.png` |
| 烛台和栏杆金属 | `src/main/resources/assets/overprotocol/textures/block/table_gold.png` |
| 30 米红毯卷及红绒地毯 | `src/main/resources/assets/overprotocol/textures/block/red_velvet.png` |

游戏材质为 64×64；原图经标准尺寸缩小，未裁切改画。桌布和布裙以中性原图配合模型染色保留既有 16 色；木头和金属不染色。16 色金花地毯保留原纹样。

重建：`python tools/prepare-materials.py`，需要 Pillow；`tools/generate-table-art.ps1` 调用同一流程。原来的金属棋盘纹理生成已停用。素材预览为 `materials-alpha21-preview.png`，它是贴图样本，不是游戏场景。
