<script setup lang="ts">
import { computed } from 'vue';
import { useChatStore } from '@/stores/chat';
import { Bot, Clock, MessageCircle } from 'lucide-vue-next';

const chatStore = useChatStore();

const sortedSessions = computed(() => {
  const aiSession = chatStore.sessions.find(s => s.partnerId === 0);
  const otherSessions = chatStore.sessions
    .filter(s => s.partnerId !== 0)
    .sort((a, b) => {
      return new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime();
    });

  return aiSession ? [aiSession, ...otherSessions] : otherSessions;
});

function formatTime(timeStr: string) {
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 60000) {
    return '刚刚';
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`;
  } else {
    return `${date.getMonth() + 1}/${date.getDate()}`;
  }
}

async function handleSelectSession(session: any) {
  await chatStore.selectSession(session);
}
</script>

<template>
  <div class="w-80 border-r border-zinc-800 bg-zinc-900 flex flex-col">
    <div class="h-16 border-b border-zinc-800 flex items-center px-4">
      <h2 class="text-lg font-bold text-zinc-100">消息</h2>
    </div>

    <div class="flex-1 overflow-y-auto">
      <div
          v-for="session in sortedSessions"
          :key="session.id"
          @click="handleSelectSession(session)"
          class="flex items-center gap-3 p-4 hover:bg-zinc-800 cursor-pointer transition-colors border-b border-zinc-800/50"
          :class="{ 'bg-zinc-800': chatStore.currentSession?.id === session.id }"
      >
        <div class="relative">
          <div
              v-if="session.partnerId === 0"
              class="w-12 h-12 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center"
          >
            <Bot class="w-6 h-6 text-white" />
          </div>
          <div
              v-else
              class="w-12 h-12 rounded-full bg-orange-500 flex items-center justify-center font-bold text-lg"
          >
            {{ session.partnerName?.[0]?.toUpperCase() || 'U' }}
          </div>

          <div
              v-if="session.unreadCount > 0"
              class="absolute -top-1 -right-1 w-5 h-5 bg-orange-500 rounded-full flex items-center justify-center text-xs font-bold border-2 border-zinc-900"
          >
            {{ session.unreadCount > 99 ? '99+' : session.unreadCount }}
          </div>
        </div>

        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between mb-1">
            <h3 class="font-medium text-zinc-100 truncate">
              {{ session.partnerId === 0 ? 'AI 助手' : (session.partnerName || '好友') }}
            </h3>
            <span class="text-xs text-zinc-500 flex items-center gap-1">
              <Clock class="w-3 h-3" />
              {{ formatTime(session.lastMessageTime) }}
            </span>
          </div>
          <p class="text-sm text-zinc-400 truncate">
            {{ session.lastMessageContent || '暂无消息' }}
          </p>
        </div>
      </div>

      <div v-if="sortedSessions.length === 0" class="flex flex-col items-center justify-center h-full text-zinc-500">
        <MessageCircle class="w-16 h-16 mb-4 opacity-50" />
        <p>暂无会话</p>
      </div>
    </div>
  </div>
</template>
