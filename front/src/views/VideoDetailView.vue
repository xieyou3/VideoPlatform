<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { useRoute } from 'vue-router';
import { 
  Heart, 
  MessageSquare, 
  Share2, 
  Star, 
  Send, 
  ThumbsUp, 
  MoreVertical,
  Play,
  Pause,
  Volume2,
  Maximize
} from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const auth = useAuthStore();

const videoId = route.params.id as string;
const video = ref<any>(null);
const isPlaying = ref(false);
const isLiked = ref(false);
const isFavorited = ref(false);
const showDanmaku = ref(true);
const danmakuInput = ref('');
const commentInput = ref('');

interface Danmaku {
  id: string;
  text: string;
  time: number;
  color: string;
  top: number;
}

const danmakus = reactive<Danmaku[]>([]);

const activeDanmakus = ref<Danmaku[]>([]);
let danmakuTimer: any = null;

const fetchVideoData = async () => {
  try {
    const [videoRes, danmakuRes] = await Promise.all([
      fetch(`/api/videos/${videoId}`),
      fetch(`/api/videos/${videoId}/danmaku`)
    ]);
    
    if (videoRes.ok) {
      video.value = await videoRes.json();
    }
    
    const danmakuData = await danmakuRes.json();
    danmakus.push(...danmakuData);
  } catch (error) {
    console.error('Failed to fetch video data:', error);
  }
};

const startDanmaku = () => {
  danmakuTimer = setInterval(() => {
    if (isPlaying.value) {
      // Simulate danmaku appearing from the pool
      const random = Math.random();
      if (random > 0.5 && danmakus.length > 0) {
        const poolDanmaku = danmakus[Math.floor(Math.random() * danmakus.length)];
        const newDanmaku: Danmaku = {
          ...poolDanmaku,
          id: Date.now().toString() + Math.random(),
          top: Math.floor(Math.random() * 80) + 5
        };
        activeDanmakus.value.push(newDanmaku);
        
        setTimeout(() => {
          activeDanmakus.value = activeDanmakus.value.filter(d => d.id !== newDanmaku.id);
        }, 8000);
      }
    }
  }, 2000);
};

onMounted(async () => {
  await fetchVideoData();
  startDanmaku();
});

onUnmounted(() => {
  if (danmakuTimer) clearInterval(danmakuTimer);
});

const togglePlay = () => {
  isPlaying.value = !isPlaying.value;
};

const sendDanmaku = () => {
  if (!danmakuInput.value.trim()) return;
  
  const newDanmaku: Danmaku = {
    id: Date.now().toString(),
    text: danmakuInput.value,
    time: 0,
    color: '#ff9800',
    top: Math.floor(Math.random() * 80) + 5
  };
  
  activeDanmakus.value.push(newDanmaku);
  danmakuInput.value = '';
  
  setTimeout(() => {
    activeDanmakus.value = activeDanmakus.value.filter(d => d.id !== newDanmaku.id);
  }, 8000);
};

const toggleLike = () => {
  isLiked.value = !isLiked.value;
};

