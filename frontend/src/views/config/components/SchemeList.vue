<template>
  <div class="scheme-list">
    <div class="toolbar">
      <el-button type="primary" @click="$emit('create')">
        <el-icon><Plus /></el-icon>
        新建方案
      </el-button>
    </div>

    <el-table :data="schemes" v-loading="loading" border>
      <el-table-column prop="schemeKey" label="方案Key" width="200" />
      <el-table-column prop="schemeName" label="方案名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="系统方案" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isSystem" type="success" size="small">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$emit('edit', row)">编辑</el-button>
          <el-button 
            link 
            type="danger" 
            :disabled="row.isSystem"
            @click="$emit('delete', row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

defineProps({
  schemes: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['create', 'edit', 'delete'])
</script>

<style scoped lang="scss">
.scheme-list {
  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
