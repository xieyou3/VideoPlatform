<script setup lang="ts"> import { ref } from 'vue'; import { useRouter, RouterLink } from 'vue-router'; import { Mail, Lock, User, ArrowRight, Video } from 'lucide-vue-next'; import { useAuthStore } from '@/stores/auth'; const auth = useAuthStore(); const router = useRouter(); const username = ref(''); const email = ref(''); const password = ref(''); const isLoading = ref(false); const errorMessage = ref(''); const handleRegister = async () => { if (!username.value || !email.value || !password.value) { errorMessage.value = '请填写所有字段'; return; } isLoading.value = true; errorMessage.value = ''; try { const success = await auth.register({ username: username.value, email: email.value, password: password.value }); if (success) { router.push('/'); } else { errorMessage.value = '注册失败，请检查输入信息'; } } catch (error) { errorMessage.value = '注册失败，请稍后重试'; console.error('Register error:', error); } finally { isLoading.value = false; } }; </script>
<template> <div class="min-h-[calc(100vh-64px)] flex items-center justify-center px-4"> <div class="w-full max-w-md bg-zinc-900/50 backdrop-blur-xl border border-zinc-800 rounded-3xl p-8 shadow-2xl"> <div class="flex flex-col items-center gap-4 mb-8"> <div class="w-16 h-16 bg-orange-500 rounded-2xl flex items-center justify-center shadow-lg shadow-orange-500/20 rotate-12"> <Video class="w-10 h-10 text-white -rotate-12" /> </div> <h1 class="text-3xl font-bold text-zinc-100">创建账号</h1> <p class="text-zinc-400 text-sm">加入 V-Stream 社区，分享您的精彩瞬间</p> </div>
  <form @submit.prevent="handleRegister" class="flex flex-col gap-6">
    <div v-if="errorMessage" class="bg-red-500/10 border border-red-500/50 text-red-500 px-4 py-3 rounded-xl text-sm">
      {{ errorMessage }}
    </div>

    <div class="flex flex-col gap-2">
      <label class="text-xs font-bold text-zinc-500 uppercase tracking-wider ml-1">用户名</label>
      <div class="relative flex items-center bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 focus-within:border-orange-500 transition-colors">
        <User class="w-5 h-5 text-zinc-500" />
        <input
          v-model="username"
          type="text"
          placeholder="您的昵称"
          class="bg-transparent border-none outline-none w-full px-3 text-zinc-100"
          required
        />
      </div>
    </div>

    <div class="flex flex-col gap-2">
      <label class="text-xs font-bold text-zinc-500 uppercase tracking-wider ml-1">电子邮箱</label>
      <div class="relative flex items-center bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 focus-within:border-orange-500 transition-colors">
        <Mail class="w-5 h-5 text-zinc-500" />
        <input
          v-model="email"
          type="email"
          placeholder="name@example.com"
          class="bg-transparent border-none outline-none w-full px-3 text-zinc-100"
          required
        />
      </div>
    </div>

    <div class="flex flex-col gap-2">
      <label class="text-xs font-bold text-zinc-500 uppercase tracking-wider ml-1">密码</label>
      <div class="relative flex items-center bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 focus-within:border-orange-500 transition-colors">
        <Lock class="w-5 h-5 text-zinc-500" />
        <input
          v-model="password"
          type="password"
          placeholder="至少 8 位字符"
          class="bg-transparent border-none outline-none w-full px-3 text-zinc-100"
          required
          minlength="8"
        />
      </div>
    </div>

    <button
      type="submit"
      :disabled="isLoading"
      class="bg-orange-500 hover:bg-orange-600 disabled:opacity-50 text-white font-bold py-4 rounded-xl flex items-center justify-center gap-2 transition-all group"
    >
      {{ isLoading ? '注册中...' : '立即注册' }}
      <ArrowRight v-if="!isLoading" class="w-5 h-5 group-hover:translate-x-1 transition-transform" />
    </button>
  </form>

  <div class="mt-8 pt-8 border-t border-zinc-800 text-center">
    <p class="text-zinc-400 text-sm">
      已经有账号了?
      <RouterLink to="/login" class="text-orange-500 font-bold hover:underline">立即登录</RouterLink>
    </p>
  </div>
</div>
</div> </template>