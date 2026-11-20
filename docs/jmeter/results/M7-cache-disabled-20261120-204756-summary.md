# JMeter Benchmark Run

- Scenario: M7-cache-disabled
- Start time: 20261120-204756
- End time: 2026-11-20 20:48:05
- Git branch: develop
- Git commit (HEAD): `8410cd69c90d4740b0b5649917c7497fdfde3b86`
- Git working tree: dirty (uncommitted changes present)
- JMX: `D:\project\untitled\docs\jmeter\template-cache.jmx`
- JMeter exit code: 0
- JTL: `D:\project\untitled\docs\jmeter\results\M7-cache-disabled-20261120-204756.jtl`
- HTML report: `D:\project\untitled\docs\jmeter\reports\M7-cache-disabled-20261120-204756/index.html`
- MySQL Com_select before: 4020
- MySQL Com_select after: 4525
- MySQL Com_select delta: 505

> Com_select 是 MySQL 全局累计计数。压测窗口内的其他连接、定时任务和手动 SQL 都可能影响差值。
> 请结合 JMeter HTML 报告、数据库业务结果和测试环境记录解读，不要把全局差值直接当成接口专属 SQL 数量。
