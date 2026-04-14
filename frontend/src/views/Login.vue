<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-gradient"></div>
      <div class="bg-pattern"></div>
      <div class="bg-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
      </div>
    </div>

    <div class="login-content">
      <div class="login-left">
        <div class="brand-section">
          <div class="logo">
            <span class="logo-icon">
              <svg viewBox="0 0 48 48" fill="none">
                <circle cx="24" cy="24" r="22" stroke="currentColor" stroke-width="3"/>
                <path d="M14 24L21 31L34 18" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </span>
            <div class="logo-text-group">
              <span class="logo-text">优选通</span>
              <span class="logo-sub">保险服务平台</span>
            </div>
          </div>
          <div class="brand-slogan">
            <h2>专业保险服务</h2>
            <p>为您提供安全、便捷、高效的一站式保险解决方案</p>
          </div>
        </div>

        <div class="features">
          <div class="feature-item">
            <span class="feature-icon">🛡️</span>
            <span class="feature-text">安全保障</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">⚡</span>
            <span class="feature-text">快速理赔</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📋</span>
            <span class="feature-text">产品丰富</span>
          </div>
        </div>
      </div>

      <div class="login-right">
        <div class="login-card">
          <div class="login-card-header">
            <h1>账号登录</h1>
            <p>请登录您的账号以继续</p>
          </div>

          <form @submit.prevent="handleLogin" novalidate>
            <div class="form-item">
              <label for="username">
                <span class="label-icon">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
                  </svg>
                </span>
                用户名
              </label>
              <input
                id="username"
                type="text"
                v-model="loginForm.username"
                placeholder="请输入用户名"
                autocomplete="username"
                :aria-invalid="loginError ? 'true' : 'false'"
                :aria-describedby="loginError ? 'login-error' : undefined"
                required
                autofocus
              >
            </div>
            <div class="form-item">
              <label for="password">
                <span class="label-icon">
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd"/>
                  </svg>
                </span>
                密码
              </label>
              <input
                id="password"
                type="password"
                v-model="loginForm.password"
                placeholder="请输入密码"
                autocomplete="current-password"
                :aria-invalid="loginError ? 'true' : 'false'"
                :aria-describedby="loginError ? 'login-error' : undefined"
                required
              >
            </div>
            <div class="form-item" v-if="loginError">
              <div id="login-error" class="error-msg" role="alert" aria-live="polite">
                <span class="error-icon">⚠️</span>
                {{ loginError }}
              </div>
            </div>
            <button type="submit" class="login-btn" :disabled="loginLoading" :aria-busy="loginLoading ? 'true' : 'false'">
              <span v-if="loginLoading" class="btn-spinner"></span>
              <span>{{ loginLoading ? '登录中...' : '立即登录' }}</span>
            </button>
          </form>

          <div class="login-footer">
            <p>请联系管理员在用户管理中创建用户账号</p>
            <p class="footer-tip">如有账号问题，请联系管理员</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loginLoading = ref(false)
const loginError = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  loginError.value = ''
  loginLoading.value = true

  try {
    const response = await axios.post('/api/auth/login', loginForm)
    if (response.data.code === '00000') {
      const data = response.data.data
      sessionStorage.setItem('authToken', data.token || '')
      sessionStorage.setItem('userId', String(data.userId || ''))
      sessionStorage.setItem('userType', data.userType)
      sessionStorage.setItem('username', data.username)
      sessionStorage.setItem('isLoggedIn', 'true')

      ElMessage.success('登录成功')

      setTimeout(() => {
        router.push('/')
      }, 500)
    } else {
      loginError.value = response.data.message || '登录失败'
    }
  } catch (error) {
    const errorData = error.response?.data
    if (errorData?.code === 'A0400' && errorData?.data) {
      loginError.value = errorData.data
    } else {
      loginError.value = errorData?.message || '网络错误，请稍后重试'
    }
  } finally {
    loginLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 项目主题背景 - 红色渐变 */
.login-bg {
  position: fixed;
  inset: 0;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 25%, #fecaca 50%, #fee2e2 75%, #fef2f2 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(230, 0, 18, 0.06) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(0, 59, 114, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, rgba(230, 0, 18, 0.03) 0%, transparent 60%);
}

.bg-shapes {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
}

.shape-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #fca5a5 0%, #f87171 100%);
  top: -200px;
  left: -200px;
  animation: float 20s ease-in-out infinite;
}

