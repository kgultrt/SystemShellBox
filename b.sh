#!/bin/bash
set -e

usage() {
    echo "用法：$0 [选项]"
    echo "选项："
    echo "  -d, --debug      构建Debug版本（默认）"
    echo "  -r, --release    构建Release版本"
    echo "  -c, --commit     执行Git提交并且构建"
    exit 1
}

# 默认配置
TASK="app:assembleDebug"
APK_DIR="debug"
APK_PREFIX="app-debug"
NO_COMMIT=1

# 参数解析
while [[ $# -gt 0 ]]; do
    case "$1" in
        -d|--debug)
            shift
            ;;
        -r|--release)
            TASK="app:assembleRelease"
            APK_DIR="release"
            APK_PREFIX="app-release"
            shift
            ;;
        -c|--commit)
            NO_COMMIT=0
            shift
            ;;
        *)
            echo "错误：未知选项 $1"
            usage
            ;;
    esac
done

# 进入项目根目录
cd "$(dirname "$0")" || exit 1

# 获取Git信息
get_git_info() {
    echo "当前Git信息："
    echo "├─ 分支: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'unknown')"
    echo "├─ 提交哈希: $(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"
    echo "├─ 最后提交作者: $(git log -1 --pretty=format:%an 2>/dev/null || echo 'unknown')"
    local commit_time=$(git log -1 --pretty=format:%ct 2>/dev/null)
    if [[ $commit_time ]]; then
        echo "├─ 最后提交时间: $(date -d @${commit_time} '+%Y-%m-%d %H:%M:%S')"
    else
        echo "├─ 最后提交时间: unknown"
    fi
    echo "└─ 构建时间: $(date '+%Y-%m-%d %H:%M:%S')"
}

# 执行Git提交（除非使用 -c 选项）
if [[ $NO_COMMIT -eq 0 ]]; then
    echo "正在处理Git提交..."
    git add -A
    git commit -m "auto commit: $(date '+%Y%m%d_%H%M%S')" || {
        echo "警告：Git提交失败，但继续构建流程"
        sleep 1  # 给用户时间查看警告
    }
else
    echo "跳过Git提交..."
fi

# 显示构建信息
echo ""
echo "════════════════ 构建信息 ════════════════"
get_git_info
echo "══════════════════════════════════════════"
echo ""

# 执行构建任务
echo "正在构建 ${TASK#*:} 版本..."
bash gradlew "$TASK" || { echo "构建失败"; exit 1; }

# 定义APK路径
APK_PATH="app/build/outputs/apk/${APK_DIR}/${APK_PREFIX}.apk"

# 验证APK文件
if [ ! -f "$APK_PATH" ]; then
    echo "错误：找不到APK文件 $APK_PATH"
    exit 1
fi

# 安装到设备
echo "部署到设备..."
cp "$APK_PATH" /sdcard

echo "构建成功!"