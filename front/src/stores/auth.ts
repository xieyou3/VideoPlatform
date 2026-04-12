import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { api } from '@/utils/api';

export interface User {
  id: string;
  username: string;
  email: string;
  avatar?: string;
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null);
  const token = ref<string | null>(null);
  
  let refreshTimer: number | null = null;

  const isLoggedIn = computed(() => !!token.value && !!user.value);

  function startRefreshTimer() {
    if (refreshTimer) {
      clearInterval(refreshTimer);
    }
    
    console.log('启动 token 刷新定时器，25分钟后刷新');
    refreshTimer = window.setInterval(async () => {
      console.log('执行定时 token 刷新');
      const success = await refreshTokens();
      if (!success) {
        console.error('定时刷新失败，已退出登录');
      }
    }, 25 * 60 * 1000);
  }

  function stopRefreshTimer() {
    if (refreshTimer) {
      clearInterval(refreshTimer);
      refreshTimer = null;
      console.log('停止 token 刷新定时器');
    }
  }

  async function refreshTokens() {
    try {
      console.log('调用刷新 token API');
      const response = await api.post<{ token: string; user: User }>('/auth/refresh', {}, { skipErrorRedirect: true });
      
      console.log('Token 刷新成功');
      user.value = response.user;
      token.value = response.token;
      startRefreshTimer();
      
      return true;
    } catch (error: any) {
      console.error('Token 刷新失败:', error);
      logout();
      return false;
    }
  }

  async function login(credentials: { email: string; password: string }) {
    try {
      const response = await api.post<{ token: string; user: User }>('/auth/login', credentials);
      user.value = response.user;
      token.value = response.token;
      startRefreshTimer();
      return true;
    } catch (error) {
      console.error('Login error:', error);
      return false;
    }
  }

  async function register(userData: { username: string; email: string; password: string }) {
    try {
      console.log('调用注册 API:', userData.username, userData.email);
      const response = await api.post<{ token: string; user: User }>('/auth/register', userData);
      console.log('注册 API 响应:', response);
      user.value = response.user;
      token.value = response.token;
      startRefreshTimer();
      return true;
    } catch (error: any) {
      console.error('Register error:', error);
      console.error('Error message:', error.message);
      console.error('Error stack:', error.stack);
      throw error;
    }
  }

  async function fetchUser() {
    try {
      const data = await api.get<{ user: User }>('/auth/me', { skipErrorRedirect: true });
      user.value = data.user;
      token.value = 'valid';
      startRefreshTimer();
      return true;
    } catch (error) {
      console.error('Fetch user error:', error);
      return false;
    }
  }

  async function initAuth() {
    console.log('初始化认证状态，尝试刷新 token 对');
    
    const refreshed = await refreshTokens();
    
    if (refreshed) {
      console.log('Token 刷新成功，用户会话已恢复');
    } else {
      console.log('Token 刷新失败，需要重新登录');
    }
  }

  function logout() {
    stopRefreshTimer();
    user.value = null;
    token.value = null;
    
    api.post('/auth/logout', {}).catch(err => {
      console.error('Logout API error:', err);
    });
  }

  return { 
    user, 
    token, 
    isLoggedIn, 
    login, 
    register, 
    logout, 
    fetchUser,
    refreshTokens,
    initAuth 
  };
});
