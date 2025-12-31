#!/bin/bash

# 预览模式：显示将要修改的内容
echo "=== 预览将要修改的文件 ==="
find app/ -type f \( -name "*.sh" -o -name "*.bash" -o -name "*.c" -o -name "*.cpp" -o -name "*.h" -o -name "*.hpp" -o -name "*.py" -o -name "*.java" -o -name "*.js" -o -name "*.ts" -o -name "*.php" \) | while read file; do
    if grep -q "Copyright (C) 2025 - 2026 kgultrt" "$file"; then
        echo "找到: $file"
    fi
done

echo ""
read -p "确定要更新这些文件吗？ (y/n): " confirm

if [[ $confirm == "y" || $confirm == "Y" ]]; then
    # 执行实际修改
    find . -type f \( -name "*.sh" -o -name "*.bash" -o -name "*.c" -o -name "*.cpp" -o -name "*.h" -o -name "*.hpp" -o -name "*.py" -o -name "*.java" -o -name "*.js" -o -name "*.ts" -o -name "*.php" \) | while read file; do
        if grep -q "Copyright (C) 2025 - 2026 kgultrt" "$file"; then
            echo "更新: $file"
            sed -i 's/Copyright (C) 2025 - 2026 kgultrt/Copyright (C) 2025-2026 kgultrt/g' "$file"
        fi
    done
    echo "更新完成！"
else
    echo "取消更新。"
fi