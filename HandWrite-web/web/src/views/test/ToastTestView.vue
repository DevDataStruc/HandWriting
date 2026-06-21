<template>
  <div class="hw-page toast-test-view">
    <BaseCard title="ElMessage 样式测试" subtitle="同时触发多条消息会按队列自动堆叠" class="card-1">
      <template #extra>
        <el-button-group>
          <el-button type="primary" @click="showStacked">同时触发 5 条（堆叠）</el-button>
          <el-button @click="showOneByOne">逐条触发</el-button>
          <el-button type="danger" plain @click="closeAll">关闭全部</el-button>
        </el-button-group>
      </template>

      <p class="hint">
        点击「同时触发 5 条」按钮：所有消息会按顺序入队，并依次向下堆叠（每条间隔 16px）。
        <code>onMounted</code> 也会自动演示一次堆叠。
      </p>

      <el-divider />

      <div class="trigger-row">
        <el-button type="success" @click="showSuccess">成功 success</el-button>
        <el-button type="warning" @click="showWarning">警告 warning</el-button>
        <el-button type="danger" @click="showError">错误 error</el-button>
        <el-button type="info" @click="showInfo">信息 info</el-button>
        <el-button type="primary" @click="showUnauthorized">未授权提示</el-button>
      </div>
    </BaseCard>

    <BaseCard title="ElMessage 在容器内（验证 fixed 修复）" subtitle="把卡片加上 transform 后，toast 仍应浮在视口顶部" class="card-1">
      <div class="transformed-card">
        <p>这个卡片使用了 <code>transform: translateY(0) scale(1)</code>，模拟场景中常见的"transform 父级"问题。</p>
        <el-button @click="showInfo">在此容器内触发 toast</el-button>
      </div>
    </BaseCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'

function showSuccess() {
  ElMessage.success('保存成功！样本已上传至服务器。')
}
function showWarning() {
  ElMessage.warning('请先书写再上传')
}
function showError() {
  ElMessage.error('操作失败，请稍后重试')
}
function showInfo() {
  ElMessage.info('这是一条普通提示信息')
}
function showUnauthorized() {
  ElMessage({
    type: 'warning',
    message: '未登录或登录已过期，请先登录',
    duration: 0,
    customClass: 'hw-unauthorized-toast',
    showClose: true,
  })
}

/** 关键演示：同时触发 5 条不同类型的 toast，验证队列堆叠 */
function showStacked() {
  ElMessage.success('① 成功：操作已完成')
  ElMessage.warning('② 警告：请检查输入')
  ElMessage.error('③ 错误：服务器异常')
  ElMessage.info('④ 信息：系统升级通知')
  ElMessage({
    type: 'warning',
    message: '⑤ 未授权：登录已过期',
    customClass: 'hw-unauthorized-toast',
    duration: 0,
    showClose: true,
  })
}

function showOneByOne() {
  showSuccess()
  setTimeout(showInfo, 400)
  setTimeout(showWarning, 800)
  setTimeout(showError, 1200)
  setTimeout(showUnauthorized, 1600)
}

function closeAll() {
  ElMessage.closeAll()
}

onMounted(() => {
  // 自动演示一次堆叠，方便用户进页面就看到效果
  setTimeout(showStacked, 300)
})
onUnmounted(() => {
  ElMessage.closeAll()
})
</script>

<style lang="scss" scoped>
.toast-test-view {
  background: linear-gradient(180deg, $bg-base 0%, $bg-muted 100%);
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}
.card-1 {
  width: 100%;
}
.trigger-row {
  @include flex-start;
  gap: $spacing-md;
  flex-wrap: wrap;
  margin-top: $spacing-md;
}
.hint {
  font-size: $font-size-sm;
  color: $text-secondary;
  line-height: $line-height-relaxed;
  code {
    background: $bg-muted;
    color: $color-primary-dark;
    padding: 2px 6px;
    border-radius: $radius-sm;
    font-family: $font-family-mono;
  }
}
.transformed-card {
  padding: $spacing-md;
  background: $bg-elevated;
  border-radius: $radius-md;
  // 关键：故意加 transform，验证 toast 不会被它影响
  transform: translateY(0) scale(1);
  will-change: transform;
  border: 1px dashed $color-warning;
  p {
    margin: 0 0 $spacing-sm;
    font-size: $font-size-sm;
    color: $text-secondary;
    code {
      background: $bg-muted;
      padding: 2px 6px;
      border-radius: $radius-sm;
      font-family: $font-family-mono;
      color: $color-primary-dark;
    }
  }
}
</style>
