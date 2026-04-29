#!/bin/bash

# ====================================================================
# Spring Boot 101 - API 测试脚本
# 用于快速测试订单管理和迁移 API
# ====================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
BASE_URL="http://localhost:8080"
API_PREFIX="${BASE_URL}/api/orders"
VERBOSE=false

# ====================================================================
# 工具函数
# ====================================================================

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# 检查服务是否运行
check_service() {
    print_header "检查服务状态"

    if curl -s "${API_PREFIX}/health" > /dev/null 2>&1; then
        print_success "服务正常运行"
        return 0
    else
        print_error "服务未运行或无法连接"
        echo "请确保应用启动在 http://localhost:8080"
        exit 1
    fi
}

# 美化 JSON 输出
pretty_json() {
    if command -v jq &> /dev/null; then
        jq . 2>/dev/null || cat
    else
        cat
    fi
}

# ====================================================================
# API 测试函数
# ====================================================================

test_create_order() {
    print_header "测试：创建订单"

    local customer_id="CUST001"
    local customer_name="张三"
    local amount="99.99"
    local description="购买商品"

    local url="${API_PREFIX}?customerId=${customer_id}&customerName=${customer_name}&amount=${amount}&description=${description}"

    print_info "请求: POST ${url}"

    local response=$(curl -s -X POST "$url")

    if [ $VERBOSE = true ]; then
        echo "$response" | pretty_json
    fi

    # 提取订单号
    local order_number=$(echo "$response" | jq -r '.data.orderNumber // empty' 2>/dev/null)

    if [ -z "$order_number" ]; then
        print_error "订单创建失败"
        return 1
    fi

    print_success "订单创建成功: $order_number"
    echo "$order_number"
}

test_query_order() {
    local order_number=$1
    print_header "测试：查询订单 ($order_number)"

    local url="${API_PREFIX}/number/${order_number}"
    print_info "请求: GET ${url}"

    local response=$(curl -s "$url")

    if [ $VERBOSE = true ]; then
        echo "$response" | pretty_json
    fi

    local status=$(echo "$response" | jq -r '.data.status // empty' 2>/dev/null)

    if [ -z "$status" ]; then
        print_error "订单查询失败"
        return 1
    fi

    print_success "订单查询成功，当前状态: $status"
}

test_order_state_transitions() {
    local order_number=$1
    print_header "测试：订单状态转换"

    local transitions=("pay" "prepare" "ship" "deliver")
    local descriptions=("支付" "准备" "发货" "签收")

    for i in "${!transitions[@]}"; do
        local action="${transitions[$i]}"
        local description="${descriptions[$i]}"

        print_info "执行: $description (${action})"

        local url="${API_PREFIX}/${order_number}/${action}"
        local response=$(curl -s -X POST "$url")

        if [ $VERBOSE = true ]; then
            echo "$response" | pretty_json
        fi

        local success=$(echo "$response" | jq -r '.success // empty' 2>/dev/null)
        local status=$(echo "$response" | jq -r '.data.status // empty' 2>/dev/null)

        if [ "$success" == "true" ]; then
            print_success "$description成功，新状态: $status"
        else
            print_error "$description失败"
            local message=$(echo "$response" | jq -r '.message // empty' 2>/dev/null)
            echo "错误信息: $message"
        fi

        sleep 1
    done
}

