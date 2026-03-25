import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'
import router from './router'

axios.defaults.baseURL = 'http://localhost:8080'
axios.defaults.withCredentials = true

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('isLoggedIn')
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
