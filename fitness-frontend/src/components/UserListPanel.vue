<template>
  <div class="user-list-panel">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>👥 系统用户</span>
          <el-tag type="success">{{ users.length }} 人</el-tag>
        </div>
      </template>
      
      <el-table :data="users" stripe size="small">
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column prop="nickname" label="昵称" width="100" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">
            {{ getGenderText(row.gender) }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="weight" label="体重" width="90">
          <template #default="{ row }">
            {{ row.weight }} kg
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const users = ref([])

const fetchUsers = async () => {
  try {
    const res = await request({
      url: '/admin/user/all',
      method: 'get',
    })
    if (res.code === 200) {
      users.value = res.data
    }
  } catch (error) {
    console.log('获取用户列表失败', error)
  }
}

const getGenderText = (gender) => {
  const map = { 0: '未知', 1: '男', 2: '女' }
  return map[gender] || '未知'
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-list-panel {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
