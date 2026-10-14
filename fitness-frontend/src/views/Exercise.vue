<template>
  <div class="exercise-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>运动记录</span>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>
            添加运动
          </el-button>
        </div>
      </template>
      
      <el-table :data="exerciseList" v-loading="loading" stripe>
        <el-table-column prop="exerciseName" label="运动类型" />
        <el-table-column prop="duration" label="时长 (分钟)" width="120" />
        <el-table-column prop="calorie" label="消耗 (kcal)" width="120">
          <template #default="{ row }">
            <el-tag type="success">{{ row.calorie }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="distance" label="距离 (km)" width="120">
          <template #default="{ row }">
            {{ row.distance || '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="exerciseTime" label="运动时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.exerciseTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 添加运动对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加运动记录" width="400px">
      <el-form :model="exerciseForm" label-width="100px">
        <el-form-item label="运动类型">
          <el-select v-model="exerciseForm.exerciseType" placeholder="选择运动类型">
            <el-option label="跑步" :value="1" />
            <el-option label="骑行" :value="2" />
            <el-option label="游泳" :value="3" />
            <el-option label="健身" :value="4" />
            <el-option label="瑜伽" :value="5" />
            <el-option label="其他" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="运动名称">
          <el-input v-model="exerciseForm.exerciseName" placeholder="如：晨跑" />
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="exerciseForm.duration" :min="1" :max="300" />
          <span class="unit">分钟</span>
        </el-form-item>
        <el-form-item label="距离">
          <el-input-number v-model="exerciseForm.distance" :min="0" :max="100" :precision="2" />
          <span class="unit">km（可选）</span>
        </el-form-item>
        <el-form-item label="运动强度">
          <el-radio-group v-model="exerciseForm.intensity">
            <el-radio :label="1">低</el-radio>
            <el-radio :label="2">中</el-radio>
            <el-radio :label="3">高</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getExerciseList, addExercise, deleteExercise } from '@/api/exercise'

const loading = ref(false)
const exerciseList = ref([])
const addDialogVisible = ref(false)
const exerciseForm = ref({
  exerciseType: 1,
  exerciseName: '',
  duration: 30,
  distance: null,
  intensity: 2,
})

const exerciseTypeMap = {
  1: '跑步',
  2: '骑行',
  3: '游泳',
  4: '健身',
  5: '瑜伽',
  6: '其他',
}

const fetchExerciseList = async () => {
  loading.value = true
  try {
    console.log('正在获取运动记录...')
    const res = await getExerciseList()
    console.log('运动记录响应:', res)
    if (res.code === 200) {
      exerciseList.value = (res.data || []).map(item => ({
        ...item,
        exerciseName: item.exerciseName || exerciseTypeMap[item.exerciseType],
      }))
      console.log('获取到记录数量:', exerciseList.value.length)
      // 空数据不报错
      if (exerciseList.value.length === 0) {
        console.log('暂无运动记录')
      }
    } else {
      console.warn('获取数据失败:', res.msg)
      exerciseList.value = []
    }
  } catch (error) {
    console.error('获取运动记录失败:', error)
    exerciseList.value = []
    // 不显示错误提示，静默处理
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  exerciseForm.value = {
    exerciseType: 1,
    exerciseName: '',
    duration: 30,
    distance: null,
    intensity: 2,
  }
  addDialogVisible.value = true
}

const handleAdd = async () => {
  if (!exerciseForm.value.exerciseName) {
    ElMessage.warning('请输入运动名称')
    return
  }
  try {
    const res = await addExercise(exerciseForm.value)
    if (res.code === 200) {
      ElMessage.success('添加成功')
      addDialogVisible.value = false
      fetchExerciseList()
    }
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      const res = await deleteExercise(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        fetchExerciseList()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  fetchExerciseList()
})
</script>

<style scoped>
.exercise-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.unit {
  margin-left: 10px;
  color: #909399;
}
</style>
