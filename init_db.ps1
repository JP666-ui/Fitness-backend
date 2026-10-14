# 个人健身管理系统 - 数据库初始化脚本 (PowerShell)
# 使用方法: 右键选择 "使用 PowerShell 运行"

$ErrorActionPreference = "Stop"

# MySQL 配置
$MYSQL_HOST = "localhost"
$MYSQL_PORT = "3306"
$MYSQL_USER = "root"
$MYSQL_PASS = "123456"
$DB_NAME = "fitness_db"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  个人健身管理系统 - 数据库初始化脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 MySQL 是否可用
Write-Host "[1/4] 正在检查 MySQL 连接..." -ForegroundColor Yellow
try {
    $result = & mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASS -e "SELECT 1;" 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "连接失败"
    }
    Write-Host "[成功] MySQL 连接正常！" -ForegroundColor Green
} catch {
    Write-Host "[错误] 无法连接到 MySQL 数据库！" -ForegroundColor Red
    Write-Host "请检查：" -ForegroundColor Red
    Write-Host "  - MySQL 服务是否已启动" -ForegroundColor Red
    Write-Host "  - 用户名和密码是否正确" -ForegroundColor Red
    Write-Host "  - MySQL 是否已安装并添加到环境变量" -ForegroundColor Red
    Read-Host "按 Enter 键退出"
    exit 1
}
Write-Host ""

# 创建数据库
Write-Host "[2/4] 正在创建数据库 $DB_NAME..." -ForegroundColor Yellow
try {
    & mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASS -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>$null
    Write-Host "[成功] 数据库创建成功！" -ForegroundColor Green
} catch {
    Write-Host "[警告] 数据库可能已存在，继续执行..." -ForegroundColor Yellow
}
Write-Host ""

# 执行 SQL 脚本
Write-Host "[3/4] 正在执行 SQL 脚本..." -ForegroundColor Yellow
try {
    Get-Content "fitness_db_complete.sql" -Raw | & mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASS $DB_NAME
    if ($LASTEXITCODE -ne 0) {
        throw "SQL 执行失败"
    }
    Write-Host "[成功] SQL 脚本执行完成！" -ForegroundColor Green
} catch {
    Write-Host "[错误] SQL 执行失败！" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Read-Host "按 Enter 键退出"
    exit 1
}
Write-Host ""

# 验证数据
Write-Host "[4/4] 正在验证数据..." -ForegroundColor Yellow
try {
    $verifySql = @"
SELECT '用户表数据' as info, COUNT(*) as count FROM user;
SELECT '饮食记录表数据' as info, COUNT(*) as count FROM food_record;
SELECT '运动记录表数据' as info, COUNT(*) as count FROM exercise_record;
SELECT '健身计划表数据' as info, COUNT(*) as count FROM fitness_plan;
"@
    $result = $verifySql | & mysql -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p$MYSQL_PASS $DB_NAME
    Write-Host $result -ForegroundColor Green
} catch {
    Write-Host "[警告] 数据验证失败，但可能不影响使用" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  数据库初始化完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "数据库信息：" -ForegroundColor White
Write-Host "  - 数据库名：$DB_NAME" -ForegroundColor White
Write-Host "  - 用户名：$MYSQL_USER" -ForegroundColor White
Write-Host "  - 测试账号：test / 123456" -ForegroundColor White
Write-Host "  - 管理员账号：admin / 123456" -ForegroundColor White
Write-Host ""
Write-Host "下一步：启动 SpringBoot 服务" -ForegroundColor Yellow
Write-Host "  mvn spring-boot:run" -ForegroundColor Gray
Write-Host ""
Read-Host "按 Enter 键退出"
