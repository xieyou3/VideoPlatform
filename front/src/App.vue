<script setup lang="ts">
import { onMounted } from 'vue';
import { RouterView } from 'vue-router';
import Navbar from '@/components/Navbar.vue';
import { useAuthStore } from './stores/auth';

const authStore = useAuthStore();

onMounted(() => {
  authStore.fetchUser();
});
</script>

<template>
  <div class="min-h-screen bg-zinc-950 text-zinc-100 font-sans">
    <Navbar />
    <main class="pt-16">
      <RouterView />
    </main>
  </div>
</template>

<style>
/* 样式已移至全局 index.css */
</style>
