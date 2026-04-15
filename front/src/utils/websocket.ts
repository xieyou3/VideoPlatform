import { useAuthStore } from '@/stores/auth';

interface WSMessage {
    type: string;
    [key: string]: any;
}

type MessageHandler = (data: any) => void;

class ChatWebSocket {
    private ws: WebSocket | null = null;
    private reconnectTimer: number | null = null;
    private messageHandlers: Map<string, MessageHandler[]> = new Map();
    private isManualClose: boolean = false;
    private reconnectAttempts: number = 0;
    private maxReconnectAttempts: number = 10;
    private userId: string | null = null;

    connect(userId: string) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            console.log('WebSocket 已连接，跳过重复连接');
            return;
        }

        if (this.ws && this.ws.readyState === WebSocket.CONNECTING) {
            console.log('WebSocket 正在连接中，等待...');
            return;
        }

        this.userId = userId;
        this.isManualClose = false;

        const wsUrl = `ws://localhost:8083/ws/chat`;

        console.log('正在连接 WebSocket:', wsUrl);
        console.log('当前用户ID:', userId);
        
        try {
            this.ws = new WebSocket(wsUrl);

            this.ws.onopen = () => {
                console.log('✅ WebSocket 连接成功');
                this.reconnectAttempts = 0;

                const initMessage = {
                    type: 'init',
                    userId: userId,
                    sessionId: this.getSessionId()
                };
                console.log('发送初始化消息:', initMessage);
                
                this.send(initMessage);
            };

            this.ws.onmessage = (event) => {
                try {
                    const message = JSON.parse(event.data);
                    console.log('📨 收到 WebSocket 消息:', message);
                    this.handleMessage(message);
                } catch (error) {
                    console.error('❌ 解析 WebSocket 消息失败:', error);
                }
            };

            this.ws.onerror = (error) => {
                console.error('❌ WebSocket 错误:', error);
                console.error('WebSocket readyState:', this.ws?.readyState);
            };

            this.ws.onclose = (event) => {
                console.log('🔴 WebSocket 连接关闭:', {
                    code: event.code,
                    reason: event.reason,
                    wasClean: event.wasClean
                });
                this.ws = null;

                if (!this.isManualClose) {
                    console.log('非主动断开，准备重连...');
                    this.reconnect(userId);
                } else {
                    console.log('用户主动断开，不重连');
                }
            };
        } catch (error) {
            console.error('❌ 创建 WebSocket 连接失败:', error);
        }
    }

    send(message: WSMessage) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
            console.log('发送 WebSocket 消息:', message);
        } else {
            console.warn('WebSocket 未连接，无法发送消息');
        }
    }

    sendMessage(receiverId: string, content: string) {
        this.send({
            type: 'message',
            receiverId,
            content,
            userId: this.userId
        });
    }

    sendAIMessage(question: string) {
        this.send({
            type: 'bigmodel',
            question,
            userId: this.userId
        });
    }

    onMessageType(type: string, handler: MessageHandler) {
        if (!this.messageHandlers.has(type)) {
            this.messageHandlers.set(type, []);
        }
        this.messageHandlers.get(type)?.push(handler);
    }

    offMessageType(type: string, handler?: MessageHandler) {
        if (!handler) {
            this.messageHandlers.delete(type);
        } else {
            const handlers = this.messageHandlers.get(type);
            if (handlers) {
                const index = handlers.indexOf(handler);
                if (index > -1) {
                    handlers.splice(index, 1);
                }
            }
        }
    }

    private handleMessage(message: any) {
        const handlers = this.messageHandlers.get(message.type);
        if (handlers) {
            handlers.forEach(handler => handler(message));
        }
    }

    private reconnect(userId: string) {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('WebSocket 重连次数已达上限');
            return;
        }

        if (this.reconnectTimer) return;

        this.reconnectAttempts++;
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);

        console.log(`将在 ${delay}ms 后尝试第 ${this.reconnectAttempts} 次重连`);

        this.reconnectTimer = window.setTimeout(() => {
            this.reconnectTimer = null;
            this.connect(userId);
        }, delay);
    }

    disconnect() {
        this.isManualClose = true;
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
        if (this.ws) {
            this.ws.close(1000, '用户主动断开');
            this.ws = null;
        }
    }

    private getSessionId(): string {
        let sessionId = sessionStorage.getItem('chat_session_id');
        if (!sessionId) {
            sessionId = Date.now().toString() + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem('chat_session_id', sessionId);
        }
        return sessionId;
    }

    isConnected(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }
}

export const chatWS = new ChatWebSocket();
