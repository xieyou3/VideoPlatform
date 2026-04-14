import { api } from './api';

export interface ChunkCheckResponse {
  exists: boolean;
  videoUrl?: string;
  durationSeconds?: number;
  fileSize?: number;
  uploadedChunks?: number;
  totalChunks?: number;
}

export interface ChunkUploadResponse {
  merged: boolean;
  videoUrl?: string;
  uploadedChunks: number;
  totalChunks: number;
}

export interface UploadCompleteData {
  fileHash: string;
  title: string;
  description?: string;
  tags?: string[];
  coverUrl?: string;
  category?: string;
  videoUrl: string;
  durationSeconds: number;
  fileSize: number;
}

export interface VideoPublishResponse {
  id: number;
  authorId: number;
  fileHash: string;
  title: string;
  description?: string;
  coverUrl?: string;
  videoUrl: string;
  durationSeconds: number;
  category?: string;
  status: string;
  reviewStatus: string;
  visibility: string;
  viewCount: number;
  likeCount: number;
  favoriteCount: number;
  commentCount: number;
  danmakuCount: number;
  createdAt: string;
  updatedAt: string;
  tags?: Array<{
    id: number;
    name: string;
  }>;
}

export const uploadApi = {
  checkMd5: (fileHash: string) => 
    api.post<ChunkCheckResponse>(`/videos/upload/check-md5?fileHash=${fileHash}`, {}, { needAuth: true }),
  
  uploadChunk: (formData: FormData) => 
    api.post<ChunkUploadResponse>('/videos/upload/chunk', formData, {
      headers: {},
      needAuth: true,
    }),
  
  publishVideo: (data: UploadCompleteData) => 
    api.post<VideoPublishResponse>('/videos', data, { needAuth: true }),
};
