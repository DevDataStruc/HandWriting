<template>
  <div class="art-gallery">
    <div class="gallery-shell">
      <!-- 顶部导航 -->
      <nav class="gallery-topbar">
        <h1 class="gallery-heading">我的样本</h1>
        <button class="nav-cta" @click="onUpload">
          <span class="cta-icon" aria-hidden="true"><Icon name="plus" /></span>
          上传作品
        </button>
      </nav>

      <!-- 搜索栏 -->
      <div class="search-dock">
        <div class="search-dock__input-wrap">
          <span class="search-dock__icon" aria-hidden="true"><Icon name="search" /></span>
          <input v-model="keyword" class="search-dock__input" placeholder="搜索作品名称..." />
        </div>
        <span class="search-dock__tag">分类筛选：</span>
        <div class="search-dock__picker-wrap">
          <select v-model="category" class="search-dock__picker">
            <option value="">全部分类</option>
            <option value="landscape">风景画</option>
            <option value="portrait">人物画</option>
            <option value="abstract">抽象画</option>
          </select>
        </div>
        <div class="search-dock__sort-wrap">
          <select v-model="sortBy" class="search-dock__sort">
            <option value="newest">最新发布</option>
            <option value="popular">最受欢迎</option>
            <option value="name">按名称</option>
          </select>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredArtworks.length === 0 && !loading" class="empty-vault">
        <div class="empty-vault__icon" aria-hidden="true"><Icon name="image" /></div>
        <p class="empty-vault__title">暂无作品</p>
        <p class="empty-vault__desc">试试上传你的第一幅作品吧</p>
      </div>

      <!-- 作品网格 -->
      <div v-else class="artwork-mosaic">
        <div
          v-for="item in filteredArtworks"
          :key="item.id"
          class="artwork-tile"
          :class="{ 'is-picked': pickedIds.has(item.id) }"
        >
          <!-- 批量选择 -->
          <label v-if="batchOn" class="artwork-tile__selector">
            <input
              type="checkbox"
              :checked="pickedIds.has(item.id)"
              @change="togglePick(item.id)"
            />
            <span class="selector-mark"></span>
          </label>

          <!-- 图片区域 -->
          <div class="artwork-tile__canvas" @click="openPreview(item)">
            <div class="canvas-art" :style="{ background: item.gradient }">
              <img
                v-if="item.imageUrl"
                class="canvas-img"
                :src="item.imageUrl"
                :alt="item.title"
                loading="lazy"
              />
              <span v-else class="canvas-glyph">{{ item.glyph }}</span>
            </div>
          </div>

          <!-- 标题 + 标签 -->
          <div class="artwork-tile__caption">
            <div class="caption-title">{{ item.title }}</div>
            <div class="caption-badges">
              <span class="badge" :class="'badge--' + item.tone">{{ item.toneLabel }}</span>
              <span v-if="item.featured" class="badge badge--star">精选</span>
            </div>
          </div>

          <!-- 元信息 -->
          <div class="artwork-tile__footnote">
            <span class="footnote-date">{{ item.date }}</span>
            <span class="footnote-views"
              ><Icon name="eye" class="icon-inline" /> {{ item.views }}</span
            >
          </div>

          <!-- 操作按钮 -->
          <div class="artwork-tile__toolbar">
            <button class="tool-btn" title="查看详情" @click.stop="openPreview(item)">
              <Icon name="search" />
            </button>
            <button class="tool-btn" title="编辑" @click.stop="onEdit(item)">
              <Icon name="edit" />
            </button>
            <button class="tool-btn" title="收藏" @click.stop="toggleFav(item)">
              <Icon :name="item.faved ? 'heart-filled' : 'heart'" />
            </button>
            <button class="tool-btn tool-btn--risk" title="删除" @click.stop="confirmRemove(item)">
              <Icon name="trash" />
            </button>
          </div>
        </div>
      </div>

      <!-- 底部分页 -->
      <div class="pager-strip">
        <label class="pager-strip__batch-toggle">
          <input v-model="batchOn" type="checkbox" />
          <span class="toggle-track"></span>
          批量模式
        </label>
        <div v-if="batchOn && pickedIds.size > 0" class="pager-strip__batch-actions">
          <button class="batch-del" @click="confirmRemoveBatch">
            <Icon name="trash" class="icon-inline" />
            删除选中 ({{ pickedIds.size }})
          </button>
        </div>
        <div class="pager-strip__controls">
          <div class="page-size">
            <span class="page-size__label">每页</span>
            <select v-model="pageSize" class="page-size__select">
              <option v-for="n in pageSizeOptions" :key="n" :value="n">{{ n }}</option>
            </select>
            <span class="page-size__label">条</span>
          </div>
          <button class="page-arrow" :disabled="page <= 1" @click="page--">
            <Icon name="chevron-left" />
          </button>
          <span
            v-for="p in totalPages"
            :key="p"
            class="page-dot"
            :class="{ 'page-dot--active': p === page }"
            @click="page = p"
            >{{ p }}</span
          >
          <button class="page-arrow" :disabled="page >= totalPages" @click="page++">
            <Icon name="chevron-right" />
          </button>
        </div>
      </div>

      <!-- 预览弹窗 -->
      <transition name="fade-scale">
        <div v-if="previewItem" class="preview-overlay" @click.self="previewItem = null">
          <div class="preview-modal">
            <button class="preview-modal__close" @click="previewItem = null">
              <Icon name="close" />
            </button>
            <div class="preview-modal__art" :style="{ background: previewItem.gradient }">
              <img
                v-if="previewItem.imageUrl"
                class="preview-img"
                :src="previewItem.imageUrl"
                :alt="previewItem.title"
              />
              <span v-else class="preview-glyph">{{ previewItem.glyph }}</span>
            </div>
            <div class="preview-modal__body">
              <h2>{{ previewItem.title }}</h2>
              <p class="preview-desc">这是一幅{{ previewItem.toneLabel }}风格的作品</p>
              <div class="preview-stats">
                <span><Icon name="eye" class="icon-inline" /> {{ previewItem.views }} 次浏览</span>
                <span><Icon name="calendar" class="icon-inline" /> {{ previewItem.date }}</span>
              </div>
            </div>
          </div>
        </div>
      </transition>

      <!-- 提示消息 -->
      <transition name="fade-slide">
        <div v-if="toastMsg" class="toast-msg">{{ toastMsg }}</div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, h } from 'vue'
