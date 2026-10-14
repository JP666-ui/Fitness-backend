import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo } from '@/api/user'
import { setToken, getToken, removeToken } from '@/utils/auth'

const USER_INFO_KEY = 'fitness_user_info'

/** 从本地缓存读取用户信息（刷新页面后不丢失） */
function loadCachedUserInfo() {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  // State —— 初始化时直接从缓存恢复，避免刷新后空白
  const token = ref(getToken())
  const userInfo = ref(loadCachedUserInfo())

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || '')

  // Actions
  const setUserInfo = (info) => {
    userInfo.value = info
    // 同步写入缓存
    try {
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
    } catch {
      // 忽略存储异常（如隐私模式）
    }
  }

  const loginAction = async (loginData) => {
    const res = await login(loginData)
    if (res.code === 200) {
      token.value = res.data.token
      setToken(res.data.token)
      setUserInfo(res.data.user)
      return true
    }
    return false
  }

  /**
   * 拉取最新用户信息
   * @param {boolean} force 是否强制从服务端刷新
   */
  const getUserInfoAction = async (force = false) => {
    // 已有缓存且不强制刷新时，直接复用，避免重复请求
    if (!force && userInfo.value && userInfo.value.id) {
      return userInfo.value
    }

    try {
      const res = await getUserInfo()
      if (res.code === 200 && res.data) {
        setUserInfo(res.data)
        return res.data
      }
    } catch (error) {
      // 请求失败时保留缓存数据，不让页面变空
      console.warn('获取用户信息失败，使用本地缓存:', error?.message)
    }
    return userInfo.value
  }

  const logout = () => {
    token.value = null
    userInfo.value = null
    removeToken()
    try {
      localStorage.removeItem(USER_INFO_KEY)
    } catch {
      // 忽略
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    nickname,
    setUserInfo,
    loginAction,
    getUserInfoAction,
    logout,
  }
})
