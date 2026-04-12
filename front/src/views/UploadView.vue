<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { Upload, X, Image as ImageIcon, CheckCircle, AlertCircle } from 'lucide-vue-next';

const router = useRouter();
const step = ref(1);
const isUploading = ref(false);
const progress = ref(0);

const title = ref('');
const description = ref('');
const tags = ref('');
const videoFile = ref<File | null>(null);
const thumbnailFile = ref<File | null>(null);

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    videoFile.value = target.files[0];
    step.value = 2;
  }
};

const startUpload = () => {
  isUploading.value = true;
  const interval = setInterval(() => {
    progress.value += 5;
    if (progress.value >= 100) {
      clearInterval(interval);
      isUploading.value = false;
      step.value = 3;
    }
  }, 100);
};

const reset = () => {
  step.value = 1;
  videoFile.value = null;
  thumbnailFile.value = null;
  title.value = '';
  description.value = '';
  tags.value = '';
  progress.value = 0;
};
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-12">
    <div class="bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden shadow-2xl">
      <!-- Header -->
      <div class="px-8 py-6 border-b border-zinc-800 flex items-center justify-between">
        <h1 class="text-xl font-bold text-zinc-100">上传视频</h1>
        <button @click="router.back()" class="p-2 hover:bg-zinc-800 rounded-full transition-colors">
          <X class="w-5 h-5 text-zinc-400" />
        </button>
      </div>

      <!-- Step 1: Select File -->
      <div v-if="step === 1" class="p-12">
        <div 
          class="border-2 border-dashed border-zinc-700 rounded-2xl p-16 flex flex-col items-center justify-center gap-6 hover:border-orange-500 hover:bg-orange-500/5 transition-all cursor-pointer group"
          @click="$refs.fileInput.click()"
        >
          <input 
            ref="fileInput"
            type="file" 
            accept="video/*" 
            class="hidden" 
            @change="handleFileSelect"
          />
          <div class="w-20 h-20 bg-zinc-800 rounded-full flex items-center justify-center group-hover:bg-orange-500 transition-colors">
            <Upload class="w-10 h-10 text-zinc-400 group-hover:text-white" />
          </div>
          <div class="text-center">
            <p class="text-lg font-bold text-zinc-100">拖拽视频文件到这里</p>
            <p class="text-sm text-zinc-500 mt-2">或者点击此处选择文件</p>
          </div>
          <div class="flex gap-4 text-xs text-zinc-500 mt-4">
            <span>支持 MP4, WebM, MOV</span>
            <span>最大 500MB</span>
          </div>
        </div>
      </div>

      <!-- Step 2: Details -->
      <div v-if="step === 2" class="p-8 grid grid-cols-1 md:grid-cols-3 gap-8">
        <div class="md:col-span-2 flex flex-col gap-6">
          <div class="flex flex-col gap-2">
            <label class="text-sm font-bold text-zinc-400">标题 (必填)</label>
            <input 
              v-model="title"
              type="text" 
              placeholder="给你的视频起个吸引人的标题" 
              class="bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 text-zinc-100 outline-none focus:border-orange-500 transition-colors"
            />
          </div>
          <div class="flex flex-col gap-2">
            <label class="text-sm font-bold text-zinc-400">简介</label>
            <textarea 
              v-model="description"
              placeholder="告诉观众关于视频的更多信息" 
              class="bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 text-zinc-100 outline-none focus:border-orange-500 transition-colors min-h-[150px] resize-none"
            ></textarea>
          </div>
          <div class="flex flex-col gap-2">
            <label class="text-sm font-bold text-zinc-400">标签 (用逗号分隔)</label>
            <input 
              v-model="tags"
              type="text" 
              placeholder="例如: 生活, 科技, VLOG" 
              class="bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 text-zinc-100 outline-none focus:border-orange-500 transition-colors"
            />
          </div>
        </div>

        <div class="flex flex-col gap-6">
          <div class="flex flex-col gap-2">
            <label class="text-sm font-bold text-zinc-400">封面图</label>
            <div class="aspect-video bg-zinc-800 rounded-xl border border-zinc-700 flex flex-col items-center justify-center gap-2 cursor-pointer hover:bg-zinc-700 transition-colors overflow-hidden relative">
              <template v-if="!thumbnailFile">
                <ImageIcon class="w-8 h-8 text-zinc-500" />
                <span class="text-xs text-zinc-500">上传封面</span>
              </template>
              <img v-else src="https://picsum.photos/seed/thumb/320/180" class="w-full h-full object-cover" />
            </div>
          </div>

          <div class="bg-zinc-800/50 p-4 rounded-xl border border-zinc-700">
            <div class="flex items-center gap-3 mb-4">
              <div class="p-2 bg-orange-500/10 rounded-lg">
                <Upload class="w-4 h-4 text-orange-500" />
              </div>
              <div class="overflow-hidden">
                <p class="text-xs font-bold text-zinc-100 truncate">{{ videoFile?.name }}</p>
                <p class="text-[10px] text-zinc-500">准备就绪</p>
              </div>
            </div>
            
            <div v-if="isUploading" class="flex flex-col gap-2">
              <div class="flex justify-between text-[10px] text-zinc-400">
                <span>正在上传...</span>
                <span>{{ progress }}%</span>
              </div>
              <div class="h-1 bg-zinc-700 rounded-full overflow-hidden">
                <div class="h-full bg-orange-500 transition-all duration-300" :style="{ width: progress + '%' }"></div>
              </div>
            </div>

            <button 
              v-if="!isUploading"
              @click="startUpload"
              class="w-full bg-orange-500 hover:bg-orange-600 text-white font-bold py-3 rounded-xl text-sm transition-colors mt-2"
            >
              发布视频
            </button>
          </div>
        </div>
      </div>

      <!-- Step 3: Success -->
      <div v-if="step === 3" class="p-20 flex flex-col items-center justify-center gap-6">
        <div class="w-24 h-24 bg-green-500/10 rounded-full flex items-center justify-center mb-4">
          <CheckCircle class="w-16 h-16 text-green-500" />
        </div>
        <div class="text-center">
          <h2 class="text-2xl font-bold text-zinc-100">视频上传成功！</h2>
          <p class="text-zinc-400 mt-2">您的视频正在后台处理中，很快就会与观众见面。</p>
        </div>
        <div class="flex gap-4 mt-4">
          <button @click="router.push('/')" class="px-8 py-3 bg-zinc-800 hover:bg-zinc-700 text-zinc-100 font-bold rounded-xl transition-colors">
            回到首页
          </button>
          <button @click="reset" class="px-8 py-3 bg-orange-500 hover:bg-orange-600 text-white font-bold rounded-xl transition-colors">
            继续上传
          </button>
        </div>
      </div>
    </div>

    <!-- Guidelines -->
    <div class="mt-8 grid grid-cols-1 md:grid-cols-2 gap-6">
      <div class="bg-zinc-900/50 p-6 rounded-2xl border border-zinc-800 flex gap-4">
        <AlertCircle class="w-6 h-6 text-orange-500 flex-shrink-0" />
        <div>
          <h4 class="font-bold text-zinc-100 text-sm">版权提示</h4>
          <p class="text-xs text-zinc-500 mt-1 leading-relaxed">
            请确保您拥有该视频的版权或已获得合法授权。上传侵权内容可能导致账号被封禁。
          </p>
        </div>
      </div>
      <div class="bg-zinc-900/50 p-6 rounded-2xl border border-zinc-800 flex gap-4">
        <CheckCircle class="w-6 h-6 text-green-500 flex-shrink-0" />
        <div>
          <h4 class="font-bold text-zinc-100 text-sm">推荐格式</h4>
          <p class="text-xs text-zinc-500 mt-1 leading-relaxed">
            为了获得最佳观看体验，建议上传 1080p 或更高分辨率的 H.264 编码 MP4 视频。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
