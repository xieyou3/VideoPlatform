import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { chatWS } from '@/utils/websocket';
import { getChatSessions, getChatHistory, markAsRead, type ChatSession, type ChatMessage } from '@/api/chat';
import { useAuthStore } from './auth';

export const useChatStore = defineStore('chat', () => {
    const authStore = useAuthStore();

    const sessions = ref<ChatSession[]>([]);
    const currentSession = ref<ChatSession | null>(null);
    const messages = ref<ChatMessage[]>([]);
    const loading = ref(false);
    const unreadTotal = computed(() => sessions.value.reduce((sum, s) => sum + s.unreadCount, 0));

    const streamingMessages = ref<Map<string, ChatMessage>>(new Map());

    async function initChat() {
        if (!authStore.user) return;

        await loadSessions();

        if (sessions.value.length > 0 && !currentSession.value) {
            const aiSession = sessions.value.find(s => s.partnerId === 0);
            if (aiSession) {
                await selectSession(aiSession);
            } else {
                await selectSession(sessions.value[0]);
            }
        }

        chatWS.connect(authStore.user.id);

        registerMessageHandlers();
    }

    function registerMessageHandlers() {
        chatWS.onMessageType('message', async (data) => {
            console.log('收到私聊消息:', data);

            if (currentSession.value?.partnerId === parseInt(data.senderId)) {
                await markAsRead({
                    userId: parseInt(authStore.user!.id),
                    partnerId: parseInt(data.senderId)
                });

                messages.value.push({
                    id: Date.now(),
                    senderId: parseInt(data.senderId),
                    receiverId: parseInt(authStore.user!.id),
                    content: data.content,
                    messageType: 0,
                    status: 1,
                    createdAt: new Date().toISOString()
                });
            } else {
                await loadSessions();
            }
        });

        chatWS.onMessageType('bigmodel', (data) => {
            console.log('收到 AI 文本消息:', data);

            if (data.status === 0) {
                const uuid = Date.now().toString();
                const aiMessage: ChatMessage = {
                    id: 0,
                    senderId: 0,
                    receiverId: parseInt(authStore.user!.id),
                    content: data.content,
                    messageType: 2,
                    status: 1,
                    aiStatus: 0,
                    uuid: uuid,
                    createdAt: new Date().toISOString()
                };
                streamingMessages.value.set(uuid, aiMessage);
                messages.value.push(aiMessage);
            } else if (data.status === 1) {
                const lastMessage = messages.value[messages.value.length - 1];
                if (lastMessage && lastMessage.aiStatus === 0) {
                    lastMessage.content += data.content;
                }
            } else if (data.status === 2) {
                const lastMessage = messages.value[messages.value.length - 1];
                if (lastMessage && lastMessage.aiStatus === 0) {
                    lastMessage.content += data.content;
                    lastMessage.aiStatus = 2;
                }
            }
        });

        chatWS.onMessageType('image', (data) => {
            console.log('收到 AI 图片消息:', data);

            let imageContent = data.imageUrl || data.content;
            
            console.log('原始图片数据:', {
                hasImageUrl: !!data.imageUrl,
                hasContent: !!data.content,
                length: imageContent?.length,
                startsWith: imageContent?.substring(0, 50)
            });
            
            if (imageContent) {
                const isBase64 = imageContent.startsWith('data:image/');
                const isUrl = imageContent.startsWith('http://') || imageContent.startsWith('https://');
                
                console.log('图片类型判断:', {
                    isBase64,
                    isUrl,
                    needAddPrefix: !isBase64 && !isUrl
                });
                
                if (!isBase64 && !isUrl) {
                    imageContent = `data:image/png;base64,${imageContent}`;
                    console.log('已添加 Base64 前缀，新长度:', imageContent.length);
                }
            }

            const aiMessage: ChatMessage = {
                id: Date.now(),
                senderId: 0,
                receiverId: parseInt(authStore.user!.id),
                content: imageContent,
                messageType: 2,
                status: 1,
                createdAt: new Date().toISOString()
            };
            
            console.log('创建的图片消息:', {
                id: aiMessage.id,
                messageType: aiMessage.messageType,
                contentLength: aiMessage.content.length,
                contentPreview: aiMessage.content.substring(0, 100)
            });
            
            messages.value.push(aiMessage);
        });

        chatWS.onMessageType('ppt', (data) => {
            console.log('收到 AI PPT 消息:', data);

            if (data.error) {
                console.error('PPT 生成错误:', data.message);
                const errorMessage: ChatMessage = {
                    id: Date.now(),
                    senderId: 0,
                    receiverId: parseInt(authStore.user!.id),
                    content: 'PPT 生成失败：' + data.message,
                    messageType: 3,
                    status: 1,
                    createdAt: new Date().toISOString()
                };
                messages.value.push(errorMessage);
                return;
            }

            const pptSid = data.pptSid;
            const title = data.title || '未命名PPT';
            const coverImg = data.coverImg;
            
            if (!pptSid) {
                console.error('PPT SID 为空');
                return;
            }

            console.log('PPT 生成成功:', {
                sid: pptSid,
                title: title,
                coverImg: coverImg
            });

            const pptData = {
                sid: pptSid,
                title: title,
                coverImg: coverImg
            };

            const aiMessage: ChatMessage = {
                id: Date.now(),
                senderId: 0,
                receiverId: parseInt(authStore.user!.id),
                content: JSON.stringify(pptData),
                messageType: 3,
                status: 1,
                createdAt: new Date().toISOString()
            };
            
            console.log('创建的 PPT 消息:', {
                id: aiMessage.id,
                messageType: aiMessage.messageType,
                content: aiMessage.content
            });
            
            messages.value.push(aiMessage);
        });
    }

    async function loadSessions() {
        if (!authStore.user) return;
        
        try {
          loading.value = true;
          const allSessions = await getChatSessions(parseInt(authStore.user.id));
          
          const aiSession = allSessions.find(s => s.partnerId === 0);
          const otherSessions = allSessions.filter(s => s.partnerId !== 0);
          
          if (aiSession) {
            sessions.value = [aiSession, ...otherSessions];
          } else {
            sessions.value = allSessions;
          }
        } catch (error) {
          console.error('加载会话列表失败:', error);
        } finally {
          loading.value = false;
        }
    }

    async function selectSession(session: ChatSession) {
        currentSession.value = session;
        await loadMessages(session.partnerId);

        if (session.unreadCount > 0 && authStore.user) {
            await markAsRead({
                userId: parseInt(authStore.user.id),
                partnerId: session.partnerId
            });

            session.unreadCount = 0;
        }
    }

    async function loadMessages(partnerId: number) {
        if (!authStore.user) return;

        try {
            loading.value = true;
            messages.value = await getChatHistory(
                parseInt(authStore.user.id),
                partnerId
            );
        } catch (error) {
            console.error('加载消息失败:', error);
        } finally {
            loading.value = false;
        }
    }

    function sendMessage(content: string) {
        if (!currentSession.value || !authStore.user) return;

        const partnerId = currentSession.value.partnerId;

        if (partnerId === 0) {
            chatWS.sendAIMessage(content);

            messages.value.push({
                id: Date.now(),
                senderId: parseInt(authStore.user.id),
                receiverId: 0,
                content: content,
                messageType: 0,
                status: 1,
                createdAt: new Date().toISOString()
            });
        } else {
            chatWS.sendMessage(partnerId.toString(), content);

            messages.value.push({
                id: Date.now(),
                senderId: parseInt(authStore.user.id),
                receiverId: partnerId,
                content: content,
                messageType: 0,
                status: 1,
                createdAt: new Date().toISOString()
            });
        }

        if (currentSession.value) {
            currentSession.value.lastMessageContent = content;
            currentSession.value.lastMessageTime = new Date().toISOString();
        }
    }

    function sendAIMessage(content: string, type: 'text' | 'image' | 'ppt' = 'text') {
        if (!currentSession.value || !authStore.user) return;

        if (type === 'text') {
            chatWS.sendAIMessage(content);
        } else if (type === 'image') {
            chatWS.send({
                type: 'image',
                description: content,
                userId: authStore.user.id
            });
        } else if (type === 'ppt') {
            chatWS.send({
                type: 'ppt',
                description: content,
                userId: authStore.user.id
            });
        }

        messages.value.push({
            id: Date.now(),
            senderId: parseInt(authStore.user.id),
            receiverId: 0,
            content: content,
            messageType: 0,
            status: 1,
            createdAt: new Date().toISOString()
        });

        if (currentSession.value) {
            currentSession.value.lastMessageContent = content;
            currentSession.value.lastMessageTime = new Date().toISOString();
        }
    }

    function clearCurrentSession() {
        currentSession.value = null;
        messages.value = [];
    }

    function disconnect() {
        chatWS.disconnect();
    }

    return {
        sessions,
        currentSession,
        messages,
        loading,
        unreadTotal,
        streamingMessages,
        initChat,
        loadSessions,
        selectSession,
        sendMessage,
        sendAIMessage,
        clearCurrentSession,
        disconnect
    };
});