const toggleFavorite = () => {
  isFavorited.value = !isFavorited.value;
};
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 py-6 grid grid-cols-1 lg:grid-cols-3 gap-8">
    <!-- Left Column: Video Player & Info -->
    <div class="lg:col-span-2 flex flex-col gap-6">
      <!-- Video Player Container -->
      <div class="relative aspect-video bg-black rounded-2xl overflow-hidden group shadow-2xl">
        <!-- Video Placeholder -->
        <div class="absolute inset-0 flex items-center justify-center">
          <img 
            src="https://picsum.photos/seed/video/1280/720" 
            class="w-full h-full object-cover opacity-50"
            referrerpolicy="no-referrer"
          />
          <button 
            v-if="!isPlaying"
            @click="togglePlay"
            class="w-20 h-20 bg-orange-500/90 rounded-full flex items-center justify-center text-white shadow-xl hover:scale-110 transition-transform z-20"
          >
            <Play class="w-10 h-10 fill-current" />
          </button>
        </div>

        <!-- Danmaku Layer -->
        <div v-if="showDanmaku" class="absolute inset-0 pointer-events-none z-10 overflow-hidden">
          <div 
            v-for="danmaku in activeDanmakus" 
            :key="danmaku.id"
            class="danmaku-item absolute whitespace-nowrap font-bold text-lg drop-shadow-md"
            :style="{ 
              top: danmaku.top + '%', 
              color: danmaku.color,
              right: '-100%'
            }"
          >
            {{ danmaku.text }}
          </div>
        </div>

        <!-- Player Controls -->
        <div class="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/80 to-transparent opacity-0 group-hover:opacity-100 transition-opacity z-20">
          <div class="flex items-center gap-4">
            <button @click="togglePlay" class="text-white hover:text-orange-500 transition-colors">
              <Pause v-if="isPlaying" class="w-6 h-6" />
              <Play v-else class="w-6 h-6" />
            </button>
            <div class="flex-1 h-1 bg-zinc-600 rounded-full cursor-pointer overflow-hidden">
              <div class="h-full bg-orange-500 w-1/3"></div>
            </div>
            <div class="text-xs text-white font-mono">03:45 / 10:20</div>
            <Volume2 class="w-5 h-5 text-white" />
            <Maximize class="w-5 h-5 text-white" />
          </div>
        </div>
      </div>

      <!-- Danmaku Input -->
      <div class="flex items-center gap-4 bg-zinc-900 p-3 rounded-xl border border-zinc-800">
        <div class="flex items-center gap-2 text-xs text-zinc-400">
          <span class="w-2 h-2 bg-green-500 rounded-full"></span>
          {{ activeDanmakus.length }} 人正在看
        </div>
        <div class="flex-1 flex items-center bg-zinc-800 rounded-lg px-4 py-2 border border-zinc-700 focus-within:border-orange-500 transition-colors">
          <input 
            v-model="danmakuInput"
            @keyup.enter="sendDanmaku"
            type="text" 
            placeholder="发个弹幕见证当下..." 
            class="bg-transparent border-none outline-none w-full text-sm text-zinc-100"
          />
          <button @click="sendDanmaku" class="text-orange-500 hover:text-orange-400">
            <Send class="w-4 h-4" />
          </button>
        </div>
        <button 
          @click="showDanmaku = !showDanmaku"
          class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
          :class="showDanmaku ? 'bg-orange-500/10 text-orange-500' : 'bg-zinc-800 text-zinc-400'"
        >
          弹幕
        </button>
      </div>

      <!-- Video Info -->
      <div v-if="video" class="flex flex-col gap-4">
        <h1 class="text-2xl font-bold text-zinc-100">
          {{ video.title }}
        </h1>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-orange-500 flex items-center justify-center font-bold text-lg">
              {{ video.author[0] }}
            </div>
            <div>
              <div class="font-bold text-zinc-100">{{ video.author }}</div>
              <div class="text-xs text-zinc-400">12.5万 粉丝</div>
            </div>
            <button class="ml-4 bg-orange-500 hover:bg-orange-600 text-white px-6 py-2 rounded-full text-sm font-bold transition-colors">
              关注
            </button>
          </div>
          <div class="flex items-center gap-2">
            <button 
              @click="toggleLike"
              class="flex items-center gap-2 px-4 py-2 rounded-full bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 transition-colors"
              :class="{ 'text-orange-500 border-orange-500/50': isLiked }"
            >
              <ThumbsUp class="w-5 h-5" :class="{ 'fill-current': isLiked }" />
              <span class="text-sm font-medium">{{ (video.likes / 1000).toFixed(1) }}万</span>
            </button>
            <button 
              @click="toggleFavorite"
              class="flex items-center gap-2 px-4 py-2 rounded-full bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 transition-colors"
              :class="{ 'text-yellow-500 border-yellow-500/50': isFavorited }"
            >
              <Star class="w-5 h-5" :class="{ 'fill-current': isFavorited }" />
              <span class="text-sm font-medium">收藏</span>
            </button>
            <button class="p-2 rounded-full bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 transition-colors">
              <Share2 class="w-5 h-5" />
            </button>
          </div>
        </div>
        <div class="bg-zinc-900/50 p-4 rounded-xl text-sm text-zinc-400 leading-relaxed">
          {{ video.description || '暂无简介' }}
          <div class="mt-4 flex gap-2">
            <span v-for="tag in (video.tags || ['#视频', '#精彩'])" :key="tag" class="px-2 py-1 bg-zinc-800 rounded text-xs text-zinc-300">
              {{ tag }}
            </span>
          </div>
        </div>
      </div>

      <!-- Comments Section -->
      <div class="mt-8">
        <div class="flex items-center gap-4 mb-6">
          <h3 class="text-xl font-bold text-zinc-100">评论 (128)</h3>
          <div class="flex gap-4 text-sm text-zinc-400">
            <button class="text-zinc-100 font-bold">按热度</button>
            <button>按时间</button>
          </div>
        </div>

        <!-- Comment Input -->
        <div class="flex gap-4 mb-8">
          <div class="w-10 h-10 rounded-full bg-zinc-800 flex-shrink-0 flex items-center justify-center font-bold">
            {{ auth.user?.username?.[0] || 'U' }}
          </div>
          <div class="flex-1 flex flex-col gap-2">
            <textarea 
              v-model="commentInput"
              placeholder="发一条友善的评论吧..."
              class="w-full bg-zinc-900 border border-zinc-800 rounded-xl p-4 text-sm text-zinc-100 outline-none focus:border-orange-500 transition-colors min-h-[100px] resize-none"
            ></textarea>
            <div class="flex justify-end">
              <button class="bg-orange-500 hover:bg-orange-600 text-white px-6 py-2 rounded-lg text-sm font-bold transition-colors">
                发表评论
              </button>
            </div>
          </div>
        </div>

        <!-- Comment List -->
        <div class="flex flex-col gap-8">
          <div v-for="i in 3" :key="i" class="flex gap-4">
            <div class="w-10 h-10 rounded-full bg-zinc-800 flex-shrink-0 flex items-center justify-center font-bold">
              {{ ['A', 'B', 'C'][i-1] }}
            </div>
            <div class="flex-1 flex flex-col gap-2">
              <div class="flex items-center gap-2">
                <span class="font-bold text-zinc-100 text-sm">用户_{{ i }}</span>
                <span class="text-xs text-zinc-500">1小时前</span>
              </div>
              <p class="text-sm text-zinc-300 leading-relaxed">
                讲得太好了！特别是关于 Composition API 的部分，解决了我在项目中遇到的很多困惑。期待下一期视频！
              </p>
              <div class="flex items-center gap-4 mt-2">
                <button class="flex items-center gap-1 text-xs text-zinc-500 hover:text-orange-500 transition-colors">
                  <ThumbsUp class="w-4 h-4" /> 128
                </button>
                <button class="text-xs text-zinc-500 hover:text-zinc-100 transition-colors">回复</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Column: Related Videos -->
    <div class="flex flex-col gap-6">
      <h3 class="font-bold text-zinc-100">相关推荐</h3>
      <div v-for="i in 6" :key="i" class="group flex gap-3 cursor-pointer">
        <div class="relative w-40 aspect-video rounded-lg overflow-hidden bg-zinc-900 flex-shrink-0">
          <img 
            :src="`https://picsum.photos/seed/related${i}/320/180`" 
            class="w-full h-full object-cover group-hover:scale-105 transition-transform"
            referrerpolicy="no-referrer"
          />
          <div class="absolute bottom-1 right-1 bg-black/70 text-[10px] px-1 rounded text-white">12:30</div>
        </div>
        <div class="flex flex-col gap-1 overflow-hidden">
          <h4 class="text-sm font-medium text-zinc-100 line-clamp-2 group-hover:text-orange-500 transition-colors">
            进阶实战：使用 Pinia 管理大型 Vue 应用的状态
          </h4>
          <div class="text-xs text-zinc-500">前端极客</div>
          <div class="text-[10px] text-zinc-600">4.5万次播放 · 2天前</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.danmaku-item {
  animation: danmaku-move 8s linear forwards;
}

@keyframes danmaku-move {
  from {
    transform: translateX(100vw);
  }
  to {
    transform: translateX(-100%);
  }
}
</style>