import { useRouter } from 'vue-router'
import { useSampleStore } from '@/stores/sample'
import type { SampleVO } from '@/api/contracts/sample'

// ===== 图标组件（SVG 替代 emoji） =====
const ICONS: Record<string, string> = {
  plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M12 5v14"/></svg>',
  search:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>',
  image:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>',
  eye: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>',
  edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>',
  heart:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>',
  'heart-filled':
    '<svg viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>',
  trash:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>',
  calendar:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>',
  close:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>',
  'chevron-left':
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>',
  'chevron-right':
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>',
}

const Icon = (props: { name: string; class?: string }) =>
  h('span', {
    class: `svg-icon ${props.class ?? ''}`,
    innerHTML: ICONS[props.name] ?? '',
    ariaHidden: true,
  })

const router = useRouter()
const sampleStore = useSampleStore()

const keyword = ref('')
const category = ref('')
const sortBy = ref('newest')
const batchOn = ref(false)
const page = ref(1)
const pageSize = ref(8)
const pageSizeOptions = [8, 12, 24, 48]
const previewItem = ref<DisplayItem | null>(null)
const toastMsg = ref('')
const pickedIds = ref(new Set<string | number>())
const loading = computed(() => sampleStore.loading)

interface DisplayItem {
  id: string | number
  title: string
  glyph: string
  gradient: string
  tone: string
  toneLabel: string
  category: string
  featured: boolean
  faved: boolean
  views: number
  date: string
  imageUrl?: string
  raw: SampleVO
}

