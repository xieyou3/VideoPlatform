<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import {
  Users, 
  MessageCircle, 
  UserPlus, 
  Search, 
  Send, 
  Smile, 
  Plus, 
  MoreHorizontal,
  Phone,
  Video as VideoIcon,
  Check,
  CheckCheck
} from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';
import { createSession } from '@/api/chat';

const auth = useAuthStore();
const router = useRouter();
const activeTab = ref<'chats' | 'friends'>('chats');
const activeChatId = ref<string | null>(null);
const messageInput = ref('');
const chatContainer = ref<HTMLElement | null>(null);
const showAddFriendModal = ref(false);
const friendSearchInput = ref('');

interface Message {
  id: string;
  senderId: string;
  text: string;
  time: string;
  status: 'sent' | 'delivered' | 'read';
}

interface Chat {
  id: string;
  name: string;
  avatar: string;
  lastMessage: string;
  time: string;
  unread: number;
  type: 'private' | 'group';
  messages: Message[];
  partnerId?: number;
}

const chats = reactive<Chat[]>([]);
const friends = reactive<any[]>([]);

const activeChat = ref<Chat | null>(null);

const selectChat = (chat: Chat) => {
  activeChatId.value = chat.id;
  activeChat.value = chat;
  chat.unread = 0;
  scrollToBottom();
};

const startChatWithFriend = async (friend: any) => {
  if (!auth.user) return;

  try {
    await createSession({
      userId: parseInt(auth.user.id),
      receiverId: friend.id
    });
    router.push('/chat');
  } catch (error) {
    console.error('创建会话失败:', error);
    alert('创建会话失败，请重试');
  }
};

const handleAddFriend = () => {
  showAddFriendModal.value = true;
};

const closeAddFriendModal = () => {
  showAddFriendModal.value = false;
  friendSearchInput.value = '';
};

const searchAndAddFriend = async () => {
  if (!friendSearchInput.value.trim()) {
    alert('请输入用户名或ID');
    return;
  }

  try {
    const response = await fetch(`/api/social/friends`);
    const result = await response.json();

    if (result.code === 200 && result.data) {
      const found = result.data.find((f: any) =>
        f.name === friendSearchInput.value || f.id.toString() === friendSearchInput.value
      );

      if (found) {
        alert(`找到用户: ${found.name}\n\n提示：好友功能正在开发中，请稍后再试。`);
      } else {
        alert('未找到该用户，请检查输入是否正确');
      }
      closeAddFriendModal();
    } else {
      alert('获取好友列表失败');
    }
  } catch (error) {
    console.error('搜索好友失败:', error);
    alert('搜索失败，请重试');
  }
};

const sendMessage = () => {
  if (!messageInput.value.trim() || !activeChat.value) return;
  
  const newMessage: Message = {
    id: Date.now().toString(),
    senderId: 'me',
    text: messageInput.value,
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    status: 'sent'
  };
  
  activeChat.value.messages.push(newMessage);
  activeChat.value.lastMessage = messageInput.value;
  activeChat.value.time = newMessage.time;
  messageInput.value = '';
  
  scrollToBottom();
  
  setTimeout(() => {
    if (activeChat.value) {
      const reply: Message = {
        id: Date.now().toString(),
        senderId: activeChat.value.id,
        text: '收到，谢谢！',
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        status: 'delivered'
      };
      activeChat.value.messages.push(reply);
      activeChat.value.lastMessage = reply.text;
      activeChat.value.time = reply.time;
      scrollToBottom();
    }
  }, 1500);
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
    }
  });
};