test_order_cancellation() {
    print_header "测试：订单取消"

    local customer_id="CUST002"
    local customer_name="李四"
    local amount="199.99"

    # 创建新订单用于测试取消
    local url="${API_PREFIX}?customerId=${customer_id}&customerName=${customer_name}&amount=${amount}"
    local response=$(curl -s -X POST "$url")
    local order_number=$(echo "$response" | jq -r '.data.orderNumber // empty' 2>/dev/null)

    if [ -z "$order_number" ]; then
        print_error "创建测试订单失败"
        return 1
    fi

    print_info "测试订单: $order_number"

    # 取消订单
    local cancel_url="${API_PREFIX}/${order_number}/cancel?reason=客户要求取消"
    print_info "请求: POST ${cancel_url}"

    local cancel_response=$(curl -s -X POST "$cancel_url")

    if [ $VERBOSE = true ]; then
        echo "$cancel_response" | pretty_json
    fi

    local status=$(echo "$cancel_response" | jq -r '.data.status // empty' 2>/dev/null)

    if [ "$status" == "CANCELLED" ]; then
        print_success "订单取消成功"

        # 测试退款
        print_info "执行退款..."
        local refund_url="${API_PREFIX}/${order_number}/refund"
        local refund_response=$(curl -s -X POST "$refund_url")

        local refund_status=$(echo "$refund_response" | jq -r '.data.status // empty' 2>/dev/null)

        if [ "$refund_status" == "REFUNDED" ]; then
            print_success "订单退款成功"
        else
            print_error "订单退款失败"
        fi
    else
        print_error "订单取消失败"
    fi
}

test_customer_queries() {
    local customer_id="CUST001"
    print_header "测试：客户统计查询"

    # 订单总金额
    print_info "查询: 客户 $customer_id 的订单总金额"
    local total=$(curl -s "${API_PREFIX}/customer/${customer_id}/total-amount" | jq '.data.totalAmount // empty')
    print_success "订单总金额: $total"

    # 已支付金额
    print_info "查询: 客户 $customer_id 的已支付金额"
    local paid=$(curl -s "${API_PREFIX}/customer/${customer_id}/paid-amount" | jq '.data.paidAmount // empty')
    print_success "已支付金额: $paid"

    # 订单数量
    print_info "查询: 客户 $customer_id 的订单数量"
    local count=$(curl -s "${API_PREFIX}/customer/${customer_id}/count" | jq '.data.orderCount // empty')
    print_success "订单数量: $count"
}

test_migration_endpoints() {
    print_header "测试：数据迁移 API"

    # 检查迁移进度
    print_info "查询迁移进度..."
    local progress=$(curl -s "${API_PREFIX}/migration/progress")

    if echo "$progress" | jq . > /dev/null 2>&1; then
        local total=$(echo "$progress" | jq '.data.totalRecords // 0')
        local migrated=$(echo "$progress" | jq '.data.migratedRecords // 0')
        local pending=$(echo "$progress" | jq '.data.pendingRecords // 0')

        print_success "迁移进度: $migrated/$total (待迁移: $pending)"
    else
        print_error "迁移服务未启用或出错"
        print_info "提示: 确保在 application.properties 中设置 datasource.enable-migration=true"
    fi
}

# ====================================================================
# 主程序
# ====================================================================

main() {
    echo ""
    print_header "Spring Boot 101 - API 自动化测试"

    # 解析命令行参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -u|--url)
                BASE_URL="$2"
                API_PREFIX="${BASE_URL}/api/orders"
                shift 2
                ;;
            -h|--help)
                print_help
                exit 0
                ;;
            *)
                print_error "未知参数: $1"
                print_help
                exit 1
                ;;
        esac
    done

    print_info "API 地址: ${API_PREFIX}"
    echo ""

    # 检查服务
    check_service
    echo ""

    # 执行测试
    local order_number=$(test_create_order)
    echo ""

    test_query_order "$order_number"
    echo ""

    test_order_state_transitions "$order_number"
    echo ""

    test_order_cancellation
    echo ""

    test_customer_queries
    echo ""

    test_migration_endpoints
    echo ""

    print_header "测试完成"
    print_success "所有测试执行完毕"
}

print_help() {
    cat << EOF
使用方法: $0 [选项]

选项:
    -v, --verbose       显示详细的 JSON 响应
    -u, --url URL       指定 API 地址（默认: http://localhost:8080）
    -h, --help          显示本帮助信息

示例:
    # 基础测试
    $0

    # 显示详细输出
    $0 --verbose

    # 指定自定义 URL
    $0 --url http://example.com:9090

EOF
}

# 运行主程序
main "$@"
