# VideoCraft AI Agent - 技术文档 (Java/LangChain4j)

## 1. 项目概述
VideoCraft AI 是一个专为视频创作者设计的创作辅助 Agent。它通过 Java 后端集成 LangChain4j 框架，结合视频内容分析、RAG（检索增强生成）技术以及本地知识库，为创作者提供智能的标题和简介优化建议。

## 2. 技术规范
- **后端语言**: Java 17+
- **Agent 框架**: LangChain4j
- **AI 引擎**: Google Gemini 2.0 Flash (多模态支持)
- **向量数据库**: Elasticsearch (ES) - 用于高性能向量检索与全文搜索
- **知识库格式**: Markdown / PDF / JSON
- **前端对接**: RESTful API / WebSocket

## 3. 核心功能划分
### 3.1 视频感知模块 (Video Perception)
- **技术**: Gemini 2.0 Multimodal Input
- **职责**: 分析视频画面，提取主题、风格、关键场景和人物特征。

### 3.2 RAG 检索模块 (Retrieval Module)
- **技术**: LangChain4j ElasticsearchEmbeddingStore + EmbeddingModel
- **职责**: 从 Elasticsearch 索引中检索 SEO 策略、标题模板和行业规范。利用 ES 的混合搜索能力（向量相似度 + 关键词匹配）提高检索精度。

### 3.3 创作辅助 Agent (Creative Agent)
- **技术**: LangChain4j AiServices + Prompt Templates
- **职责**: 整合视频理解、检索知识与用户原始输入，生成结构化的优化建议。

## 4. 实现流程
1. **上传**: 用户上传视频及原始标题/简介。
2. **分析**: 后端调用 Gemini 进行视频内容理解。
3. **检索**: 根据分析结果在向量库中匹配相关创作技巧。
4. **推理**: Agent 结合所有上下文生成 3 组标题及 1 份深度简介。
5. **反馈**: 前端展示建议，用户可进行二次编辑或采纳。

## 5. 模块详细设计
- **Embedding 层**: 使用 Google 的 `embedding-001` 模型，将知识库文档转化为 768 维向量。
- **存储与检索**: 采用 Elasticsearch 作为向量存储引擎。
    - **索引设计**: 包含 `vector` (dense_vector类型), `content` (text类型), `metadata` (object类型)。
    - **检索策略**: 优先进行向量相似度检索，辅以关键词过滤（如：指定视频平台、特定领域）。
- **Prompt 策略**: 包含“角色设定（SEO专家）”、“任务目标”、“约束条件（字数、违禁词）”。
- **数据流**: 采用异步处理模式，防止长连接超时。