const fetchData = async () => {
  try {
    const [chatsRes, friendsRes] = await Promise.all([
      fetch('/api/social/chats'),
      fetch('/api/social/friends')
    ]);
    
    const chatsJson = await chatsRes.json();
    const friendsJson = await friendsRes.json();
    
    const chatsData = chatsJson.data || [];
    const friendsData = friendsJson.data || [];

    chats.push(...(Array.isArray(chatsData) ? chatsData : []));
    friends.push(...(Array.isArray(friendsData) ? friendsData : []));
    
    if (chats.length > 0) {
      selectChat(chats[0]);
    }
  } catch (error) {
    console.error('Failed to fetch social data:', error);
  }
};

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 py-6 h-[calc(100vh-64px)] flex gap-6 overflow-hidden">
    <!-- Left Sidebar: Conversations/Friends -->
    <div class="w-80 flex flex-col bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden shadow-xl">
      <!-- Tabs -->
      <div class="flex border-b border-zinc-800 p-2">
        <button 
          @click="activeTab = 'chats'"
          class="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl transition-all font-bold text-sm"
          :class="activeTab === 'chats' ? 'bg-orange-500 text-white' : 'text-zinc-500 hover:bg-zinc-800'"
        >
          <MessageCircle class="w-4 h-4" />
          聊天
        </button>
        <button 
          @click="activeTab = 'friends'"
          class="flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl transition-all font-bold text-sm"
          :class="activeTab === 'friends' ? 'bg-orange-500 text-white' : 'text-zinc-500 hover:bg-zinc-800'"
        >
          <Users class="w-4 h-4" />
          好友
        </button>
      </div>

      <!-- Search -->
      <div class="p-4">
        <div class="flex items-center bg-zinc-800 rounded-xl px-4 py-2 border border-zinc-700 focus-within:border-orange-500 transition-colors">
          <Search class="w-4 h-4 text-zinc-500" />
          <input 
            type="text" 
            placeholder="搜索..." 
            class="bg-transparent border-none outline-none w-full px-3 text-sm text-zinc-100"
          />
        </div>
      </div>

      <!-- List -->
      <div class="flex-1 overflow-y-auto px-2 pb-4">
        <template v-if="activeTab === 'chats'">
          <div 
            v-for="chat in chats" 
            :key="chat.id"
            @click="selectChat(chat)"
            class="flex items-center gap-3 p-3 rounded-2xl cursor-pointer transition-all hover:bg-zinc-800 group relative"
            :class="{ 'bg-zinc-800 border border-zinc-700': activeChatId === chat.id }"
          >
            <div class="relative flex-shrink-0">
              <img :src="chat.avatar" class="w-12 h-12 rounded-2xl object-cover" />
              <span v-if="chat.unread > 0" class="absolute -top-1 -right-1 w-5 h-5 bg-orange-500 rounded-full flex items-center justify-center text-[10px] font-bold text-white border-2 border-zinc-900">
                {{ chat.unread }}
              </span>
            </div>
            <div class="flex-1 overflow-hidden">
              <div class="flex justify-between items-center mb-1">
                <span class="font-bold text-sm text-zinc-100 truncate">{{ chat.name }}</span>
                <span class="text-[10px] text-zinc-500">{{ chat.time }}</span>
              </div>
              <p class="text-xs text-zinc-500 truncate">{{ chat.lastMessage }}</p>
            </div>
          </div>
        </template>
        <template v-else>
          <div 
            v-for="friend in friends" 
            :key="friend.id"
            class="flex items-center gap-3 p-3 rounded-2xl cursor-pointer transition-all hover:bg-zinc-800 group"
          >
            <div class="relative flex-shrink-0">
              <img :src="friend.avatar" class="w-12 h-12 rounded-2xl object-cover" />
              <span 
                class="absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-zinc-900"
                :class="friend.status === 'online' ? 'bg-green-500' : 'bg-zinc-600'"
              ></span>
            </div>
            <div class="flex-1">
              <span class="font-bold text-sm text-zinc-100">{{ friend.name }}</span>
              <p class="text-[10px] text-zinc-500">{{ friend.status === 'online' ? '在线' : '离线' }}</p>
            </div>
            <button
              @click.stop="startChatWithFriend(friend)"
              class="p-2 bg-zinc-800 rounded-xl opacity-0 group-hover:opacity-100 transition-opacity hover:bg-orange-500/20"
            >
              <MessageCircle class="w-4 h-4 text-orange-500" />
            </button>
          </div>
          <button
            @click="handleAddFriend"
            class="w-full flex items-center justify-center gap-2 p-4 text-orange-500 text-sm font-bold hover:bg-orange-500/5 rounded-2xl mt-2 transition-colors"
          >
            <UserPlus class="w-4 h-4" />
            添加新好友
          </button>
        </template>
      </div>
    </div>

    <!-- Add Friend Modal -->
    <div
      v-if="showAddFriendModal"
      class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center"
      @click.self="closeAddFriendModal"
    >
      <div class="bg-zinc-900 border border-zinc-800 rounded-3xl p-6 w-full max-w-md shadow-2xl">
        <div class="flex items-center justify-between mb-6">
          <h3 class="text-xl font-bold text-zinc-100">添加好友</h3>
          <button
            @click="closeAddFriendModal"
            class="p-2 hover:bg-zinc-800 rounded-xl transition-colors"
          >
            <svg class="w-5 h-5 text-zinc-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-zinc-400 mb-2">搜索用户</label>
            <div class="flex gap-2">
              <input
                v-model="friendSearchInput"
                @keyup.enter="searchAndAddFriend"
                type="text"
                placeholder="输入用户名或ID"
                class="flex-1 bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-2 text-sm text-zinc-100 outline-none focus:border-orange-500 transition-colors"
              />
              <button
                @click="searchAndAddFriend"
                class="px-4 py-2 bg-orange-500 hover:bg-orange-600 rounded-xl text-white font-medium transition-colors"
              >
                搜索
              </button>
            </div>
          </div>

          <div class="pt-4 border-t border-zinc-800">
            <p class="text-xs text-zinc-500">
              提示：输入对方的用户名或ID进行搜索，发送好友请求后等待对方通过即可开始聊天。
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Chat Area -->
    <div class="flex-1 flex flex-col bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden shadow-xl relative">
      <template v-if="activeChat">
        <!-- Chat Header -->
        <div class="px-6 py-4 border-b border-zinc-800 flex items-center justify-between bg-zinc-900/50 backdrop-blur-md z-10">
          <div class="flex items-center gap-4">
            <img :src="activeChat.avatar" class="w-10 h-10 rounded-xl object-cover" />
            <div>
              <h2 class="font-bold text-zinc-100">{{ activeChat.name }}</h2>
              <p class="text-[10px] text-green-500 flex items-center gap-1">
                <span class="w-1.5 h-1.5 bg-green-500 rounded-full"></span>
                在线
              </p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button class="p-2 hover:bg-zinc-800 rounded-xl transition-colors text-zinc-400">
              <Phone class="w-5 h-5" />
            </button>
            <button class="p-2 hover:bg-zinc-800 rounded-xl transition-colors text-zinc-400">
              <VideoIcon class="w-5 h-5" />
            </button>
            <button class="p-2 hover:bg-zinc-800 rounded-xl transition-colors text-zinc-400">
              <MoreHorizontal class="w-5 h-5" />
            </button>
          </div>
        </div>

        <!-- Messages Area -->
        <div 
          ref="chatContainer"
          class="flex-1 overflow-y-auto p-6 flex flex-col gap-4 bg-[url('https://www.transparenttextures.com/patterns/carbon-fibre.png')]"
        >
          <div 
            v-for="msg in activeChat.messages" 
            :key="msg.id"
            class="flex flex-col max-w-[70%]"
            :class="msg.senderId === 'me' ? 'self-end items-end' : 'self-start items-start'"
          >
            <div 
              class="px-4 py-2.5 rounded-2xl text-sm leading-relaxed shadow-sm"
              :class="msg.senderId === 'me' ? 'bg-orange-500 text-white rounded-tr-none' : 'bg-zinc-800 text-zinc-100 rounded-tl-none'"
            >
              {{ msg.text }}
            </div>
            <div class="flex items-center gap-1.5 mt-1 text-[10px] text-zinc-500">
              <span>{{ msg.time }}</span>
              <template v-if="msg.senderId === 'me'">
                <CheckCheck v-if="msg.status === 'read'" class="w-3 h-3 text-orange-400" />
                <Check v-else class="w-3 h-3" />
              </template>
            </div>
          </div>
        </div>

        <!-- Message Input -->
        <div class="p-4 bg-zinc-900 border-t border-zinc-800">
          <div class="flex items-center gap-3 bg-zinc-800 rounded-2xl px-4 py-2 border border-zinc-700 focus-within:border-orange-500 transition-colors">
            <button class="p-2 hover:bg-zinc-700 rounded-xl transition-colors text-zinc-400">
              <Plus class="w-5 h-5" />
            </button>
            <input 
              v-model="messageInput"
              @keyup.enter="sendMessage"
              type="text" 
              placeholder="输入消息..." 
              class="bg-transparent border-none outline-none flex-1 text-sm text-zinc-100 py-2"
            />
            <button class="p-2 hover:bg-zinc-700 rounded-xl transition-colors text-zinc-400">
              <Smile class="w-5 h-5" />
            </button>
            <button 
              @click="sendMessage"
              class="p-2 bg-orange-500 hover:bg-orange-600 rounded-xl transition-colors text-white shadow-lg shadow-orange-500/20"
            >
              <Send class="w-5 h-5" />
            </button>
          </div>
        </div>
      </template>
      <div v-else class="flex-1 flex flex-col items-center justify-center gap-4 text-zinc-500">
        <div class="w-20 h-20 bg-zinc-800 rounded-3xl flex items-center justify-center">
          <MessageCircle class="w-10 h-10" />
        </div>
        <p class="font-bold">选择一个会话开始聊天</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Custom scrollbar for chat */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #27272a;
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: #3f3f46;
}
</style>
