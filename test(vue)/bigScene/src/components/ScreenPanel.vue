<template>
  <section class="panel fade-in">
    <div class="panel-corners"></div>
    <div class="panel-header">
      <div class="panel-titlebar">
        <div class="panel-title">{{ title }}</div>
        <div v-if="subtitle" class="panel-subtitle">{{ subtitle }}</div>
      </div>
      <div class="panel-extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="panel-body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.panel-extra {
  font-size: 11px;
  color: var(--t-2);
}
</style>

