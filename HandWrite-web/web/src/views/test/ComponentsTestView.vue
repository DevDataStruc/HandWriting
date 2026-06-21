<template>
  <div class="components-test-view hw-page">
    <BaseCard title="组件样式总览" subtitle="用于审查 Element Plus 改造后的所有交互元素">
      <template #extra>
        <el-button-group>
          <el-button @click="triggerRandom">随机触发 toast</el-button>
          <el-button type="primary" @click="openDialog">打开弹窗</el-button>
        </el-button-group>
      </template>

      <p class="hint">
        本页面系统展示按钮 / 表单 / 提示 / 弹窗 / 表格 / 标签 / 分页 / 加载 等所有交互组件的最新样式。
        所有改动遵循 teal 主色系（#0D9488），无侵入、不破坏原项目结构。
      </p>
    </BaseCard>

    <!-- Section: 按钮 -->
    <BaseCard title="1. 按钮 Buttons" subtitle="Primary 使用渐变 + 悬浮抬起；loading 旋转 spinner" class="section">
      <p class="hint-line">默认尺寸：高度 40px / 圆角 12px</p>
      <div class="row">
        <el-button>默认</el-button>
        <el-button type="primary">主要操作</el-button>
        <el-button type="success">成功 success</el-button>
        <el-button type="warning">警告 warning</el-button>
        <el-button type="danger">危险 danger</el-button>
        <el-button type="info">信息 info</el-button>
      </div>
      <p class="hint-line">全圆角（is-round）：圆角 = 高度一半</p>
      <div class="row">
        <el-button round>默认</el-button>
        <el-button type="primary" round>主要操作</el-button>
        <el-button type="success" round>成功</el-button>
        <el-button type="warning" round>警告</el-button>
        <el-button type="danger" round>危险</el-button>
        <el-button round>
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
      <p class="hint-line">状态：朴素 / loading / 禁用 / 圆圈</p>
      <div class="row">
        <el-button plain>朴素按钮</el-button>
        <el-button type="primary" plain>主要朴素</el-button>
        <el-button circle><el-icon><Search /></el-icon></el-button>
        <el-button type="primary" :loading="true">加载中</el-button>
        <el-button disabled>禁用</el-button>
      </div>
      <p class="hint-line">三档尺寸：small(32px) / default(40px) / large(48px)</p>
      <div class="row">
        <el-button size="small">小号</el-button>
        <el-button>默认</el-button>
        <el-button size="large">大号</el-button>
        <el-button type="primary" size="large" round>
          <el-icon><Position /></el-icon>
          立即开始
        </el-button>
        <el-button type="warning" size="large" round>领取奖励</el-button>
        <el-button text>文字按钮</el-button>
      </div>
    </BaseCard>

    <!-- Section: 表单 -->
    <BaseCard title="2. 表单 Form" subtitle="聚焦光晕 + 错误抖动 + 错误外环" class="section">
      <el-form :model="form" label-width="100px" label-position="right" style="max-width: 520px">
        <el-form-item label="用户名" :error="form.usernameError">
          <el-input v-model="form.username" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.bio" type="textarea" :rows="2" placeholder="自我介绍" />
        </el-form-item>
        
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="请选择" style="width: 180px">
            <el-option
            v-for  = "item in roleOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
            />
          </el-select>
        </el-form-item>
        

        <el-form-item label="记住">
          <el-checkbox-group v-model="form.perms">
            <el-checkbox value="read">读取</el-checkbox>
            <el-checkbox value="write">写入</el-checkbox>
            <el-checkbox value="audit">审核</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label=" ">
          <el-button @click="validate">校验</el-button>
          <el-button type="primary" @click="validate">提交</el-button>
        </el-form-item>
      </el-form>
    </BaseCard>

    <!-- Section: 提示 / 弹窗 -->
    <BaseCard title="3. 提示与弹窗" subtitle="4 种类型 toast + 通知 + 确认框 + 居中动画" class="section">
      <div class="row">
        <el-button @click="showToast('success')">success</el-button>
        <el-button @click="showToast('warning')">warning</el-button>
        <el-button @click="showToast('error')">error</el-button>
        <el-button @click="showToast('info')">info</el-button>
        <el-button type="warning" @click="showUnauthorized">未授权提示</el-button>
      </div>
      <el-divider />
      <div class="row">
        <el-button @click="showNotification('success')">通知 success</el-button>
        <el-button @click="showNotification('warning')">通知 warning</el-button>
        <el-button @click="showNotification('error')">通知 error</el-button>
        <el-button @click="showNotification('info')">通知 info</el-button>
      </div>
      <el-divider />
      <div class="row">
        <el-button @click="showMessageBox('alert')">MessageBox alert</el-button>
        <el-button @click="showMessageBox('confirm')">MessageBox confirm</el-button>
        <el-button @click="showMessageBox('prompt')">MessageBox prompt</el-button>
        <el-button @click="openDialog">Dialog 弹窗</el-button>
      </div>
    </BaseCard>

    <!-- Section: Tag -->
    <BaseCard title="4. 标签 Tag" subtitle="胶囊形 + 加深色文字 + 多尺寸" class="section">
      <p class="hint-line">默认尺寸：高度 28px / 圆角 999px（胶囊）</p>
      <div class="row">
        <el-tag>默认</el-tag>
        <el-tag type="primary">采集中</el-tag>
        <el-tag type="success">已通过</el-tag>
        <el-tag type="warning">待审核</el-tag>
        <el-tag type="danger">已驳回</el-tag>
        <el-tag type="info">信息</el-tag>
      </div>
      <p class="hint-line">不同 effect：light / dark / plain</p>
      <div class="row">
        <el-tag effect="light" type="success">light 已通过</el-tag>
        <el-tag effect="dark" type="success">dark 已通过</el-tag>
        <el-tag effect="plain" type="success">plain 已通过</el-tag>
        <el-tag effect="light" type="warning" size="large">
          <el-icon><Warning /></el-icon>
          大号带图标
        </el-tag>
        <el-tag effect="light" type="danger" size="small">小号 small</el-tag>
      </div>
    </BaseCard>

    <!-- Section: 表格 -->
    <BaseCard title="5. 表格 Table" subtitle="表头加深 + 悬浮高亮" class="section">
      <el-table :data="tableData" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="user" label="用户" />
        <el-table-column prop="char" label="字符" width="80" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.statusType" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="提交时间" />
        <el-table-column label="操作" width="160">
          <template #default>
            <el-button text type="primary">查看</el-button>
            <el-button text type="danger">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="123"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </BaseCard>

    <!-- Section: 其他交互 -->
    <BaseCard title="6. 其他交互" subtitle="Tooltip / Dropdown / Tabs / Loading" class="section">
      <div class="row">
        <el-tooltip content="这是提示内容" placement="top">
          <el-button>悬停查看 Tooltip</el-button>
        </el-tooltip>

        <el-dropdown @command="onCommand">
          <el-button>
            下拉菜单<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="a">选项 A</el-dropdown-item>
              <el-dropdown-item command="b" disabled>选项 B（禁用）</el-dropdown-item>
              <el-dropdown-item command="c" divided>选项 C</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-switch v-model="switchVal" active-text="开" inactive-text="关" />

        <el-button :loading="loading" @click="simulateLoading">模拟 loading</el-button>
      </div>
    </BaseCard>

    <!-- Dialog 弹窗 -->
    <el-dialog v-model="dialogVisible" title="Dialog 弹窗" width="520px">
      <p>这是弹窗主体内容。展示新的阴影、圆角、进入动画。</p>
      <p>支持任意 Element Plus 风格的交互。</p>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="dialogVisible = false">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import BaseCard from '@/components/base/BaseCard.vue'

