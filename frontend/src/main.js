import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'
import router from './router'

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || ''

axios.interceptors.request.use(config => {
  const authToken = sessionStorage.getItem('authToken')
  if (authToken) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${authToken}`
  }
  return config
})

axios.interceptors.response.use(
  response => {
    if (response?.data?.code === 'A0401') {
      sessionStorage.removeItem('authToken')
      sessionStorage.removeItem('isLoggedIn')
      sessionStorage.removeItem('userId')
      sessionStorage.removeItem('userType')
      sessionStorage.removeItem('username')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('authToken')
      sessionStorage.removeItem('isLoggedIn')
      sessionStorage.removeItem('userId')
      sessionStorage.removeItem('userType')
      sessionStorage.removeItem('username')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

const app = createApp(App)

app.config.globalProperties.$axios = axios

app.use(ElementPlus)
app.use(router)

app.mount('#app')
