# 慧知虚拟电厂管理平台（VPP）

当前版本：1.2.2

[🔥 慧知虚拟电厂平台后端源码](https://github.com/roinli/govpp-cloud)（当前）

[🔥 慧知虚拟电厂管理平台前端源码](https://github.com/roinli/govpp-admin)

[🔥 个人博客](https://wenhui.huizhidata.com)

> 基于 Spring Cloud 微服务架构的虚拟电厂（Virtual Power Plant）管理平台，实现"资源管理 → 事件申报 → 调度分配 → 执行监测 → 效果评估 → 结算分账"全流程闭环。

<p align="center">
    <a href="https://vpp-pc.huizhidata.com">在线体验</a> | <a href="https://vpp-doc.huizhidata.com/">帮助文档</a>
</p>

<p align="center">
    <img src="https://img.shields.io/badge/VPP-v1.2.2-brightgreen" alt="VPP">
    <img src="https://img.shields.io/badge/license-MIT-blue" alt="license">
    <img src="https://img.shields.io/badge/Java-1.8-orange" alt="Java">
    <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen" alt="SpringBoot">
</p>

⚡️慧知开源虚拟电厂（VPP）平台全套源码⚡️；⚡️VPP 慧知虚拟电厂开源平台⚡️；SpringCloud + MySQL + Redis + Nacos + Vue，多租户、多级分账、削峰填谷、智能调度、执行监测、效果评估、数据大屏，一站式虚拟电厂解决方案。

本平台基于 MIT 协议发布，代码全开源无加密、可免费商用，适合充电运营商、聚合商与需求响应服务商，快速构建充电桩需求响应与虚拟电厂运营平台。

---

## 📖 项目介绍

系统采用 Java + Spring Cloud 微服务架构，后端基于 RuoYi-Cloud 多租户版（witos-platform）二次开发，前后端分离设计清晰。围绕「资源管理 → 事件申报 → 调度分配 → 执行监测 → 效果评估 → 结算分账」实现充电桩需求响应全流程闭环：平台下发削峰/填谷事件，场站申报参与，按可调容量智能派单到桩，实时监测目标与实际功率偏差，按响应电量评估合格率并完成平台/运营商/场站分账；原生多租户隔离，可同时服务多个运营商与场站，数据独立、权限隔离。

![首页驾驶舱](./doc/readme/vpp-dashboard.png)
![事件工作台](./doc/readme/vpp-event.png)
![调度分配](./doc/readme/vpp-dispatch.png)
![执行监测](./doc/readme/vpp-execute.png)
![结算管理](./doc/readme/vpp-settlement.png)
![运行大屏](./doc/readme/vpp-screen.png)

---

## 系统演示

在线演示：https://vpp-pc.huizhidata.com（账号：`xndc` / 密码：`admin123`）

演示环境权限开放，请勿随意删除数据。本地启动体验见下方「快速开始」，启动后可通过 Swagger 在线查看与调试接口。

---

## 📚 项目资料

- 在线文档：https://vpp-doc.huizhidata.com/  （使用文档 / 接口文档）
- 仓库内文档：`sql/nacos-configs/` 目录包含 Nacos 配置示例等。
- Swagger 接口文档：部署后访问各服务 `/swagger-ui.html` 在线查看。

---

## 核心功能

#### 运营驾驶舱与数据大屏
首页驾驶舱聚合资源、场站、事件、收益等核心指标，统计卡 + 图表 + 快捷入口一屏掌握；运行大屏深蓝科技风三栏布局，30 秒自动刷新、一键全屏，适用于调度中心投屏展示。

#### 需求响应全流程闭环
需求响应事件录入（削峰/填谷）、状态流转（待响应 → 响应中 → 已结束）、场站级申报参与与申报容量管理（申报容量 ≤ 可调容量），完整承载"收事件 → 申报 → 派单 → 控桩 → 监测 → 验收 → 结账"业务闭环。

#### 资源与场站管理
充电站台账、地理位置标注、启停控制；充电桩资源台账、额定功率与可调容量评估、聚合总览，支持场站 - 桩两级资源建模。

#### 智能调度分配
目标调节量按可调容量/申报容量比例一键分配到运营商、场站、桩，支持逐级派单、微调与人工确认，形成平台 → 运营商 → 场站 → 桩的多级调度体系。

#### 执行监测
目标曲线 vs 实际曲线实时对比，偏差超阈值自动告警，直观呈现各场站/桩的执行偏差与响应态势。

#### 效果评估
基线功率计算、响应电量统计、按场站任务目标判定合格（合格线 80%），形成可量化的响应考核结果。

#### 结算分账
响应电量 × 单价核算收益，按平台/运营商/场站比例自动分账（演示默认 8:1:1），生成结算单并支持确认、打款流程。

#### 多租户治理
多租户数据隔离、自定义租户套餐、租户到期禁用；运营商 - 场站多级经营体系，满足多个运营主体 SaaS 化运营。

#### 系统底座与二开支持
用户、部门、岗位、角色、菜单、按钮权限、数据范围、字典参数、通知公告、操作/登录日志全套系统管理；内置代码生成器（前后端一键生成）、文件服务、XXL-JOB 定时任务与服务监控，便于持续二开。

---

## 系统优势

#### 成熟稳定的微服务架构
后端 Spring Boot + Spring Cloud Alibaba，Nacos 注册与配置中心、网关统一鉴权，模块化拆分清晰，可按需水平扩展。

#### 完整的需求响应业务闭环
从事件、申报、分配到执行、评估、结算全流程贯通，申报容量约束、比例分配、偏差告警、合格判定、分账比例等业务规则均已落地，开箱即用。

#### 多级分账模型
平台 / 运营商 / 场站多级收益分成，分成比例可配置，贴合充电运营商与聚合商的实际经营模式。

#### 数据可视化
首页驾驶舱、运行大屏、目标/实际曲线对比，运营态势一屏呈现，演示效果突出。

#### 精细权限控制
基于 Spring Security 实现 RBAC 角色权限控制，可精确到按钮级别，支持租户数据隔离与数据权限范围，保障系统安全。

#### 高效开发支持
内置代码生成器，支持前后端代码一键生成；`vpp-demo` 提供单体运行示例，新业务服务可快速抽离搭建。

---

## 💻 技术特点

### 运行环境及框架

1. 后台服务 Java Spring Boot + Spring Cloud Alibaba + MyBatis-Plus + MySQL + Redis + Nacos
2. 前端 Vue 2 + Element UI + ECharts（`govpp-admin` 独立仓库，前后端分离）
3. 运行环境 Linux 和 Windows 等都支持，只要有 Java 环境和对应的数据库、Redis、Nacos
4. 运行条件 Java 1.8、MySQL 8.0、Redis 5+、Nacos 2.x、Maven 3.6+

### Java 项目框架版本

```
1. Spring Boot 2.7.18
2. Spring Cloud 2021.0.8
3. Spring Cloud Alibaba 2021.0.5.0
4. MyBatis-Plus 3.5.0
5. Nacos 2.1.1
6. XXL-JOB 2.3.0
7. Maven 3.6+
```

### 项目代码包介绍

```
1. vpp-gateway              微服务网关（统一鉴权、验证码、路由转发）
2. vpp-auth                 认证授权中心（登录、令牌签发）
3. vpp-register             注册与配置中心（Nacos 内置，可直接启动）
4. vpp-event-service        需求响应事件服务（资源/场站/事件/申报）
5. vpp-dispatch-service     调度执行服务（分配/执行/评估/结算）
6. vpp-modules/vpp-system   系统管理（租户/用户/角色/菜单/字典/日志）
7. vpp-modules/vpp-file     文件服务
8. vpp-modules/vpp-gen      代码生成
9. vpp-modules/vpp-job      定时任务（XXL-JOB）
10. vpp-api/vpp-api-system  系统模块远程调用接口
11. vpp-common/*            公共组件（核心/数据源/安全/日志/缓存/MyBatis-Plus 扩展等）
12. vpp-visual/vpp-monitor  服务监控（Spring Boot Admin）
13. vpp-demo                单体运行示例（二开模块抽离参考）
```

## 快速开始

环境要求：JDK 1.8+、Maven 3.6+、MySQL 8.0、Redis 5+、Nacos 2.x。

1. 构建打包：执行 `bin/package.bat`（或 `mvn clean install -DskipTests`）构建各模块 Jar。
2. 初始化数据库：依次导入 `sql/vpp_platform.sql`（业务库）、`sql/witos_config.sql`（Nacos 配置库）、`sql/witos_seata.sql`（分布式事务库）。
3. 导入 Nacos 配置：参考 `sql/nacos-configs/` 下的 `*.yml` 示例与各服务 `resources/bootstrap.yml`，在 Nacos 配置中心创建对应 Data ID。
4. 启动服务：先启动 Nacos（`vpp-register` 已内置）、MySQL、Redis；再执行 `bin/run-gateway.bat` → `bin/run-auth.bat` → `bin/run-modules-system.bat`；`vpp-event-service`、`vpp-dispatch-service` 用 `mvn spring-boot:run` 或 `java -jar` 启动，其余模块按需启动。
5. 前端启动：`git clone https://github.com/roinli/govpp-admin && cd govpp-admin && npm install && npm run dev`，浏览器访问 http://localhost:80。
6. 验证：在 Nacos 控制台查看服务注册情况，打开 Swagger 在线文档登录调试。

---

## 功能矩阵

| 🔴 需求响应业务 | 🟠 资源与调度 | 🟡 监测评估 | 🟢 结算收益 | 🔵 平台底座 | 🟣 系统设置 |
|---|---|---|---|---|---|
| 事件管理（削峰/填谷） | 场站管理 | 执行监测 | 结算分账 | 网关路由与鉴权 | 租户管理 |
| 事件申报 | 资源台账 | 偏差告警 | 平台/运营商/场站分成 | 认证授权中心 | 租户套餐 |
| 状态流转 | 可调容量评估 | 效果评估 | 结算单生成 | Redis 缓存 | 用户管理 |
| 聚合总览 | 智能调度分配 | 基线计算 | 收益核算 | XXL-JOB 定时任务 | 角色权限 |
| 运营驾驶舱 | 运营商逐级派单 | 合格判定 | 响应电量统计 | 代码生成器 | 菜单管理 |
| 数据大屏 | 人工确认 | | | 文件服务 | 部门/岗位 |
| | | | | 服务监控 | 数据字典 |
| | | | | | 通知公告 |
| | | | | | 操作/登录日志 |

## 相关文档

- [Nacos 配置示例](./sql/nacos-configs/)

## 致谢

本项目基于 [RuoYi-Cloud](https://gitee.com/y_project/RuoYi-Cloud) 与 [witos-platform](https://gitee.com/witos/witos-platform) 二次开发，感谢若依开源社区与 witos 开源作者的贡献。

## 反馈与交流

- 项目主页：https://github.com/roinli/govpp-cloud
- 欢迎通过 GitHub Issues 提交 Bug、交流方案、获取更新动态。

---

© 2026 慧知 版权所有 · 开源协议：MIT License · 详见 [LICENSE](./LICENSE)