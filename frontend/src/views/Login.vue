<template>
  <div class="login-page">
    <header class="login-header">
      <div class="logo">
        <span class="logo-text">优选通</span>
        <span class="logo-sub">保险服务平台</span>
      </div>
    </header>
    
    <div class="login-container">
      <div class="login-tabs">
        <div class="login-tab" :class="{ active: activeTab === 'login' }" @click="activeTab = 'login'">登录</div>
        <div class="login-tab" :class="{ active: activeTab === 'register' }" @click="activeTab = 'register'">注册</div>
      </div>

      <form v-if="activeTab === 'login'" @submit.prevent="handleLogin">
        <div class="form-item">
          <label>用户名</label>
          <input type="text" v-model="loginForm.username" placeholder="请输入用户名" required>
        </div>
        <div class="form-item">
          <label>密码</label>
          <input type="password" v-model="loginForm.password" placeholder="请输入密码" required>
        </div>
        <div class="form-item" v-if="loginError">
          <div class="error-msg">{{ loginError }}</div>
        </div>
        <button type="submit" class="login-btn" :disabled="loginLoading">
          {{ loginLoading ? '登录中...' : '登录' }}
        </button>
      </form>

      <form v-else @submit.prevent="handleRegister">
        <div class="form-item">
          <label>用户名</label>
          <input type="text" v-model="registerForm.username" placeholder="请输入用户名" required>
        </div>
        <div class="form-item">
          <label>手机号</label>
          <input type="tel" v-model="registerForm.mobile" placeholder="请输入手机号" required>
        </div>
        <div class="form-item">
          <label>密码</label>
          <input type="password" v-model="registerForm.password" placeholder="请输入密码" required>
        </div>
        <div class="form-item">
          <label>确认密码</label>
          <input type="password" v-model="registerForm.confirmPassword" placeholder="请再次输入密码" required>
        </div>
        <div class="form-item" v-if="registerError">
          <div class="error-msg">{{ registerError }}</div>
        </div>
        <button type="submit" class="login-btn" :disabled="registerLoading">
          {{ registerLoading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="register-link" v-if="activeTab === 'login'">
        还没有账号？<a @click="activeTab = 'register'">立即注册</a>
      </div>
      <div class="register-link" v-else>
        已有账号？<a @click="activeTab = 'login'">立即登录</a>
      </div>

      <div class="admin-hint" v-if="activeTab === 'login'">
        <p>管理员账号：<strong>admin</strong> / <strong>admin111</strong></p>
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
const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginError = ref('')
const registerError = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  mobile: '',
  password: '',
  confirmPassword: ''
})

const handleLogin = async () => {
  loginError.value = ''
  loginLoading.value = true

  try {
    const response = await axios.post('/api/auth/login', loginForm)
    if (response.data.code === '00000') {
      const data = response.data.data
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

const handleRegister = async () => {
  registerError.value = ''
  
  if (registerForm.password !== registerForm.confirmPassword) {
    registerError.value = '两次输入的密码不一致'
    return
  }

  if (registerForm.password.length < 6) {
    registerError.value = '密码长度不能少于6位'
    return
  }

  registerLoading.value = true

  try {
    const response = await axios.post('/api/auth/register', {
      username: registerForm.username,
      mobile: registerForm.mobile,
      password: registerForm.password
    })
    if (response.data.code === '00000') {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      loginForm.username = registerForm.username
      loginForm.password = ''
      registerForm.password = ''
      registerForm.confirmPassword = ''
    } else {
      registerError.value = response.data.message || '注册失败'
    }
  } catch (error) {
    const errorData = error.response?.data
    if (errorData?.code === 'A0400' && errorData?.data) {
      registerError.value = errorData.data
    } else {
      registerError.value = errorData?.message || '网络错误，请稍后重试'
    }
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-header {
  width: 100%;
  background-color: #fff;
  border-bottom: 3px solid #e60012;
  padding: 15px 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.logo {
  display: inline-flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-text {
  font-size: 22px;
  font-weight: bold;
  color: #e60012;
}

.logo-sub {
  font-size: 11px;
  color: #999;
}

.login-container {
  width: 420px;
  margin-top: 60px;
  padding: 30px 40px 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.login-tabs {
  display: flex;
  margin-bottom: 30px;
  border-bottom: 2px solid #eee;
}

.login-tab {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.3s;
}

.login-tab:hover {
  color: #e60012;
}

.login-tab.active {
  color: #e60012;
  border-bottom-color: #e60012;
  font-weight: 500;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-item input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-item input:focus {
  outline: none;
  border-color: #e60012;
  box-shadow: 0 0 0 2px rgba(230, 0, 18, 0.1);
}

.form-item input::placeholder {
  color: #c0c4cc;
}

.form-item .error-msg {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 6px;
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 4px;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: #e60012;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 10px;
}

.login-btn:hover {
  background: #c5000f;
}

.login-btn:active {
  transform: scale(0.98);
}

.login-btn:disabled {
  background: #fab6b6;
  cursor: not-allowed;
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

  .login-tabs {
    margin-bottom: 24px;
  }

  .login-tab {
    padding: 10px 0;
    font-size: 15px;
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

  .register-link {
    margin-top: 16px;
    font-size: 13px;
  }

  .admin-hint {
    margin-top: 16px;
    padding: 10px 12px;
    font-size: 12px;
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

.register-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #999;
}

.register-link a {
  color: #e60012;
  text-decoration: none;
  cursor: pointer;
}

.register-link a:hover {
  text-decoration: underline;
}

.admin-hint {
  text-align: center;
  margin-top: 20px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 4px;
  font-size: 12px;
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.admin-hint strong {
  color: #e60012;
}
</style>
