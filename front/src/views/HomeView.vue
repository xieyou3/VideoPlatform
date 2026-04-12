<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import { Play, Eye, Clock, Heart } from 'lucide-vue-next';

interface Video {
  id: string;
  title: string;
  thumbnail: string;
  author: string;
  views: number;
  duration: string;
  createdAt: string;
  likes: number;
}

const videos = ref<Video[]>([]);

onMounted(async () => {
  try {
    const response = await fetch('/api/videos');
    const data = await response.json();
    videos.value = data.items;
  } catch (error) {
    console.error('Failed to fetch videos:', error);
  }
});

const formatViews = (views: number) => {
  if (views >= 10000) {
    return (views / 10000).toFixed(1) + '万';
  }
  return views.toString();
};
</script>

<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <div class="flex items-center justify-between mb-8">
      <h2 class="text-2xl font-bold text-zinc-100 flex items-center gap-2">
        <Play class="w-6 h-6 text-orange-500" />
        热门推荐
      </h2>
      <div class="flex gap-2">
        <button class="px-4 py-1.5 bg-zinc-800 rounded-full text-sm hover:bg-zinc-700 transition-colors">全部</button>
        <button class="px-4 py-1.5 bg-zinc-900 rounded-full text-sm hover:bg-zinc-800 transition-colors">音乐</button>
        <button class="px-4 py-1.5 bg-zinc-900 rounded-full text-sm hover:bg-zinc-800 transition-colors">游戏</button>
        <button class="px-4 py-1.5 bg-zinc-900 rounded-full text-sm hover:bg-zinc-800 transition-colors">科技</button>
      </div>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <RouterLink 
        v-for="video in videos" 
        :key="video.id" 
        :to="`/video/${video.id}`"
        class="group flex flex-col gap-3"
      >
        <div class="relative aspect-video rounded-xl overflow-hidden bg-zinc-900">
          <img 
            :src="video.thumbnail" 
            :alt="video.title"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            referrerpolicy="no-referrer"
          />
          <div class="absolute bottom-2 right-2 bg-black/70 text-white text-xs px-1.5 py-0.5 rounded font-mono">
            {{ video.duration }}
          </div>
          <div class="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
            <div class="w-12 h-12 bg-orange-500 rounded-full flex items-center justify-center shadow-lg transform scale-90 group-hover:scale-100 transition-transform">
              <Play class="w-6 h-6 text-white fill-current" />
            </div>
          </div>
        </div>
        
        <div class="flex gap-3">
          <div class="w-10 h-10 rounded-full bg-zinc-800 flex-shrink-0 flex items-center justify-center font-bold text-sm text-zinc-400">
            {{ video.author[0] }}
          </div>
          <div class="flex flex-col gap-1 overflow-hidden">
            <h3 class="font-medium text-zinc-100 line-clamp-2 group-hover:text-orange-500 transition-colors">
              {{ video.title }}
            </h3>
            <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-zinc-400">
              <span class="hover:text-zinc-200 transition-colors">{{ video.author }}</span>
              <div class="flex items-center gap-1">
                <Eye class="w-3 h-3" />
                {{ formatViews(video.views) }}
              </div>
              <div class="flex items-center gap-1">
                <Heart class="w-3 h-3" />
                {{ formatViews(video.likes) }}
              </div>
              <span>{{ video.createdAt }}</span>
            </div>
          </div>
        </div>
      </RouterLink>
    </div>
  </div>
</template>
