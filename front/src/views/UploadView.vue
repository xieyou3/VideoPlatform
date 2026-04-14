<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { Upload, X, Image as ImageIcon, CheckCircle, AlertCircle, FileVideo } from 'lucide-vue-next';
import * as SparkMD5 from 'spark-md5';
import { uploadApi, type ChunkUploadResponse } from '@/utils/upload';

const router = useRouter();

const step = ref(1);
const isUploading = ref(false);
const isSubmitting = ref(false);
const uploadProgress = ref(0);
const uploadedCount = ref(0);
const totalChunks = ref(0);

const title = ref('');
const description = ref('');
const tags = ref('');
const videoFile = ref<File | null>(null);
const thumbnailFile = ref<File | null>(null);
const thumbnailUrl = ref('');
const fileHash = ref('');
const videoUrl = ref('');
const durationSeconds = ref(0);
const fileSize = ref(0);

const fileInput = ref<HTMLInputElement | null>(null);
const thumbnailInput = ref<HTMLInputElement | null>(null);

const CHUNK_SIZE = 5 * 1024 * 1024;
const MAX_CONCURRENT = 4;

const calculateMD5 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer();
    const reader = new FileReader();
    const chunks = Math.ceil(file.size / CHUNK_SIZE);
    let currentChunkIndex = 0;

    const loadNext = () => {
      const start = currentChunkIndex * CHUNK_SIZE;
      const end = Math.min(start + CHUNK_SIZE, file.size);

      reader.readAsArrayBuffer(file.slice(start, end));
    };

    reader.onload = (e) => {
      if (e.target?.result) {
        spark.append(e.target.result as ArrayBuffer);
        currentChunkIndex++;

        if (currentChunkIndex < chunks) {
          loadNext();
        } else {
          resolve(spark.end());
        }
      }
    };

    reader.onerror = () => reject(new Error('MD5 计算失败'));
    loadNext();
  });
};

const handleFileSelect = async (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    videoFile.value = target.files[0];
    fileSize.value = videoFile.value.size;

    isUploading.value = true;
    step.value = 2;

    try {
      fileHash.value = await calculateMD5(videoFile.value);
      console.log('文件 MD5:', fileHash.value);

      const checkResult = await uploadApi.checkMd5(fileHash.value);

      if (checkResult.exists) {
        videoUrl.value = checkResult.videoUrl!;
        durationSeconds.value = checkResult.durationSeconds || 0;
        isUploading.value = false;
        step.value = 3;
        console.log('秒传成功');
      } else {
        await uploadChunks();
      }
    } catch (error) {
      console.error('文件处理失败:', error);
      alert('文件处理失败，请重试');
      reset();
    }
  }
};

const triggerFileInput = () => {
  fileInput.value?.click();
};

const triggerThumbnailInput = () => {
  thumbnailInput.value?.click();
};

const handleThumbnailSelect = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    const file = target.files[0];

    if (!file.type.startsWith('image/')) {
      alert('请选择图片文件');
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      alert('图片大小不能超过 10MB');
      return;
    }

    thumbnailFile.value = file;

    const reader = new FileReader();
    reader.onload = (e) => {
      if (e.target?.result) {
        thumbnailUrl.value = e.target.result as string;
      }
    };
    reader.readAsDataURL(file);

    console.log('封面图已选择:', file.name);
  }
};

const uploadChunks = async () => {
  if (!videoFile.value) return;

  const file = videoFile.value;
  totalChunks.value = Math.ceil(file.size / CHUNK_SIZE);
  uploadedCount.value = 0;

  const uploadedChunks = new Set<number>();
  const pendingChunks: number[] = [];

  for (let i = 0; i < totalChunks.value; i++) {
    pendingChunks.push(i);
  }

  const uploadSingleChunk = async (chunkIndex: number): Promise<void> => {
    const start = chunkIndex * CHUNK_SIZE;
    const end = Math.min(start + CHUNK_SIZE, file.size);
    const chunk = file.slice(start, end);

    const formData = new FormData();
    formData.append('fileHash', fileHash.value);
    formData.append('fileName', file.name);
    formData.append('chunkIndex', chunkIndex.toString());
    formData.append('totalChunks', totalChunks.value.toString());
    formData.append('file', chunk);

    try {
      const response: ChunkUploadResponse = await uploadApi.uploadChunk(formData);

      uploadedChunks.add(chunkIndex);
      uploadedCount.value = uploadedChunks.size;
      uploadProgress.value = Math.floor((uploadedChunks.size / totalChunks.value) * 100);

      if (response.merged && response.videoUrl) {
        videoUrl.value = response.videoUrl;
        durationSeconds.value = 0;
        console.log('视频合并完成，URL:', response.videoUrl);
      }
    } catch (error) {
      console.error(`分片 ${chunkIndex} 上传失败:`, error);
      throw error;
    }
  };

  try {
    const chunkBatches = [];
    for (let i = 0; i < pendingChunks.length; i += MAX_CONCURRENT) {
      const batch = pendingChunks.slice(i, i + MAX_CONCURRENT);
      chunkBatches.push(batch);
    }

    for (const batch of chunkBatches) {
      await Promise.all(batch.map(index => uploadSingleChunk(index)));
    }

    isUploading.value = false;
    step.value = 3;
    console.log('所有分片上传完成');
  } catch (error) {
    console.error('上传失败:', error);
    alert('上传失败，请重试');
    reset();
  }
};