.shape-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%);
  bottom: -150px;
  right: -100px;
  animation: float 25s ease-in-out infinite reverse;
}

.shape-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #fecaca 0%, #f87171 100%);
  top: 50%;
  left: 30%;
  animation: float 18s ease-in-out infinite;
  animation-delay: -5s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(20px, 30px) scale(1.02); }
}

/* 内容区域 */
.login-content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 80px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 左侧品牌区域 */
.login-left {
  flex: 1;
  max-width: 500px;
  color: #1f2937;
}

.brand-section {
  margin-bottom: 60px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 40px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  color: #e60012;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 32px rgba(230, 0, 18, 0.15);
}

.logo-icon svg {
  width: 32px;
  height: 32px;
}

.logo-text-group {
  display: flex;
  flex-direction: column;
}

.logo-text {
  font-size: 32px;
  font-weight: 800;
  color: #e60012;
  letter-spacing: -0.5px;
}

.logo-sub {
  font-size: 13px;
  color: #003b72;
  font-weight: 600;
}

.brand-slogan h2 {
  font-size: 42px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 16px 0;
  letter-spacing: -1px;
}

.brand-slogan p {
  font-size: 18px;
  color: #4b5563;
  margin: 0;
  line-height: 1.6;
}

.features {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 10px;
  border: 1px solid rgba(230, 0, 18, 0.1);
  box-shadow: 0 4px 16px rgba(230, 0, 18, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.feature-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(230, 0, 18, 0.12);
}

.feature-icon {
  font-size: 20px;
}

.feature-text {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

/* 右侧登录卡片 */
.login-right {
  flex: 0 0 440px;
}

.login-card {
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px);
  border-radius: 18px;
  padding: 48px 40px;
  box-shadow:
    0 24px 48px rgba(15, 23, 42, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  border: 1px solid rgba(230, 0, 18, 0.08);
}

.login-card-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-card-header h1 {
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.login-card-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.label-icon {
  width: 18px;
  height: 18px;
  color: #e60012;
}

.label-icon svg {
  width: 100%;
  height: 100%;
}

.form-item input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #d8dee9;
  border-radius: 10px;
  font-size: 15px;
  transition: all 0.2s ease;
  box-sizing: border-box;
  background: #ffffff;
  color: #1f2937;
}

.form-item input:hover {
  border-color: #e60012;
}

.form-item input:focus {
  outline: none;
  border-color: #e60012;
  box-shadow: 0 0 0 3px rgba(230, 0, 18, 0.1);
}

.form-item input::placeholder {
  color: #9ca3af;
}

.error-msg {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #dc2626;
  font-size: 13px;
  padding: 12px 16px;
  background: rgba(254, 226, 226, 0.8);
  border-radius: 10px;
  border: 1px solid rgba(254, 202, 202, 0.5);
}

.error-icon {
  font-size: 14px;
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #e60012 0%, #b0000e 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 8px;
  box-shadow: 0 10px 28px rgba(230, 0, 18, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 36px rgba(230, 0, 18, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:disabled {
  background: #fab6b6;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e7ebf2;
}

.login-footer p {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 8px 0;
}

.login-footer .footer-tip {
  font-size: 12px;
  color: #98a2b3;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .login-content {
    flex-direction: column;
    gap: 40px;
    padding: 24px;
  }

  .login-left {
    text-align: center;
    max-width: 100%;
  }

  .logo {
    justify-content: center;
  }

  .features {
    justify-content: center;
  }

  .login-right {
    flex: 0 0 auto;
    width: 100%;
    max-width: 440px;
  }
}

@media (max-width: 640px) {
  .login-content {
    padding: 16px;
  }

  .brand-slogan h2 {
    font-size: 28px;
  }

  .brand-slogan p {
    font-size: 15px;
  }

  .features {
    gap: 12px;
  }

  .feature-item {
    padding: 10px 14px;
  }

  .login-card {
    padding: 32px 24px;
  }

  .login-card-header h1 {
    font-size: 24px;
  }
}

/* 无障碍支持 */
.login-btn:focus-visible,
.form-item input:focus-visible {
  outline: 3px solid rgba(230, 0, 18, 0.18);
  outline-offset: 2px;
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .bg-gradient,
  .shape,
  .btn-spinner {
    animation: none;
  }

  .login-btn:hover,
  .feature-item:hover {
    transform: none;
  }
}
</style>
