<template>
  <!--
    2FA 绑定向导
    - 样式仅作用于本文件（scoped），所有类加 ts- 前缀（totp-setup）避免冲突
  -->
  <div class="ts-page">
    <div class="ts-card">
      <!-- 顶部品牌头 -->
      <div class="ts-header">
        <div class="ts-header__icon">
          <el-icon :size="24"><Cellphone /></el-icon>
        </div>
        <div class="ts-header__text">
          <h1 class="ts-header__title">动态口令（2FA）</h1>
          <p class="ts-header__sub">
            绑定 Authenticator 类应用（Google / Microsoft / 阿里 / 腾讯 等）后，每次登录或找回密码时需要 6 位动态口令
          </p>
        </div>
      </div>

      <!-- 状态：未绑定 -->
      <div v-if="!status?.bound" class="ts-state ts-state--unbound">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="你尚未启用动态口令"
          description="绑定后，你的账号将多一层保护。建议使用 Google Authenticator / Microsoft Authenticator。"
        />
        <div v-if="!setup" class="ts-cta">
          <BaseButton type="primary" :loading="loading" @click="onSetup">
            开始绑定
          </BaseButton>
        </div>
      </div>

      <!-- 状态：已绑定 -->
      <div v-else class="ts-state ts-state--bound">
        <el-alert type="success" :closable="false" show-icon
          title="已启用动态口令"
          :description="`账户：${status?.accountLabel}，剩余恢复码：${status?.remainingRecoveryCodes} 个`"
        />
        <div class="ts-cta">
          <BaseButton type="danger" plain :loading="unbinding" @click="onUnbind">
            解绑动态口令
          </BaseButton>
        </div>
      </div>

      <!-- 绑定向导：二维码 + secret + 恢复码 -->
      <div v-if="setup" class="ts-setup">
        <el-steps :active="2" finish-status="success" simple class="ts-steps">
          <el-step title="安装认证器" />
          <el-step title="扫描二维码" />
          <el-step title="输入动态口令" />
        </el-steps>

        <div class="ts-setup__body">
          <!-- 二维码 + secret -->
          <div class="ts-setup__qr">
            <div class="ts-qr-frame">
              <img v-if="setup.qrCodeBase64" :src="setup.qrCodeBase64" alt="二维码" />
            </div>
            <p class="ts-tip">使用认证器扫描二维码</p>
            <p class="ts-tip ts-tip--mute">
              扫描失败？将密钥手动输入到认证器：
              <code class="ts-secret">{{ setup.secret }}</code>
              <el-button
                class="ts-copy"
                size="small"
                link
                :icon="CopyDocument"
                @click="copySecret"
              >复制</el-button>
            </p>
          </div>

          <!-- 恢复码 -->
          <div class="ts-setup__recovery">
            <h3 class="ts-block-title">
              <el-icon><Tickets /></el-icon>
              一次性恢复码
            </h3>
            <p class="ts-tip">
              以下 <strong>10 个</strong>恢复码每个仅可使用一次。请截图或抄写保存到安全的地方，丢失后无法找回。
            </p>
            <ul class="ts-recovery-list">
              <li v-for="(c, idx) in setup.recoveryCodes" :key="idx" class="ts-recovery-item">
                <code>{{ c }}</code>
                <el-button size="small" link @click="copyOne(c)">复制</el-button>
              </li>
            </ul>
          </div>
        </div>

        <!-- 验证 6 位码完成绑定 -->
        <div class="ts-confirm">
          <h3 class="ts-block-title">
            <el-icon><Check /></el-icon>
            验证并完成绑定
          </h3>
          <p class="ts-tip">打开认证器，将当前显示的 6 位数字输入下方：</p>
          <div class="ts-confirm-row">
            <BaseInput
              v-model="verifyCode"
              placeholder="6 位动态口令"
              :maxlength="6"
              :prefix-icon="'Key'"
              style="max-width: 240px"
            />
            <BaseButton
              type="primary"
              :loading="confirming"
              :disabled="!/^\d{6}$/.test(verifyCode)"
              @click="onConfirmBind"
            >
              完成绑定
            </BaseButton>
            <BaseButton type="ghost" @click="onCancelSetup">取消</BaseButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Cellphone,
  Check,
  CopyDocument,
  Tickets,
} from '@element-plus/icons-vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import {
  bindTotp,
  getTotpStatus,
  setupTotp,
  unbindTotp,
} from '@/api/auth'
import type { TotpSetupVO, TotpStatusVO } from '@/api/contracts/auth'

const status = ref<TotpStatusVO | null>(null)
const setup = ref<TotpSetupVO | null>(null)
const verifyCode = ref('')
const loading = ref(false)
const confirming = ref(false)
const unbinding = ref(false)

async function refresh() {
  loading.value = true
  try {
    status.value = await getTotpStatus()
  } catch (err) {
    console.warn('totp status error', err)
  } finally {
    loading.value = false
  }
}

async function onSetup() {
  loading.value = true
  try {
    setup.value = await setupTotp()
  } catch (err) {
    console.warn('totp setup error', err)
  } finally {
    loading.value = false
  }
}

