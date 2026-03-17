# Texas Poker - GGPoker风格德州扑克 Android游戏

## 🃏 功能特性

### 游戏模式
- **现金局 (Cash Game)** - 多种盲注档位，从新手桌到豪客桌
- **锦标赛 (Tournament)** - 含盲注结构的标准锦标赛
- **坐就走 (Sit & Go)** - 2人/6人/9人快速SnG局

### 核心玩法
- 完整德州扑克规则（翻前/翻牌/转牌/河牌/摊牌）
- 全手牌组合判断：高牌到皇家同花顺
- 边底池（All-in）计算
- 计时器倒计时行动
- 庄家/小盲/大盲标识

### AI系统
- 4个难度级别：新手/中级/高级/专家
- 手牌强度评估（蒙特卡洛近似）
- 位置感知策略
- GTO近似诈唬范围

### 大厅 & 社交
- 房间列表（含人数/底池/手数统计）
- 排行榜
- 每日免费筹码
- 胜率/最佳手牌统计

---

## 🚀 打包APK步骤

### 前置要求
1. **JDK 17** - [下载地址](https://adoptium.net/)
2. **Android Studio** (含Android SDK) - [下载地址](https://developer.android.com/studio)
   - SDK版本：Android API 34 (Android 14)
   - Build Tools：34.0.0

### 方法一：使用Android Studio（推荐）
1. 打开Android Studio
2. 选择 `File > Open` → 选择 `TexasPoker` 文件夹
3. 等待Gradle同步完成（需要联网下载依赖）
4. 点击菜单 `Build > Build Bundle(s)/APK(s) > Build APK(s)`
5. 构建完成后点击通知栏的 **"locate"** 链接
6. APK位于：`app/build/outputs/apk/debug/app-debug.apk`

### 方法二：命令行构建
```bash
# Windows
cd TexasPoker
build_apk.bat

# Mac/Linux
cd TexasPoker
chmod +x build_apk.sh
./build_apk.sh
```

### 方法三：Gradle命令
```bash
cd TexasPoker
./gradlew assembleDebug
# APK位于: app/build/outputs/apk/debug/app-debug.apk
```

### 安装到手机
```bash
# 通过ADB安装（需开启USB调试）
adb install app-debug.apk

# 或直接将APK文件传输到手机，打开安装
```

---

## 📁 项目结构

```
TexasPoker/
├── app/src/main/
│   ├── java/com/texaspoker/game/
│   │   ├── engine/
│   │   │   ├── PokerEngine.kt      # 核心游戏引擎
│   │   │   └── HandEvaluator.kt    # 手牌评估器
│   │   ├── model/
│   │   │   ├── Card.kt             # 牌/花色/手牌数据
│   │   │   ├── Player.kt           # 玩家数据
│   │   │   └── GameState.kt        # 游戏状态
│   │   ├── ai/
│   │   │   └── AIDecisionEngine.kt # AI决策引擎
│   │   ├── data/
│   │   │   └── GameRepository.kt   # 数据持久化
│   │   └── ui/
│   │       ├── splash/             # 启动页
│   │       ├── auth/               # 登录/注册
│   │       ├── lobby/              # 大厅（现金局/锦标赛/SnG/排行榜）
│   │       ├── table/              # 游戏桌（核心）
│   │       ├── tournament/         # 锦标赛详情
│   │       └── profile/            # 个人资料/设置
│   └── res/
│       ├── layout/                 # 所有布局文件
│       ├── values/                 # 颜色/字符串/主题
│       └── drawable/               # 图形资源
└── build_apk.bat                   # Windows构建脚本
```

---

## 🔧 自定义配置

### 修改初始筹码
`GameRepository.kt` 中修改：
```kotlin
chips = 100_000L  // 默认10万筹码
```

### 修改AI难度
`GameTableActivity.kt` 中设置默认难度，或在设置页面调整。

### 添加新桌类型
在 `CashGameFragment.kt` 的 `generateRooms()` 中添加新房间配置。

---

## 📌 注意事项
- 本游戏为单机模式，所有对手均为AI
- 游戏数据存储在本地（SharedPreferences）
- 筹码为虚拟货币，无任何真实价值
- 本游戏仅供娱乐学习，不涉及真实赌博
