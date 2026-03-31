<template>
  <div class="ticker">
    <ul>
      <li v-for="it in items" :key="it.id">
        <span class="dot"></span>
        <span class="main">{{ it.title }}</span>
        <span class="time">{{ it.time }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ id: string | number; title: string; time: string }>
}>()
</script>

