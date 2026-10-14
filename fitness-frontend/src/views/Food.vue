<template>
  <div class="food-page">
    <!-- 今日热量统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="12">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ todayTotalCalorie }}</div>
          <div class="stat-label">今日总摄入 (kcal)</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ remainingCalorie }}</div>
          <div class="stat-label">剩余可摄入 (kcal)</div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-card class="food-card">
      <template #header>
        <div class="card-header">
          <span>🍽️ 饮食记录</span>
          <div class="header-actions">
            <el-button type="info" @click="exportData">
              <el-icon><Download /></el-icon>
              导出记录
            </el-button>
            <el-button type="primary" @click="showCameraDialog">
              <el-icon><Camera /></el-icon>
              拍照识别
            </el-button>
            <el-button type="success" @click="showAddDialog">
              <el-icon><Plus /></el-icon>
              手动添加
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 食物列表 -->
      <el-table :data="foodList" v-loading="loading" stripe>
        <el-table-column prop="foodName" label="食物名称" />
        <el-table-column prop="weight" label="重量 (g)" width="120">
          <template #default="{ row }">
            {{ row.weight || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="calorie" label="热量 (kcal)" width="120">
          <template #default="{ row }">
            <el-tag type="warning">{{ row.calorie }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordTime" label="记录时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.recordTime) }}
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
      
      <!-- 空状态 -->
      <el-empty v-if="foodList.length === 0 && !loading" description="暂无饮食记录" />
    </el-card>
    
    <!-- 手动添加对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加饮食记录" width="480px">
      <el-form :model="foodForm" label-width="100px">
        <el-form-item label="食物分类">
          <el-select
            v-model="selectedCategory"
            placeholder="全部分类"
            clearable
            style="width: 100%"
            @change="handleCategoryChange"
          >
            <el-option
              v-for="cat in foodCategories"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="食物名称">
          <el-select 
            v-model="foodForm.foodName" 
            placeholder="选择或输入食物"
            filterable
            allow-create
            clearable
            style="width: 100%"
            @change="handleFoodChange"
          >
            <el-option
              v-for="item in filteredFoods"
              :key="item.id || item.foodName"
              :label="`${item.foodName} (${item.caloriePer100g} kcal/100g)`"
              :value="item.foodName"
            />
          </el-select>
          <div class="form-hint">共 {{ filteredFoods.length }} 种可选，也可直接输入自定义食物</div>
        </el-form-item>
        <el-form-item label="重量 (g)">
          <el-input-number 
            v-model="foodForm.weight" 
            :min="1" 
            :max="5000" 
            style="width: 150px"
            @change="calculateCalorie"
          />
        </el-form-item>
        <el-form-item label="热量 (kcal)">
          <el-input-number 
            v-model="foodForm.calorie" 
            :min="0" 
            :max="5000" 
            style="width: 150px"
          />
          <span class="unit">自动计算</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 拍照识别对话框 -->
    <el-dialog v-model="cameraDialogVisible" title="📷 拍照识别食物热量" width="500px">
      <div class="camera-container">
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          capture="environment"
          style="display: none"
          @change="handleFileChange"
        />
        <div class="upload-area" @click="triggerFileInput">
          <el-icon v-if="!previewImage" class="upload-icon"><Plus /></el-icon>
          <img v-else :src="previewImage" class="preview-image" />
          <p v-if="!previewImage">点击拍照或选择图片</p>
        </div>
        <div v-if="recognizeResult" class="recognize-result">
          <p><strong>识别结果：</strong>{{ recognizeResult.foodName }}</p>
          <p><strong>预估热量：</strong>{{ recognizeResult.calorie }} kcal/100g</p>
          <p><strong>置信度：</strong>{{ (recognizeResult.confidence * 100).toFixed(2) }}%</p>
          <el-form-item label="食物重量 (g)" style="margin-top: 15px;">
            <el-input-number v-model="recognizeWeight" :min="1" :max="5000" />
          </el-form-item>
          <p><strong>总热量：</strong>{{ Math.round(recognizeResult.calorie * recognizeWeight / 100) }} kcal</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="cameraDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="recognizing" @click="handleRecognize">
          识别
        </el-button>
        <el-button v-if="recognizeResult" type="success" @click="saveRecognizedFood">
          保存记录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Camera, Download } from '@element-plus/icons-vue'
import { getFoodList, addFood, deleteFood } from '@/api/food'
import { recognizeFood } from '@/api/recognition'
import { getCommonFoods, getFoodCategories } from '@/api/commonFood'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const foodList = ref([])
const addDialogVisible = ref(false)
const cameraDialogVisible = ref(false)
const previewImage = ref('')
const recognizing = ref(false)
const recognizeResult = ref(null)
const recognizeWeight = ref(100)
const fileInput = ref(null)

// 常见食物（从后端加载，按分类展示）
const commonFoods = ref([])
const foodCategories = ref([])
const selectedCategory = ref('')

// 当前分类下可选的食物
const filteredFoods = computed(() => {
  if (!selectedCategory.value) {
    return commonFoods.value
  }
  return commonFoods.value.filter((f) => f.category === selectedCategory.value)
})

const foodForm = ref({
  foodName: '',
  weight: 100,
  calorie: 0,
})

// 今日总热量
const todayTotalCalorie = computed(() => {
  return foodList.value.reduce((sum, item) => sum + (item.calorie || 0), 0)
})

