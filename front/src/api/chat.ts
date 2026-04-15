import { api } from '@/utils/api';

export interface ChatSession {
  id: number;
  partnerId: number;
  partnerName?: string;
  partnerAvatar?: string;
  lastMessageContent: string;
  lastMessageTime: string;
  unreadCount: number;
}

export interface ChatMessage {
  id: number;
  senderId: number;
  receiverId: number;
  content: string;
  messageType: number;
  status: number;
  aiStatus?: number;
  uuid?: string;
  createdAt: string;
}

export interface CreateSessionRequest {
  userId: number;
  receiverId: number;
}

export interface MarkReadRequest {
  userId: number;
  partnerId: number;
}

export const getChatSessions = (userId: number) => {
  return api.get<ChatSession[]>(`/chat/sessions/${userId}`);
};

export const getChatHistory = (userId: number, partnerId: number) => {
  return api.get<ChatMessage[]>(`/chat/history/${userId}/${partnerId}`);
};

export const createSession = (data: CreateSessionRequest) => {
  return api.post<number>('/chat/session/create', data);
};

export const markAsRead = (data: MarkReadRequest) => {
  return api.post<boolean>('/chat/read', data);
};

export const getFriends = () => {
  return api.get<any[]>('/social/friends');
};
