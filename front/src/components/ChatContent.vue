<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue';
import { useChatStore } from '@/stores/chat';
import { Send, Bot, MessageCircle, Image, FileText, Sparkles, Download, AlertCircle } from 'lucide-vue-next';

const chatStore = useChatStore();
const inputMessage = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const messageType = ref<'text' | 'image' | 'ppt'>('text');
const imageLoadErrors = ref<Set<number>>(new Set());

const currentPartnerName = computed(() => {
  if (!chatStore.currentSession) return '';
  return chatStore.currentSession.partnerId === 0
      ? 'AI 助手'
      : (chatStore.currentSession.partnerName || '好友');
});

const isAIChat = computed(() => {
  const result = chatStore.currentSession?.partnerId === 0;
  console.log('isAIChat 计算:', {
    currentSession: chatStore.currentSession,
    partnerId: chatStore.currentSession?.partnerId,
    isAIChat: result
  });
  return result;
});

watch(() => chatStore.messages.length, () => {
  scrollToBottom();
});

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
}

function getMessagePlaceholder() {
  if (!isAIChat.value) return '输入消息... (Enter 发送, Shift+Enter 换行)';

  const placeholders = {
    text: '向 AI 提问... (Enter 发送, Shift+Enter 换行)',
    image: '描述你想生成的图片... (Enter 发送)',
    ppt: '描述你想生成的 PPT 内容... (Enter 发送)'
  };

  return placeholders[messageType.value];
}

function handleSend() {
  if (!inputMessage.value.trim()) return;

  console.log('发送消息:', {
    content: inputMessage.value.trim(),
    isAIChat: isAIChat.value,
    messageType: messageType.value,
    currentSession: chatStore.currentSession
  });

  if (isAIChat.value) {
    console.log('调用 sendAIMessage');
    chatStore.sendAIMessage(inputMessage.value.trim(), messageType.value);
  } else {
    console.log('调用 sendMessage');
    chatStore.sendMessage(inputMessage.value.trim());
  }

  inputMessage.value = '';
  scrollToBottom();
}

function handleKeyPress(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    handleSend();
  }
}

function formatTime(timeStr: string) {
  const date = new Date(timeStr);
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}

function isMyMessage(message: any) {
  if (message.senderId === 0) return false;
  if (!chatStore.currentSession) return false;
  return message.senderId.toString() !== chatStore.currentSession.partnerId.toString();
}

function isImageUrl(url: string): boolean {
  if (!url) {
    console.log('isImageUrl: url为空');
    return false;
  }

  const base64Prefix = 'data:image/';
  if (url.startsWith(base64Prefix)) {
    console.log('isImageUrl: 完整Base64格式');
    return true;
  }

  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp', '.svg'];
  const lowerUrl = url.toLowerCase();

  if (lowerUrl.startsWith('http://') || lowerUrl.startsWith('https://')) {
    const result = imageExtensions.some(ext => lowerUrl.includes(ext) || lowerUrl.endsWith(ext));
    console.log('isImageUrl: URL格式, 结果:', result);
    return result;
  }

  if (url.length > 100 && /^[A-Za-z0-9+/=]+$/.test(url.substring(0, 100))) {
    if (url.startsWith('iVBORw0KGgo') || url.startsWith('/9j/4AAQSkZJRg') || url.startsWith('R0lGODlh')) {
      console.log('isImageUrl: 纯Base64格式, 长度:', url.length);
      return true;
    }
  }

  console.log('isImageUrl: 未识别格式, 长度:', url.length, '前50字符:', url.substring(0, 50));
  return false;
}

function handleImageError(messageId: number) {
  imageLoadErrors.value.add(messageId);
}

function downloadImage(imageSrc: string, messageId: number) {
  try {
    const link = document.createElement('a');

    if (imageSrc.startsWith('data:image/')) {
      link.href = imageSrc;
      const timestamp = new Date().getTime();
      link.download = `image_${timestamp}.png`;
    } else {
      link.href = imageSrc;
      link.download = `image_${messageId}.png`;
      link.target = '_blank';
    }

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    console.error('下载图片失败:', error);
    alert('图片下载失败，请重试');
  }
}

function parsePPTContent(content: string) {
  try {
    return JSON.parse(content);
  } catch (e) {
    return null;
  }
}
</script>