const roleOptions = [
  { value: 'collector', label: '采集员' },
  { value: 'auditor', label: '审核员' },
  { value: 'admin', label: '管理员' },
] 

const form = reactive({
  username: '',
  usernameError: '',
  password: '',
  bio: '',
  role: '',
  perms: [] as Array<string | number>,
})

const page = ref(1)
const size = ref(10)
const switchVal = ref(true)
const dialogVisible = ref(false)
const loading = ref(false)

const tableData = [
  { id: 1, user: '张三', char: '永', status: '已通过', statusType: 'success', time: '2025-01-15 14:23' },
  { id: 2, user: '李四', char: '字', status: '待审核', statusType: 'warning', time: '2025-01-15 14:35' },
  { id: 3, user: '王五', char: '春', status: '已驳回', statusType: 'danger', time: '2025-01-15 15:01' },
  { id: 4, user: '赵六', char: '风', status: '待审核', statusType: 'warning', time: '2025-01-15 15:20' },
]

function validate() {
  if (!form.username) {
    form.usernameError = '请输入用户名'
  } else {
    form.usernameError = ''
    ElMessage.success('校验通过！')
  }
}

function showToast(type: 'success' | 'warning' | 'error' | 'info') {
  const map = {
    success: '操作成功，样本已保存',
    warning: '请检查输入内容',
    error: '操作失败，请稍后重试',
    info: '这是一条普通通知',
  }
  ElMessage[type](map[type])
}

