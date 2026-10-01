# 构建验证 / Build validation

2026-10-01，Windows，Java 21，Minecraft 1.21.1，NeoForge 21.1.197。

```powershell
.\gradlew.bat build runGameTestServer --offline --console=plain
```

- 构建成功；77 项必需 GameTest 全部通过。
- 测试实例未安装 TaCZ；需要真实 TaCZ 的可选检查被跳过。
- 已检查发行 JAR，不包含开发 GameTest 类或测试结构。
- 英文与中文语言文件的键集合一致。

The build succeeded and all 77 required GameTests passed. The test instance did not include TaCZ, so optional checks requiring the real TaCZ mod were skipped. The installable JAR excludes development GameTest classes and the test structure; English and Chinese language keys match.

首次构建需要联网下载依赖；上述离线命令使用已有依赖缓存。自动检查不替代玩家对外观、操控和可选模组兼容性的游戏测试。

The offline command used an existing dependency cache. A first build requires network access. Automated checks do not replace player testing of appearance, handling, or optional-mod compatibility.
