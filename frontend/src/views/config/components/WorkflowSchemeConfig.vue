<template>
  <TypeSchemeConfig :scheme="scheme" @save="$emit('save', $event)" />
</template>

<script setup>
import TypeSchemeConfig from './TypeSchemeConfig.vue'

defineProps({ scheme: Object })
defineEmits(['save'])
</script>
