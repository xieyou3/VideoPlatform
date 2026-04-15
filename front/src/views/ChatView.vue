<script setup lang="ts">
import { onMounted, onBeforeUnmount } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useChatStore } from '@/stores/chat';
import ChatSidebar from '@/components/ChatSidebar.vue';
import ChatContent from '@/components/ChatContent.vue';

const authStore = useAuthStore();
const chatStore = useChatStore();

onMounted(async () => {
  if (authStore.user) {
    console.log('ChatView 挂载，初始化聊天');
    await chatStore.initChat();
  }
});

onBeforeUnmount(() => {
  console.log('ChatView 即将卸载，但保持 WebSocket 连接');
});
</script>

<template>
  <div class="h-[calc(100vh-64px)] flex bg-zinc-950">
    <ChatSidebar />
    <ChatContent />
  </div>
</template>
