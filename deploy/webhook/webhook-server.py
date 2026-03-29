#!/usr/bin/env python3
"""
GitHub Webhook 接收服务

使用 Python3 标准库实现，零外部依赖。
监听 GitHub push 事件，验证 HMAC-SHA256 签名后触发部署。

环境变量:
  WEBHOOK_SECRET  - GitHub Webhook Secret（必填）
  WEBHOOK_PORT    - 监听端口（默认 9000）
  PROJECT_DIR     - 项目目录（默认自动检测）
  DEPLOY_SCRIPT   - 部署脚本路径（默认同目录下 webhook-deploy.sh）
  LOG_FILE        - 日志文件路径（可选）
"""

import hashlib
import hmac
import json
import os
import shlex
import subprocess
import sys
import threading
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler

# 配置
WEBHOOK_SECRET = os.environ.get("WEBHOOK_SECRET", "")
WEBHOOK_PORT = int(os.environ.get("WEBHOOK_PORT", "9000"))
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.environ.get("PROJECT_DIR", os.path.abspath(os.path.join(SCRIPT_DIR, "..", "..")))
DEPLOY_SCRIPT = os.environ.get("DEPLOY_SCRIPT", os.path.join(SCRIPT_DIR, "webhook-deploy.sh"))
LOG_FILE = os.environ.get("LOG_FILE", "")
DEPLOY_LOCK = os.path.join(os.path.dirname(DEPLOY_SCRIPT), ".deploy.lock") if os.path.exists(DEPLOY_SCRIPT) else ""


def log_message(level, message):
    """记录日志到标准输出和可选的日志文件"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    line = f"[{timestamp}] [{level}] {message}"
    print(line, flush=True)
    if LOG_FILE:
        try:
            with open(LOG_FILE, "a") as f:
                f.write(line + "\n")
        except OSError:
            pass


def verify_signature(secret, payload, signature_header):
    """验证 GitHub Webhook HMAC-SHA256 签名"""
    if not secret:
        log_message("ERROR", "WEBHOOK_SECRET 未配置")
        return False

    if not signature_header:
        log_message("ERROR", "缺少 X-Hub-Signature-256 头")
        return False

    if not signature_header.startswith("sha256="):
        log_message("ERROR", f"签名格式错误: {signature_header[:20]}...")
        return False

    expected = "sha256=" + hmac.new(
        secret.encode("utf-8"),
        payload,
        hashlib.sha256,
    ).hexdigest()

    if not hmac.compare_digest(expected, signature_header):
        log_message("ERROR", "签名验证失败")
        return False

    return True


def run_deploy(payload_body):
    """在子线程中执行部署脚本"""
    try:
        data = json.loads(payload_body)
    except json.JSONDecodeError:
        log_message("ERROR", "无法解析请求体 JSON")
        return

    # 提取事件信息
    ref = data.get("ref", "")
    before = data.get("before", "")[:8]
    after = data.get("after", "")[:8]
    pusher = data.get("pusher", {}).get("name", "unknown")
    repository = data.get("repository", {}).get("full_name", "unknown")

    # 只处理 main 分支的 push
    if ref != "refs/heads/main":
        log_message("INFO", f"忽略非 main 分支推送: {ref}")
        return

    log_message("INFO", f"收到部署请求: {repository} {before}..{after} (by {pusher})")

    if not os.path.isfile(DEPLOY_SCRIPT):
        log_message("ERROR", f"部署脚本不存在: {DEPLOY_SCRIPT}")
        return

    # 获取部署锁，防止并发
    flock_cmd = ["flock", "-n", DEPLOY_LOCK] if DEPLOY_LOCK else []

    cmd = flock_cmd + [DEPLOY_SCRIPT, PROJECT_DIR, after, pusher]

    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=600,
        )

        if result.returncode == 0:
            log_message("INFO", "部署成功")
            if result.stdout.strip():
                for line in result.stdout.strip().split("\n"):
                    log_message("INFO", f"  {line}")
        else:
            log_message("ERROR", f"部署失败 (exit code: {result.returncode})")
            if result.stderr.strip():
                for line in result.stderr.strip().split("\n"):
                    log_message("ERROR", f"  {line}")
            if result.stdout.strip():
                for line in result.stdout.strip().split("\n"):
                    log_message("INFO", f"  {line}")
    except subprocess.TimeoutExpired:
        log_message("ERROR", "部署超时（600 秒）")
    except Exception as e:
        log_message("ERROR", f"部署执行异常: {e}")


class WebhookHandler(BaseHTTPRequestHandler):
    """GitHub Webhook 请求处理器"""

    def do_POST(self):
        # 读取请求体
        content_length = int(self.headers.get("Content-Length", 0))
        if content_length <= 0 or content_length > 10 * 1024 * 1024:
            self.send_response(400)
            self.end_headers()
            return

        payload = self.rfile.read(content_length)

        # 验证事件类型
        event = self.headers.get("X-GitHub-Event", "")
        if event != "push":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ignored", "event": event}).encode())
            return

        # 验证签名
        signature = self.headers.get("X-Hub-Signature-256", "")
        if not verify_signature(WEBHOOK_SECRET, payload, signature):
            self.send_response(403)
            self.end_headers()
            return

        # 异步执行部署
        thread = threading.Thread(target=run_deploy, args=(payload.decode("utf-8"),), daemon=True)
        thread.start()

        # 立即返回响应
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "accepted"}).encode())

    def do_GET(self):
        """健康检查端点"""
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "running", "project": PROJECT_DIR}).encode())

    def log_message(self, format, *args):
        """覆盖默认的 HTTP 日志"""
        log_message("HTTP", format % args)


def main():
    if not WEBHOOK_SECRET:
        print("错误: 请设置 WEBHOOK_SECRET 环境变量", file=sys.stderr)
        sys.exit(1)

    log_message("INFO", f"Webhook 服务启动中...")
    log_message("INFO", f"监听端口: {WEBHOOK_PORT}")
    log_message("INFO", f"项目目录: {PROJECT_DIR}")
    log_message("INFO", f"部署脚本: {DEPLOY_SCRIPT}")

    server = HTTPServer(("0.0.0.0", WEBHOOK_PORT), WebhookHandler)
    log_message("INFO", f"Webhook 服务已启动，等待 GitHub 事件...")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        log_message("INFO", "Webhook 服务已停止")
        server.server_close()


if __name__ == "__main__":
    main()
