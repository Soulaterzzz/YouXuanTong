<template>
  <div class="login-page">
    <header class="login-header">
      <div class="logo">
        <span class="logo-text">优选通</span>
        <span class="logo-sub">保险服务平台</span>
      </div>
    </header>
    
    <div class="login-container">
      <div class="login-panel-header">
        <h1>账号登录</h1>
        <p>请联系管理员在用户管理中创建用户账号</p>
      </div>

      <form @submit.prevent="handleLogin" novalidate>
        <div class="form-item">
          <label for="username">用户名</label>
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
          <label for="password">密码</label>
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
          <div id="login-error" class="error-msg" role="alert" aria-live="polite">{{ loginError }}</div>
        </div>
        <button type="submit" class="login-btn" :disabled="loginLoading" :aria-busy="loginLoading ? 'true' : 'false'">
          {{ loginLoading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div class="login-tip">
        如有账号问题，请联系管理员。
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
  background:
    radial-gradient(circle at top left, rgba(230, 0, 18, 0.08), transparent 32%),
    linear-gradient(180deg, #f7f8fb 0%, #eef2f7 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-header {
  width: 100%;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(230, 0, 18, 0.18);
  padding: 15px 24px;
  text-align: center;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
}

.logo {
  display: inline-flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-text {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-primary);
}

.logo-sub {
  font-size: 11px;
  color: #999;
}

.login-container {
  width: 420px;
  margin-top: 60px;
  padding: 30px 40px 40px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
}

.login-panel-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-panel-header h1 {
  margin: 0 0 10px;
  font-size: 26px;
  color: var(--color-text-primary);
}

.login-panel-header p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  font-size: 14px;
  color: var(--color-text-primary);
  margin-bottom: 8px;
  font-weight: 500;
}

.form-item input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  box-sizing: border-box;
  background: #fff;
}

.form-item input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(230, 0, 18, 0.14);
}

.form-item input::placeholder {
  color: #c0c4cc;
}

.form-item .error-msg {
  color: var(--color-danger);
  font-size: 12px;
  margin-top: 6px;
  padding: 8px 12px;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 10px;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff4d5a 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
  margin-top: 10px;
  box-shadow: 0 10px 24px rgba(230, 0, 18, 0.22);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(230, 0, 18, 0.26);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:disabled {
  background: #fab6b6;
  cursor: not-allowed;
  box-shadow: none;
}

.login-btn:focus-visible,
.form-item input:focus-visible {
  outline: 3px solid rgba(230, 0, 18, 0.18);
  outline-offset: 2px;
}

@media (pointer: coarse) {
  .login-btn {
    min-height: 44px;
  }

  .form-item input {
    min-height: 44px;
  }
}

@media screen and (max-width: 768px) {
  .login-page {
    min-height: 100vh;
    padding: 0 16px;
    box-sizing: border-box;
  }

  .login-header {
    padding: 12px 16px;
  }

  .logo-text {
    font-size: 20px;
  }

  .logo-sub {
    font-size: 10px;
  }

  .login-container {
    width: 100%;
    max-width: 420px;
    margin-top: 40px;
    padding: 24px 20px 30px;
  }

  .login-panel-header {
    margin-bottom: 24px;
  }

  .login-panel-header h1 {
    font-size: 24px;
  }

  .form-item {
    margin-bottom: 16px;
  }

  .form-item label {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .form-item input {
    padding: 11px 12px;
    font-size: 15px;
  }

  .login-btn {
    padding: 14px;
    font-size: 15px;
  }

  .login-tip {
    margin-top: 16px;
    font-size: 13px;
  }
}

@media screen and (max-width: 375px) {
  .login-container {
    margin-top: 30px;
    padding: 20px 16px 24px;
  }

  .logo-text {
    font-size: 18px;
  }

  .form-item input {
    padding: 10px;
    font-size: 14px;
  }
}

.login-tip {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  line-height: 1.6;
  color: #6b7280;
}
</style>