const submitVideo = async () => {
  if (!title.value.trim()) {
    alert('请输入视频标题');
    return;
  }

  if (!videoUrl.value) {
    alert('视频上传未完成');
    return;
  }

  isSubmitting.value = true;

  try {
    const tagList = tags.value
        .split(',')
        .map(t => t.trim())
        .filter(t => t.length > 0);

    let coverUrl = '';

    if (thumbnailFile.value) {
      const formData = new FormData();
      formData.append('file', thumbnailFile.value);

      try {
        const response = await fetch('/api/videos/upload/cover', {
          method: 'POST',
          body: formData,
          credentials: 'include',
        });

        if (response.ok) {
          const result = await response.json();
          if (result.code === 200) {
            coverUrl = result.data.coverUrl;
            console.log('封面图上传成功:', coverUrl);
          }
        }
      } catch (error) {
        console.error('封面图上传失败，将使用空值:', error);
      }
    }

    const videoData = {
      fileHash: fileHash.value,
      title: title.value,
      description: description.value,
      tags: tagList,
      coverUrl: coverUrl || undefined,
      videoUrl: videoUrl.value,
      durationSeconds: durationSeconds.value,
      fileSize: fileSize.value,
    };

    console.log('提交视频信息:', videoData);

    await uploadApi.publishVideo(videoData);

    console.log('视频发布成功');
    isSubmitting.value = false;
    step.value = 4;
  } catch (error) {
    console.error('提交失败:', error);
    alert('提交失败，请重试');
    isSubmitting.value = false;
  }
};

const cancelUpload = () => {
  if (confirm('确定要取消上传吗？')) {
    reset();
  }
};

const reset = () => {
  step.value = 1;
  videoFile.value = null;
  thumbnailFile.value = null;
  thumbnailUrl.value = '';
  fileHash.value = '';
  videoUrl.value = '';
  title.value = '';
  description.value = '';
  tags.value = '';
  uploadProgress.value = 0;
  uploadedCount.value = 0;
  totalChunks.value = 0;
  durationSeconds.value = 0;
  fileSize.value = 0;
  isUploading.value = false;
  isSubmitting.value = false;
  if (fileInput.value) {
    fileInput.value.value = '';
  }
  if (thumbnailInput.value) {
    thumbnailInput.value.value = '';
  }
};

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};
</script>