const statusToneMap: Record<number, { tone: string; label: string }> = {
  0: { tone: 'calm', label: '待审' },
  1: { tone: 'warm', label: '通过' },
  2: { tone: 'vivid', label: '驳回' },
}

const gradients = [
  'linear-gradient(135deg, #0d9488 0%, #5eead4 100%)',
  'linear-gradient(135deg, #6366f1 0%, #a78bfa 100%)',
  'linear-gradient(135deg, #f472b6 0%, #fda4af 100%)',
  'linear-gradient(135deg, #78716c 0%, #d6d3d1 100%)',
  'linear-gradient(135deg, #0ea5e9 0%, #67e8f9 100%)',
  'linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%)',
  'linear-gradient(135deg, #22c55e 0%, #86efac 100%)',
  'linear-gradient(135deg, #8b5cf6 0%, #c4b5fd 100%)',
]

function pickGradient(id: string | number, idx = 0): string {
  const seed = String(id)
    .split('')
    .reduce((a, c) => a + c.charCodeAt(0), 0)
  return gradients[(seed + idx) % gradients.length]
}

function formatDateTime(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function mapToDisplay(raw: SampleVO): DisplayItem {
  const meta = statusToneMap[raw.status] ?? statusToneMap[0]
  const char = raw.char || String(raw.charId || '?')
  return {
    id: raw.id,
    title: raw.remark || `${char} · 手写样本`,
    glyph: char,
    gradient: pickGradient(raw.id),
    tone: meta.tone,
    toneLabel: meta.label,
    category: 'sample',
    featured: raw.status === 1,
    faved: false,
    views: 0,
    date: formatDateTime(raw.createTime),
    imageUrl: raw.thumbUrl || raw.fileUrl || raw.imageUrl,
    raw,
  }
}

const artworks = computed<DisplayItem[]>(() => sampleStore.list.map(mapToDisplay))

const filteredArtworks = computed(() => {
  let list = artworks.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter((a) => a.title.toLowerCase().includes(kw))
  }
  if (category.value) {
    list = list.filter((a) => a.category === category.value)
  }
  if (sortBy.value === 'popular') {
    list = [...list].sort((a, b) => b.views - a.views)
  } else if (sortBy.value === 'name') {
    list = [...list].sort((a, b) => a.title.localeCompare(b.title))
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(sampleStore.total / pageSize.value)))

watch(page, () => loadPage())
watch(pageSize, () => {
  page.value = 1
  loadPage()
})

async function loadPage() {
  await sampleStore.fetchPage({ pageNum: page.value, pageSize: pageSize.value })
  // 翻页后清空批量选中
  pickedIds.value = new Set()
}

function togglePick(id: string | number) {
  const s = new Set(pickedIds.value)
  s.has(id) ? s.delete(id) : s.add(id)
  pickedIds.value = s
}

function openPreview(item: DisplayItem) {
  previewItem.value = item
}

function showToast(msg: string) {
  toastMsg.value = msg
  setTimeout(() => {
    toastMsg.value = ''
  }, 2000)
}

function toggleFav(item: DisplayItem) {
  item.faved = !item.faved
  showToast(item.faved ? '已收藏' : '已取消收藏')
}

function onUpload() {
  router.push({ name: 'Collect' })
}

function onEdit(item: DisplayItem) {
  router.push({
    name: 'Collect',
    query: { editId: String(item.id), charId: String(item.raw.charId) },
  })
}

async function confirmRemove(item: DisplayItem) {
  if (confirm(`确定删除「${item.title}」吗？`)) {
    try {
      await sampleStore.remove(item.id)
      showToast('已删除')
    } catch (err) {
      showToast('删除失败')
    }
  }
}

async function confirmRemoveBatch() {
  if (pickedIds.value.size === 0) return
  if (!confirm(`确定删除选中的 ${pickedIds.value.size} 项吗？`)) return
  try {
    await sampleStore.removeMany([...pickedIds.value])
    pickedIds.value = new Set()
    showToast('已批量删除')
  } catch (err) {
    showToast('批量删除失败')
  }
}

onMounted(() => {
  loadPage()
})
</script>

<style>
/* ===== Reset & Variables ===== */
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

:root {
  --c-primary: #0d9488;
  --c-primary-dark: #0f766e;
  --c-primary-light: rgba(13, 148, 136, 0.12);
  --c-danger: #ef4444;
  --c-danger-dark: #b91c1c;
  --c-danger-light: rgba(239, 68, 68, 0.12);

  --bg-base: #f5f5f4;
  --bg-elevated: #ffffff;
  --bg-muted: #f0efed;

  --text-primary: #1c1917;
  --text-regular: #44403c;
  --text-secondary: #78716c;
  --text-placeholder: #a8a29e;

  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;

  --shadow-xs: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.08);

  --font-cn: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;
}

