const API_BASE = '/api';

interface RequestOptions extends RequestInit {
  needAuth?: boolean;
  skipErrorRedirect?: boolean;
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { needAuth = false, headers: customHeaders, skipErrorRedirect = false, ...restOptions } = options;
  
  const headers: HeadersInit = {
    ...customHeaders,
  };

  if (!(restOptions.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  try {
    const response = await fetch(`${API_BASE}${url}`, {
      ...restOptions,
      headers,
      credentials: 'include',
    });

    if (!response.ok) {
      if (response.status === 401 && !skipErrorRedirect) {
        window.location.href = '/login';
      }
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    
    if (result.code !== 200) {
      throw new Error(result.message || '请求失败');
    }
    
    return result.data;
  } catch (error) {
    console.error('Request error:', error);
    throw error;
  }
}

export const api = {
  get: <T>(url: string, options?: RequestOptions) => 
    request<T>(url, { ...options, method: 'GET' }),
  
  post: <T>(url: string, data?: any, options?: RequestOptions) => 
    request<T>(url, { 
      ...options, 
      method: 'POST',
      body: data instanceof FormData ? data : JSON.stringify(data),
    }),
  
  put: <T>(url: string, data?: any, options?: RequestOptions) => 
    request<T>(url, { 
      ...options, 
      method: 'PUT',
      body: data instanceof FormData ? data : JSON.stringify(data),
    }),
  
  delete: <T>(url: string, options?: RequestOptions) => 
    request<T>(url, { ...options, method: 'DELETE' }),
};