// 剩余可摄入热量
const remainingCalorie = computed(() => {
  const goal = userStore.userInfo?.dailyCalorieGoal || 2000
  return goal - todayTotalCalorie.value
})

// 根据食物名称自动计算热量
const calculateCalorie = () => {
  const food = commonFoods.value.find(f => f.foodName === foodForm.value.foodName)
  if (food && foodForm.value.weight) {
    foodForm.value.calorie = Math.round(food.caloriePer100g * foodForm.value.weight / 100)
  }
}

// 食物选择变化时重新计算
const handleFoodChange = () => {
  calculateCalorie()
}

// 分类切换
const handleCategoryChange = () => {
  // 若当前选中的食物不在新分类中，清空选择
  if (selectedCategory.value && foodForm.value.foodName) {
    const stillValid = filteredFoods.value.some(f => f.foodName === foodForm.value.foodName)
    if (!stillValid) {
      foodForm.value.foodName = ''
      foodForm.value.calorie = 0
    }
  }
}

// 加载常见食物数据
const fetchCommonFoods = async () => {
  try {
    const res = await getCommonFoods()
    if (res.code === 200 && res.data) {
      commonFoods.value = res.data
    }
  } catch (error) {
    console.error('加载常见食物失败:', error)
  }
}

// 加载食物分类
const fetchFoodCategories = async () => {
  try {
    const res = await getFoodCategories()
    if (res.code === 200 && res.data) {
      foodCategories.value = res.data
    }
  } catch (error) {
    console.error('加载食物分类失败:', error)
  }
}

const fetchFoodList = async () => {
  loading.value = true
  try {
    console.log('正在获取食物记录...')
    const res = await getFoodList()
    console.log('食物记录响应:', res)
    if (res.code === 200) {
      // 只显示最近一个月的数据
      const oneMonthAgo = new Date()
      oneMonthAgo.setMonth(oneMonthAgo.getMonth() - 1)
      
      foodList.value = (res.data || [])
        .filter(item => new Date(item.recordTime) >= oneMonthAgo)
        .sort((a, b) => new Date(b.recordTime) - new Date(a.recordTime))
      console.log('获取到记录数量:', foodList.value.length)
      // 空数据不报错
      if (foodList.value.length === 0) {
        console.log('暂无饮食记录')
      }
    } else {
      console.warn('获取数据失败:', res.msg)
      foodList.value = []
    }
  } catch (error) {
    console.error('获取食物记录失败:', error)
    foodList.value = []
    // 不显示错误提示，静默处理
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  foodForm.value = {
    foodName: '',
    weight: 100,
    calorie: 0,
  }
  selectedCategory.value = ''
  addDialogVisible.value = true
}

const handleAdd = async () => {
  if (!foodForm.value.foodName) {
    ElMessage.warning('请输入食物名称')
    return
  }
  
  try {
    const res = await addFood({
      foodName: foodForm.value.foodName,
      weight: foodForm.value.weight,
      calorie: foodForm.value.calorie,
    })
    if (res.code === 200) {
      ElMessage.success('添加成功')
      addDialogVisible.value = false
      fetchFoodList()
    }
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

// 导出数据
const exportData = () => {
  // 创建 CSV 内容
  const headers = ['序号', '食物名称', '重量(g)', '热量(kcal)', '记录时间']
  const rows = foodList.value.map((item, index) => [
    index + 1,
    item.foodName,
    item.weight || '-',
    item.calorie,
    new Date(item.recordTime).toLocaleString('zh-CN')
  ])
  
  const csvContent = [
    headers.join(','),
    ...rows.map(row => row.join(','))
  ].join('\n')
  
  // 添加 BOM 以支持中文
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
  
  // 下载文件
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.href = url
  link.download = `饮食记录_${new Date().toLocaleDateString('zh-CN')}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('导出成功')
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除这条记录吗？', '提示', {
      type: 'warning',
    })
    const res = await deleteFood(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchFoodList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const showCameraDialog = () => {
  cameraDialogVisible.value = true
  previewImage.value = ''
  recognizeResult.value = null
  recognizeWeight.value = 100
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      previewImage.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

const handleRecognize = async () => {
  if (!previewImage.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  
  recognizing.value = true
  try {
    const res = await recognizeFood(previewImage.value)
    if (res.code === 200) {
      recognizeResult.value = res.data
      ElMessage.success('识别成功')
    }
  } catch (error) {
    ElMessage.error('识别失败')
  } finally {
    recognizing.value = false
  }
}

const saveRecognizedFood = async () => {
  if (!recognizeResult.value) return
  
  try {
    const totalCalorie = Math.round(recognizeResult.value.calorie * recognizeWeight.value / 100)
    const res = await addFood({
      foodName: recognizeResult.value.foodName,
      weight: recognizeWeight.value,
      calorie: totalCalorie,
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      cameraDialogVisible.value = false
      fetchFoodList()
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchFoodList()
  fetchCommonFoods()
  fetchFoodCategories()
})
</script>

<style scoped>
.food-page {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.food-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.unit {
  margin-left: 10px;
  color: #909399;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}

.camera-container {
  text-align: center;
}

.upload-area {
  width: 100%;
  height: 300px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-area:hover {
  border-color: #409eff;
}

.upload-icon {
  font-size: 48px;
  color: #909399;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.recognize-result {
  margin-top: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.recognize-result p {
  margin-bottom: 10px;
}
</style>