<template>
  <div class="min-h-screen bg-black px-4 py-8">
    <div class="max-w-4xl mx-auto">
      <div class="bg-zinc-900 border border-zinc-800 rounded-3xl overflow-hidden shadow-2xl">
        <div class="px-8 py-6 border-b border-zinc-800 flex items-center justify-between">
          <h1 class="text-xl font-bold text-zinc-100">上传视频</h1>
          <button @click="router.back()" class="p-2 hover:bg-zinc-800 rounded-full transition-colors">
            <X class="w-5 h-5 text-zinc-400" />
          </button>
        </div>

        <div class="p-8">
          <div v-if="step === 1" class="flex flex-col items-center justify-center py-12">
            <div
              class="border-2 border-dashed border-zinc-700 rounded-2xl p-16 flex flex-col items-center justify-center gap-6 hover:border-orange-500 hover:bg-orange-500/5 transition-all cursor-pointer group w-full max-w-2xl"
              @click="triggerFileInput"
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
                <p class="text-lg font-bold text-zinc-100">点击选择视频文件</p>
                <p class="text-sm text-zinc-500 mt-2">支持 MP4, WebM, MOV 格式</p>
              </div>
            </div>
          </div>

          <div v-if="step === 2" class="flex flex-col items-center justify-center py-12 gap-8">
            <div class="w-full max-w-md">
              <div class="flex items-center gap-4 mb-6">
                <div class="p-3 bg-orange-500/10 rounded-xl">
                  <FileVideo class="w-8 h-8 text-orange-500" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-bold text-zinc-100 truncate">{{ videoFile?.name }}</p>
                  <p class="text-xs text-zinc-500 mt-1">{{ formatFileSize(fileSize) }}</p>
                </div>
              </div>

              <div class="flex flex-col gap-3">
                <div class="flex justify-between text-sm text-zinc-400">
                  <span>{{ isUploading ? '正在上传...' : '处理中...' }}</span>
                  <span>{{ uploadProgress }}%</span>
                </div>
                <div class="h-2 bg-zinc-800 rounded-full overflow-hidden">
                  <div
                    class="h-full bg-gradient-to-r from-orange-500 to-orange-400 transition-all duration-300"
                    :style="{ width: uploadProgress + '%' }"
                  ></div>
                </div>
                <p class="text-xs text-zinc-500 text-center mt-2">
                  已上传 {{ uploadedCount }} / {{ totalChunks }} 个分片
                </p>
              </div>
            </div>
          </div>

          <div v-if="step === 3" class="flex flex-col gap-6">
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
                class="bg-zinc-800 border border-zinc-700 rounded-xl px-4 py-3 text-zinc-100 outline-none focus:border-orange-500 transition-colors min-h-[120px] resize-none"
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

            <div class="flex flex-col gap-2">
              <label class="text-sm font-bold text-zinc-400">封面图</label>
              <div
                class="aspect-video bg-zinc-800 rounded-xl border border-zinc-700 flex flex-col items-center justify-center gap-2 cursor-pointer hover:bg-zinc-700 transition-colors overflow-hidden relative"
                @click="triggerThumbnailInput"
              >
                <input
                  ref="thumbnailInput"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  @change="handleThumbnailSelect"
                />
                <template v-if="!thumbnailUrl">
                  <ImageIcon class="w-8 h-8 text-zinc-500" />
                  <span class="text-xs text-zinc-500">点击上传封面（可选）</span>
                </template>
                <img v-else :src="thumbnailUrl" class="w-full h-full object-cover" />
              </div>
            </div>

            <div class="bg-zinc-800/50 p-4 rounded-xl border border-zinc-700 flex items-center gap-3">
              <div class="p-2 bg-green-500/10 rounded-lg">
                <CheckCircle class="w-4 h-4 text-green-500" />
              </div>
              <div class="flex-1 overflow-hidden">
                <p class="text-xs font-bold text-zinc-100 truncate">{{ videoFile?.name }}</p>
                <p class="text-[10px] text-green-500">视频上传完成</p>
              </div>
            </div>
          </div>

          <div v-if="step === 4" class="flex flex-col items-center justify-center py-12 gap-6">
            <div class="w-24 h-24 bg-green-500/10 rounded-full flex items-center justify-center">
              <CheckCircle class="w-16 h-16 text-green-500" />
            </div>
            <div class="text-center">
              <h2 class="text-2xl font-bold text-zinc-100">视频上传成功！</h2>
              <p class="text-zinc-400 mt-2">您的视频正在后台处理中</p>
            </div>
          </div>
        </div>

        <div class="px-8 py-6 border-t border-zinc-800 flex items-center justify-between">
          <button
            @click="cancelUpload"
            class="px-6 py-3 bg-zinc-800 hover:bg-zinc-700 text-zinc-100 font-bold rounded-xl transition-colors"
          >
            取消
          </button>

          <button
            v-if="step === 3"
            @click="submitVideo"
            :disabled="isSubmitting || !title.trim()"
            class="px-8 py-3 bg-orange-500 hover:bg-orange-600 disabled:bg-zinc-700 disabled:cursor-not-allowed text-white font-bold rounded-xl transition-colors"
          >
            {{ isSubmitting ? '提交中...' : '发布视频' }}
          </button>

          <button
            v-if="step === 4"
            @click="reset"
            class="px-8 py-3 bg-orange-500 hover:bg-orange-600 text-white font-bold rounded-xl transition-colors"
          >
            继续上传
          </button>
        </div>
      </div>

      <div class="mt-8 grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="bg-zinc-900/50 p-6 rounded-2xl border border-zinc-800 flex gap-4">
          <AlertCircle class="w-6 h-6 text-orange-500 flex-shrink-0" />
          <div>
            <h4 class="font-bold text-zinc-100 text-sm">版权提示</h4>
            <p class="text-xs text-zinc-500 mt-1 leading-relaxed">
              请确保您拥有该视频的版权或已获得合法授权。
            </p>
          </div>
        </div>
        <div class="bg-zinc-900/50 p-6 rounded-2xl border border-zinc-800 flex gap-4">
          <CheckCircle class="w-6 h-6 text-green-500 flex-shrink-0" />
          <div>
            <h4 class="font-bold text-zinc-100 text-sm">推荐格式</h4>
            <p class="text-xs text-zinc-500 mt-1 leading-relaxed">
              建议上传 1080p H.264 编码的 MP4 视频。
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