<template>
  <div class="flex-1 flex flex-col bg-zinc-950">
    <div v-if="!chatStore.currentSession" class="flex-1 flex items-center justify-center text-zinc-500">
      <div class="text-center">
        <MessageCircle class="w-20 h-20 mx-auto mb-4 opacity-30" />
        <p class="text-lg">选择一个会话开始聊天</p>
      </div>
    </div>

    <template v-else>
      <div class="h-16 border-b border-zinc-800 flex items-center px-6 bg-zinc-900">
        <div class="flex items-center gap-3">
          <div
              v-if="isAIChat"
              class="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center"
          >
            <Bot class="w-5 h-5 text-white" />
          </div>
          <div
              v-else
              class="w-10 h-10 rounded-full bg-orange-500 flex items-center justify-center font-bold"
          >
            {{ currentPartnerName[0]?.toUpperCase() }}
          </div>
          <div>
            <h3 class="font-bold text-zinc-100">{{ currentPartnerName }}</h3>
            <p v-if="isAIChat" class="text-xs text-zinc-400">在线</p>
          </div>
        </div>
      </div>

      <div
          ref="messagesContainer"
          class="flex-1 overflow-y-auto p-6 space-y-4"
      >
        <div
            v-for="message in chatStore.messages"
            :key="message.id"
            class="flex"
            :class="{ 'justify-end': isMyMessage(message) }"
        >
          <div
              class="max-w-[70%] rounded-2xl px-4 py-2"
              :class="[
              message.senderId === 0
                ? 'bg-gradient-to-br from-purple-600 to-pink-600 text-white'
                : isMyMessage(message)
                  ? 'bg-orange-500 text-white'
                  : 'bg-zinc-800 text-zinc-100'
            ]"
          >
            <div v-if="message.messageType === 2 && isImageUrl(message.content)">
              <div v-if="!imageLoadErrors.has(message.id)" class="relative group">
                <img
                  :src="message.content"
                  alt="生成的图片"
                  class="rounded-lg max-w-full cursor-pointer hover:opacity-95 transition-opacity"
                  @error="handleImageError(message.id)"
                  @click="downloadImage(message.content, message.id)"
                />
                <button
                  @click.stop="downloadImage(message.content, message.id)"
                  class="absolute top-2 right-2 p-2 bg-black/50 hover:bg-black/70 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity"
                  title="下载图片"
                >
                  <Download class="w-4 h-4 text-white" />
                </button>
              </div>
              <div v-else class="flex items-center gap-2 text-red-300 bg-red-500/20 px-3 py-2 rounded-lg">
                <AlertCircle class="w-4 h-4" />
                <span class="text-sm">图片加载失败</span>
              </div>
            </div>
            <div v-else-if="message.messageType === 3">
              <div class="flex flex-col gap-3">
                <div class="flex items-center gap-2">
                  <FileText class="w-4 h-4" />
                  <span class="font-medium">PPT 已生成</span>
                </div>
                
                <div v-if="parsePPTContent(message.content)">
                  <div v-if="parsePPTContent(message.content).coverImg" class="mb-2">
                    <img 
                      :src="parsePPTContent(message.content).coverImg" 
                      :alt="parsePPTContent(message.content).title"
                      class="rounded-lg max-w-full cursor-pointer hover:opacity-95 transition-opacity"
                      @click="window.open(parsePPTContent(message.content).coverImg, '_blank')"
                    />
                  </div>
                  <p v-if="parsePPTContent(message.content).title" class="text-sm font-medium mb-2">
                    {{ parsePPTContent(message.content).title }}
                  </p>
                  <a 
                    :href="parsePPTContent(message.content).coverImg"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-2 px-3 py-2 bg-blue-500/20 hover:bg-blue-500/30 text-blue-300 rounded-lg transition-colors text-sm"
                  >
                    <Download class="w-4 h-4" />
                    查看封面图
                  </a>
                  <p class="text-xs text-zinc-400 mt-1">
                    任务ID: {{ parsePPTContent(message.content).sid }}
                  </p>
                </div>
                <p v-else class="text-sm text-zinc-400">
                  PPT 信息解析失败
                </p>
              </div>
            </div>
            <p v-else class="text-sm whitespace-pre-wrap break-words">{{ message.content }}</p>
            <p class="text-xs mt-1 opacity-70 text-right">
              {{ formatTime(message.createdAt) }}
            </p>
          </div>
        </div>
      </div>

      <div class="border-t border-zinc-800 p-4 bg-zinc-900">
        <div v-if="isAIChat" class="flex gap-2 mb-3">
          <button
              @click="messageType = 'text'"
              class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
              :class="messageType === 'text' ? 'bg-purple-600 text-white' : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'"
          >
            <Sparkles class="w-3.5 h-3.5" />
            文本对话
          </button>
          <button
              @click="messageType = 'image'"
              class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
              :class="messageType === 'image' ? 'bg-purple-600 text-white' : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'"
          >
            <Image class="w-3.5 h-3.5" />
            生成图片
          </button>
          <button
              @click="messageType = 'ppt'"
              class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
              :class="messageType === 'ppt' ? 'bg-purple-600 text-white' : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'"
          >
            <FileText class="w-3.5 h-3.5" />
            生成 PPT
          </button>
        </div>

        <div class="flex items-end gap-3">
          <textarea
              v-model="inputMessage"
              @keydown="handleKeyPress"
              :placeholder="getMessagePlaceholder()"
              class="flex-1 bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 text-sm text-zinc-100 placeholder-zinc-500 focus:outline-none focus:border-orange-500 resize-none"
              rows="2"
          ></textarea>
          <button
              @click="handleSend"
              :disabled="!inputMessage.trim()"
              class="p-3 bg-orange-500 hover:bg-orange-600 disabled:bg-zinc-700 disabled:cursor-not-allowed rounded-xl transition-colors"
          >
            <Send class="w-5 h-5 text-white" />
          </button>
        </div>
        <p v-if="isAIChat" class="text-xs text-zinc-500 mt-2">
          💡 AI 助手可以帮你生成文本、图片和 PPT
        </p>
      </div>
    </template>
  </div>
</template>