body {
  font-family: var(--font-cn);
  background: var(--bg-base);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
}
</style>

<style scoped>
.art-gallery {
  min-height: 100vh;
  padding: 32px 24px;
}

.gallery-shell {
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== 顶部导航 ===== */
.gallery-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}

.gallery-heading {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.nav-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 22px;
  background: var(--c-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.nav-cta:hover {
  background: var(--c-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.3);
}

.cta-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* ===== 搜索栏 ===== */
.search-dock {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.search-dock__input-wrap {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-muted);
  border-radius: var(--radius-sm);
  padding: 0 14px;
}

.search-dock__icon {
  width: 16px;
  height: 16px;
  color: var(--text-placeholder);
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.search-dock__input {
  flex: 1;
  height: 40px;
  background: transparent;
  font-size: 14px;
  color: var(--text-primary);
  border: none;
  outline: none;
  font-family: inherit;
}

.search-dock__input::placeholder {
  color: var(--text-placeholder);
}

.search-dock__tag {
  font-size: 14px;
  color: var(--text-regular);
  white-space: nowrap;
  flex-shrink: 0;
}

.search-dock__picker-wrap,
.search-dock__sort-wrap {
  flex-shrink: 0;
}

.search-dock__picker,
.search-dock__sort {
  height: 40px;
  padding: 0 32px 0 14px;
  background: var(--bg-muted);
  font-size: 14px;
  color: var(--text-primary);
  border: none;
  outline: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: inherit;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2378716c' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
}

/* ===== 空状态 ===== */
.empty-vault {
  text-align: center;
  padding: 80px 20px;
}

.empty-vault__icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: var(--text-placeholder);
  opacity: 0.6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.empty-vault__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-vault__desc {
  font-size: 14px;
  color: var(--text-secondary);
}

/* ===== 作品网格 ===== */
.artwork-mosaic {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1199px) {
  .artwork-mosaic {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 899px) {
  .artwork-mosaic {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 599px) {
  .artwork-mosaic {
    grid-template-columns: 1fr;
  }
}

/* ===== 作品卡片 ===== */
.artwork-tile {
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--bg-elevated);
  border-radius: var(--radius-md);
  padding: 12px;
  box-shadow: var(--shadow-xs);
  transition: all 0.25s;
}

.artwork-tile:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.artwork-tile.is-picked {
  outline: 2px solid var(--c-primary);
  outline-offset: -2px;
}

/* 选择器 */
.artwork-tile__selector {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 2;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 6px;
}

.artwork-tile__selector input {
  display: none;
}

.selector-mark {
  display: block;
  width: 14px;
  height: 14px;
  border-radius: 3px;
  background: var(--bg-muted);
  transition: all 0.15s;
}

.artwork-tile__selector input:checked + .selector-mark {
  background: var(--c-primary);
}

/* 图片区域 */
.artwork-tile__canvas {
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  aspect-ratio: 4 / 3;
}

.canvas-art {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.canvas-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f7f5f2;
}

.canvas-glyph {
  font-family: var(--font-cn);
  font-size: 56px;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

/* 标题 + 标签 */
.artwork-tile__caption {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.caption-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.caption-badges {
  display: flex;
  gap: 4px;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
  line-height: 1.6;
}

.badge--calm {
  background: rgba(13, 148, 136, 0.1);
  color: var(--c-primary-dark);
}

.badge--vivid {
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
}

.badge--warm {
  background: rgba(245, 158, 11, 0.1);
  color: #b45309;
}

.badge--star {
  background: rgba(245, 158, 11, 0.15);
  color: #b45309;
}

/* 元信息 */
.artwork-tile__footnote {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.footnote-views {
  color: var(--c-primary-dark);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 操作按钮 */
.artwork-tile__toolbar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-top: auto;
  padding-top: 10px;
}

.tool-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: var(--bg-muted);
  color: var(--text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tool-btn svg {
  width: 16px;
  height: 16px;
}

.tool-btn:hover {
  background: var(--c-primary-light);
  color: var(--c-primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px -1px rgba(13, 148, 136, 0.25);
}

.tool-btn--risk:hover {
  background: var(--c-danger-light);
  color: var(--c-danger-dark);
  box-shadow: 0 2px 6px -1px rgba(239, 68, 68, 0.25);
}

/* SVG 图标通用 */
.svg-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1em;
  height: 1em;
  line-height: 1;
}

.svg-icon svg {
  width: 100%;
  height: 100%;
}

.icon-inline {
  width: 14px;
  height: 14px;
}

/* ===== 底部分页 ===== */
.pager-strip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 24px;
  padding: 12px 20px;
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
}

.pager-strip__batch-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  user-select: none;
}

.pager-strip__batch-toggle input {
  display: none;
}

.toggle-track {
  display: inline-block;
  width: 36px;
  height: 20px;
  background: var(--bg-muted);
  border-radius: 10px;
  position: relative;
  transition: background 0.2s;
}

.toggle-track::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s;
}

.pager-strip__batch-toggle input:checked + .toggle-track {
  background: var(--c-primary);
}

.pager-strip__batch-toggle input:checked + .toggle-track::after {
  transform: translateX(16px);
}

.pager-strip__batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-del {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--c-danger-light);
  color: var(--c-danger-dark);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.batch-del:hover {
  background: var(--c-danger);
  color: #fff;
}

.pager-strip__controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-arrow {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--bg-muted);
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.page-arrow svg {
  width: 16px;
  height: 16px;
}

.page-arrow:hover:not(:disabled) {
  background: var(--c-primary-light);
  color: var(--c-primary-dark);
}

.page-arrow:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-dot {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}

.page-dot:hover {
  background: var(--bg-muted);
}

.page-dot--active {
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.page-dot--active:hover {
  background: var(--c-primary-dark);
}

.page-size {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 8px;
}

.page-size__label {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.page-size__select {
  height: 32px;
  padding: 0 26px 0 10px;
  background: var(--bg-muted);
  border: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2378716c' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
}

/* ===== 预览弹窗 ===== */
.preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 24px;
}

.preview-modal {
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  max-width: 480px;
  width: 100%;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  position: relative;
}

.preview-modal__close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 50%;
  cursor: pointer;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all 0.15s;
}

.preview-modal__close svg {
  width: 18px;
  height: 18px;
}

.preview-modal__close:hover {
  background: #fff;
  color: var(--text-primary);
}

.preview-modal__art {
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.preview-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-glyph {
  font-size: 80px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 700;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.preview-modal__body {
  padding: 24px;
}

.preview-modal__body h2 {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 8px;
}

.preview-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.preview-stats {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.preview-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ===== Toast ===== */
.toast-msg {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 28px;
  background: var(--text-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 14px;
  box-shadow: var(--shadow-md);
  z-index: 200;
}

/* ===== Transitions ===== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.25s ease;
}
.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
