<template>
  <div class="base-input" :class="{ 'is-disabled': disabled, 'has-error': error }">
    <label v-if="label" class="base-input__label" :for="inputId">
      {{ label }}
      <span v-if="required" class="required">*</span>
    </label>
    <div class="base-input__wrapper">
      <el-icon v-if="prefixIcon" class="base-input__prefix"><component :is="prefixIcon" /></el-icon>
      <input
        :id="inputId"
        class="base-input__inner"
        :type="realType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxlength"
        :autocomplete="autocomplete"
        @input="onInput"
        @blur="emit('blur', $event)"
        @focus="emit('focus', $event)"
      />
      <span class="base-input__suffix">
        <el-icon
          v-if="showPassword && type === 'password'"
          class="suffix-icon"
          @click="togglePassword"
        >
          <component :is="passwordVisible ? 'View' : 'Hide'" />
        </el-icon>
        <el-icon v-else-if="clearable && modelValue" class="clear-icon" @click="clear"><CircleClose /></el-icon>
        <span v-else-if="suffix">{{ suffix }}</span>
      </span>
    </div>
    <div v-if="error || hint" class="base-input__hint" :class="{ 'is-error': !!error }">
      {{ error || hint }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string | number
    label?: string
    placeholder?: string
    type?: string
    disabled?: boolean
    readonly?: boolean
    required?: boolean
    clearable?: boolean
    error?: string
    hint?: string
    maxlength?: number
    prefixIcon?: string
    suffix?: string
    autocomplete?: string
    showPassword?: boolean
  }>(),
  { type: 'text', disabled: false, readonly: false, required: false, clearable: false, showPassword: false }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  blur: [event: FocusEvent]
  focus: [event: FocusEvent]
  clear: []
}>()

const inputId = computed(() => `bi-${Math.random().toString(36).slice(2, 8)}`)
const passwordVisible = ref(false)
const realType = computed(() => {
  if (props.type === 'password' && props.showPassword) {
    return passwordVisible.value ? 'text' : 'password'
  }
  return props.type
})

function onInput(e: Event) {
  const target = e.target as HTMLInputElement
  emit('update:modelValue', target.value)
}

function clear() {
  emit('update:modelValue', '')
  emit('clear')
}

function togglePassword() {
  passwordVisible.value = !passwordVisible.value
}
</script>

<style lang="scss" scoped>
.base-input {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;

  &__label {
    font-size: $font-size-sm;
    color: $text-regular;
    font-weight: 500;
    .required {
      color: $color-danger;
      margin-left: 2px;
    }
  }

  &__wrapper {
    position: relative;
    @include flex-start;
    height: 40px;
    padding: 0 12px;
    background: $bg-elevated;
    border: 1px solid $border-base;
    border-radius: $radius-md;
    transition: all $transition-fast;

    &:hover {
      border-color: $color-primary-lighter;
    }
    &:focus-within {
      border-color: $color-primary;
      box-shadow: 0 0 0 3px rgba(13, 148, 136, 0.12);
    }
  }

  &.is-disabled &__wrapper {
    background: $bg-muted;
    cursor: not-allowed;
    opacity: 0.7;
  }

  &.has-error &__wrapper {
    border-color: $color-danger;
  }

  &__prefix {
    color: $text-placeholder;
    margin-right: $spacing-sm;
  }

  &__inner {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    font-size: $font-size-base;
    color: $text-primary;
    &::placeholder {
      color: $text-placeholder;
    }
  }

  &__suffix {
    @include flex-end;
    gap: 4px;
    color: $text-placeholder;
    font-size: $font-size-sm;
  }

  &__hint {
    font-size: $font-size-xs;
    color: $text-secondary;
    &.is-error {
      color: $color-danger;
    }
  }
}

.clear-icon {
  cursor: pointer;
  transition: color $transition-fast;
  &:hover {
    color: $color-danger;
  }
}

.suffix-icon {
  cursor: pointer;
  color: $text-placeholder;
  transition: color $transition-fast;
  &:hover {
    color: $color-primary;
  }
}
</style>
