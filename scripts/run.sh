#!/usr/bin/env bash
# jink 交互式 Demo 启动器
# 用法: ./run.sh [JDK_HOME]
#   ./run.sh                        # 使用系统 java（须 >= 8）
#   ./run.sh /path/to/jdk8          # 指定 JDK 路径
set -euo pipefail

get_java_major() {
    local java_bin="$1"
    local ver
    ver=$("$java_bin" -version 2>&1 | head -1) || { echo 0; return; }
    if [[ "$ver" =~ \"1\.([0-9]+) ]]; then
        echo "${BASH_REMATCH[1]}"   # Java 8: "1.8.xxx"
    elif [[ "$ver" =~ ([0-9]+)\.[0-9]+ ]]; then
        echo "${BASH_REMATCH[1]}"   # Java 11+: 11.0.x or "11.0.x"
    elif [[ "$ver" =~ ([0-9]+) ]]; then
        echo "${BASH_REMATCH[1]}"
    else
        echo 0
    fi
}

JDK_HOME="${1:-}"
JAVA_BIN="java"

# 1. 确定 Java 可执行路径
if [[ -n "$JDK_HOME" ]]; then
    JAVA_BIN="$JDK_HOME/bin/java"
    export JAVA_HOME="$JDK_HOME"
    export PATH="$JDK_HOME/bin:$PATH"
    echo "[jink] 使用指定 JDK: $JDK_HOME"
else
    major=$(get_java_major "java" 2>/dev/null || echo 0)
    if [[ "$major" -ge 8 ]]; then
        echo "[jink] 使用系统 Java $major"
    else
        if [[ "$major" -eq 0 ]]; then
            echo "❌ 未找到 Java，请安装 JDK 8+ 或通过参数指定路径："
        else
            echo "❌ 当前 Java 版本 $major < 8，请通过参数指定 JDK 8+ 路径："
        fi
        echo "   ./run.sh /path/to/jdk8"
        exit 1
    fi
fi

# 切换到项目根目录
cd "$(dirname "$0")/.."
export MAVEN_OPTS="-Dfile.encoding=UTF-8"

# 2. 编译
echo "[jink] 编译项目..."
mvn -q compile test-compile -DskipTests

# 3. 构建依赖 classpath
mvn -q "-Dmdep.outputFile=target/cp.txt" dependency:build-classpath
DEPS=$(tr -d '[:space:]' < target/cp.txt)
SEP=":"
if [[ -n "$DEPS" ]]; then
    CP="target/classes:target/test-classes:$DEPS"
else
    CP="target/classes:target/test-classes"
fi

# 4. 动态扫描 demo 类
DEMO_DIR="target/test-classes/io/mybatis/jink/demo"
if [[ ! -d "$DEMO_DIR" ]]; then
    echo "❌ 未找到 demo 类（先编译项目）"
    exit 1
fi

mapfile -t CLASSES < <(
    find "$DEMO_DIR" -maxdepth 1 -name "*.class" ! -name '*$*' \
        -exec basename {} .class \; | sort | \
    while read -r name; do echo "io.mybatis.jink.demo.$name"; done
)

if [[ ${#CLASSES[@]} -eq 0 ]]; then
    echo "❌ demo 目录为空"
    exit 1
fi

# 5. 显示交互菜单
echo ""
echo "╔══════════════════════════════╗"
echo "║     jink Demo 列表           ║"
echo "╚══════════════════════════════╝"
for i in "${!CLASSES[@]}"; do
    name="${CLASSES[$i]##*.}"
    printf "  %2d. %s\n" $((i + 1)) "$name"
done
echo ""
read -rp "请输入序号 (1-${#CLASSES[@]}): " choice
idx=$((choice - 1))
if [[ $idx -lt 0 || $idx -ge ${#CLASSES[@]} ]]; then
    echo "❌ 无效选择"
    exit 1
fi
MAIN_CLASS="${CLASSES[$idx]}"

# 6. 启动
echo ""
echo "[jink] 启动 $MAIN_CLASS ..."
echo ""
exec "$JAVA_BIN" \
    -Dfile.encoding=UTF-8 \
    -cp "$CP" \
    "$MAIN_CLASS"