async function onConfirmBind() {
  if (!/^\d{6}$/.test(verifyCode.value)) {
    ElMessage.warning('请输入 6 位动态口令')
    return
  }
  confirming.value = true
  try {
    await bindTotp({ code: verifyCode.value })
    ElMessage.success('绑定成功！后续登录 / 找回密码时将需要 6 位动态口令')
    setup.value = null
    verifyCode.value = ''
    await refresh()
  } catch (err) {
    console.warn('bind error', err)
  } finally {
    confirming.value = false
  }
}

function onCancelSetup() {
  setup.value = null
  verifyCode.value = ''
}

async function onUnbind() {
  try {
    await ElMessageBox.confirm(
      '解绑后无法使用动态口令找回密码，确认解绑？',
      '解绑动态口令',
      { type: 'warning', confirmButtonText: '确认解绑', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  unbinding.value = true
  try {
    await unbindTotp()
    ElMessage.success('已解绑动态口令')
    await refresh()
  } catch (err) {
    console.warn('unbind error', err)
  } finally {
    unbinding.value = false
  }
}

async function copySecret() {
  if (!setup.value?.secret) return
  try {
    await navigator.clipboard.writeText(setup.value.secret)
    ElMessage.success('密钥已复制')
  } catch {
    ElMessage.error('复制失败，请手动选中')
  }
}

async function copyOne(c: string) {
  try {
    await navigator.clipboard.writeText(c)
    ElMessage.success(`已复制：${c}`)
  } catch {
    ElMessage.error('复制失败，请手动选中')
  }
}

onMounted(refresh)
</script>

<style lang="scss" scoped>
/* =========================================================================
   2FA 设置页 — 样式仅作用于本文件
   - 所有类以 ts- 前缀（totp-setup）
   - scoped 隔离，自动 data-v-xxx
   ========================================================================= */

.ts-page {
  width: 100%;
  padding: $spacing-lg 0;
}

.ts-card {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  background: $bg-elevated;
  border-radius: $radius-2xl;
  padding: $spacing-xl $spacing-2xl;
  box-shadow: $shadow-md;
}

.ts-header {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;
  padding-bottom: $spacing-md;
  border-bottom: 1px solid $border-light;
  margin-bottom: $spacing-lg;

  &__icon {
    width: 48px;
    height: 48px;
    border-radius: $radius-lg;
    background: linear-gradient(135deg, $color-primary 0%, $color-primary-dark 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__title {
    font-size: 20px;
    font-weight: 700;
    margin: 0 0 4px;
    color: $text-primary;
  }

  &__sub {
    font-size: $font-size-sm;
    color: $text-secondary;
    margin: 0;
    line-height: 1.6;
  }
}

.ts-state {
  &--bound,
  &--unbound {
    display: flex;
    flex-direction: column;
    gap: $spacing-md;
  }
}

.ts-cta {
  display: flex;
  gap: $spacing-sm;
}

.ts-setup {
  margin-top: $spacing-md;
}

.ts-steps {
  margin-bottom: $spacing-lg;
}

.ts-setup__body {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: $spacing-lg;
  align-items: stretch;

  @media (max-width: 720px) {
    grid-template-columns: 1fr;
  }
}

.ts-setup__qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: $spacing-sm;
  padding: $spacing-md;
  background: $bg-muted;
  border-radius: $radius-lg;
}

.ts-qr-frame {
  width: 220px;
  height: 220px;
  background: #fff;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-sm;
  overflow: hidden;

  img {
    width: 200px;
    height: 200px;
    object-fit: contain;
  }
}

.ts-tip {
  font-size: $font-size-sm;
  color: $text-secondary;
  margin: 0;
  line-height: 1.6;

  &--mute {
    font-size: $font-size-xs;
    color: $text-placeholder;
    word-break: break-all;
  }
}

.ts-secret {
  display: inline-block;
  background: #fff;
  border: 1px solid $border-light;
  border-radius: $radius-sm;
  padding: 2px 6px;
  margin: 0 4px;
  font-family: $font-family-mono;
  font-size: 12px;
  color: $text-primary;
  user-select: all;
}

.ts-copy {
  margin-left: 4px;
}

.ts-setup__recovery {
  padding: $spacing-md;
  background: $bg-muted;
  border-radius: $radius-lg;
}

.ts-block-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: $font-size-md;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 $spacing-sm;
}

.ts-recovery-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-xs $spacing-sm;

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.ts-recovery-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: $radius-sm;
  padding: 6px 10px;
  font-size: $font-size-sm;
  color: $text-primary;
  border: 1px solid $border-light;

  code {
    font-family: $font-family-mono;
    letter-spacing: 1px;
  }
}

.ts-confirm {
  margin-top: $spacing-lg;
  padding: $spacing-md;
  background: $bg-overlay;
  border-radius: $radius-lg;
  border: 1px solid $border-base;
}

.ts-confirm-row {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  flex-wrap: wrap;
  margin-top: $spacing-sm;
}
</style>