function showUnauthorized() {
  ElMessage({
    type: 'warning',
    message: '未登录或登录已过期，请先登录',
    duration: 3000,
    customClass: 'hw-unauthorized-toast',
  })
}

function showNotification(type: 'success' | 'warning' | 'error' | 'info') {
  ElNotification[type]({
    title: '系统通知',
    message: `这是一条 ${type} 类型的通知，测试左侧色条 + 进入动画。`,
    duration: 0,
  })
}

function showMessageBox(type: 'alert' | 'confirm' | 'prompt') {
  if (type === 'alert') {
    ElMessageBox.alert('这是 alert 消息框，居中显示并带有动画。', '提示', { type: 'warning' })
  } else if (type === 'confirm') {
    ElMessageBox.confirm('确认执行此操作吗？', '确认', { type: 'info' })
      .then(() => ElMessage.success('已确认'))
      .catch(() => ElMessage.info('已取消'))
  } else {
    ElMessageBox.prompt('请输入您的昵称', '昵称', { confirmButtonText: '确定' })
      .then(({ value }) => ElMessage.success(`你好，${value}`))
      .catch(() => ElMessage.info('已取消'))
  }
}

function openDialog() {
  dialogVisible.value = true
}

function onCommand(cmd: string) {
  ElMessage.info(`选中：${cmd}`)
}

function triggerRandom() {
  const types: Array<'success' | 'warning' | 'error' | 'info'> = ['success', 'warning', 'error', 'info']
  types.forEach((t, i) => setTimeout(() => showToast(t), i * 400))
}

function simulateLoading() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('加载完成')
  }, 1500)
}
</script>

<style lang="scss" scoped>
.components-test-view {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  padding: $spacing-md;
  background: $bg-base;
  min-height: 100vh;
}

.section {
  .row {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
    align-items: center;
    margin-bottom: $spacing-md;

    &:last-child {
      margin-bottom: 0;
    }
  }
  .line{
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
    align-items: center;
    margin-bottom: $spacing-md;
  }
}

.hint {
  font-size: $font-size-sm;
  color: $text-secondary;
  line-height: $line-height-relaxed;
  margin: 0;
  padding: $spacing-sm $spacing-md;
  background: $bg-muted;
  border-left: 3px solid $color-primary;
  border-radius: $radius-sm;
}

.hint-line {
  font-size: $font-size-xs;
  color: $text-placeholder;
  margin: 0 0 $spacing-sm;
  letter-spacing: 0.3px;
  width: 100%;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-md;
}
</style>
