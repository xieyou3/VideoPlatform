<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router';
import { 
  Video, 
  Search, 
  PlusSquare, 
  Users, 
  MessageCircle, 
  User, 
  LogOut,
  Bell
} from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';
import { useChatStore } from '@/stores/chat';

const auth = useAuthStore();
const chatStore = useChatStore();
const router = useRouter();

const handleLogout = () => {
  chatStore.disconnect();
  auth.logout();
  router.push('/login');
};
</script>

<template>
  <nav class="fixed top-0 left-0 right-0 h-16 bg-zinc-900/80 backdrop-blur-md border-b border-zinc-800 z-50 px-4 flex items-center justify-between">
    <div class="flex items-center gap-8">
      <RouterLink to="/" class="flex items-center gap-2 text-orange-500 font-bold text-xl">
        <Video class="w-8 h-8" />
        <span>V-Stream</span>
      </RouterLink>

      <div class="hidden md:flex items-center bg-zinc-800 rounded-full px-4 py-1.5 w-96 border border-zinc-700 focus-within:border-orange-500 transition-colors">
        <Search class="w-4 h-4 text-zinc-400" />
        <input 
          type="text" 
          placeholder="搜索视频、好友..." 
          class="bg-transparent border-none outline-none px-3 text-sm w-full text-zinc-100"
        />
      </div>
    </div>

    <div class="flex items-center gap-4">
      <template v-if="auth.isLoggedIn">
        <RouterLink to="/upload" class="p-2 hover:bg-zinc-800 rounded-full transition-colors" title="上传视频">
          <PlusSquare class="w-6 h-6 text-zinc-300" />
        </RouterLink>
        <RouterLink to="/social" class="p-2 hover:bg-zinc-800 rounded-full transition-colors" title="社交中心">
          <Users class="w-6 h-6 text-zinc-300" />
        </RouterLink>
        <RouterLink to="/chat" class="p-2 hover:bg-zinc-800 rounded-full transition-colors relative" title="消息">
          <MessageCircle class="w-6 h-6 text-zinc-300" />
          <span v-if="chatStore.unreadTotal > 0" class="absolute top-1 right-1 min-w-[18px] h-[18px] bg-orange-500 rounded-full text-[10px] font-bold flex items-center justify-center px-1 border-2 border-zinc-900">
            {{ chatStore.unreadTotal > 99 ? '99+' : chatStore.unreadTotal }}
          </span>
        </RouterLink>
        <button class="p-2 hover:bg-zinc-800 rounded-full transition-colors relative">
          <Bell class="w-6 h-6 text-zinc-300" />
          <span class="absolute top-2 right-2 w-2 h-2 bg-orange-500 rounded-full border-2 border-zinc-900"></span>
        </button>
        
        <div class="h-8 w-px bg-zinc-800 mx-2"></div>

        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-full bg-orange-500 flex items-center justify-center font-bold text-sm">
            {{ auth.user?.username?.[0]?.toUpperCase() || 'U' }}
          </div>
          <button @click="handleLogout" class="text-zinc-400 hover:text-zinc-100 transition-colors">
            <LogOut class="w-5 h-5" />
          </button>
        </div>
      </template>
      <template v-else>
        <RouterLink to="/login" class="text-sm font-medium hover:text-orange-500 transition-colors">登录</RouterLink>
        <RouterLink to="/register" class="bg-orange-500 hover:bg-orange-600 text-white px-4 py-1.5 rounded-full text-sm font-medium transition-colors">注册</RouterLink>
      </template>
    </div>
  </nav>
</template>
