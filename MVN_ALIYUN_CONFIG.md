# Maven 阿里云镜像配置说明

## 📁 配置文件位置

项目已配置阿里云Maven镜像,配置文件位于:
- `.mvn/settings.xml` - 项目级Maven配置
- `.mvn/maven.config` - Maven启动参数

## 🚀 使用方法

### 方法1: 使用项目配置(推荐)

在IDE中配置Maven使用项目级的settings.xml:

**IntelliJ IDEA:**
1. `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Maven`
2. 设置以下路径:
   - **User settings file**: `D:\code\java\meta\.mvn\settings.xml`
   - **Local repository**: 自动使用配置中的路径
3. 点击 `Apply` → `OK`
4. 右键 `pom.xml` → `Maven` → `Reload Project`

### 方法2: 命令行使用

```powershell
# 使用项目配置执行Maven命令
mvn clean install -s .mvn/settings.xml

# 或者设置环境变量
$env:MAVEN_OPTS = "-s .mvn/settings.xml"
mvn clean install
```

### 方法3: 全局配置(可选)

如果想让所有Maven项目都使用阿里云镜像,可以复制到用户目录:

```powershell
# 复制到用户Maven配置目录
Copy-Item .mvn\settings.xml $env:USERPROFILE\.m2\settings.xml
```

## 📦 配置的镜像源

1. **阿里云公共仓库** (主镜像)
   - URL: https://maven.aliyun.com/repository/public
   - 包含: central, jcenter, google等

2. **阿里云中央仓库**
   - URL: https://maven.aliyun.com/repository/central

3. **阿里云Spring仓库**
   - URL: https://maven.aliyun.com/repository/spring

## ⚡ 加速效果

- ✅ 国内访问速度快 10-50 倍
- ✅ 自动同步中央仓库
- ✅ 支持 HTTPS 安全连接
- ✅ 高可用性,稳定可靠

## 🔧 验证配置

执行以下命令验证是否使用阿里云镜像:

```powershell
mvn help:effective-settings -s .mvn/settings.xml
```

查看输出中的 `<mirrors>` 部分,应该看到阿里云的配置。

## 💡 提示

- 首次使用会下载大量依赖,请耐心等待
- 下载速度取决于网络状况,通常比官方仓库快很多
- 如果某些包在阿里云找不到,会自动回退到中央仓库
