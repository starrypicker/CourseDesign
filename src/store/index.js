import { createStore } from 'vuex'

export default createStore({
  state: {
    // 用户信息
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    // 购物车
    cart: JSON.parse(localStorage.getItem('cart') || '[]')
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    SET_USER_INFO(state, info) {
      state.userInfo = info
      localStorage.setItem('userInfo', JSON.stringify(info))
    },
    CLEAR_USER(state) {
      state.token = ''
      state.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },
    // ========== 购物车 ==========
    ADD_TO_CART(state, item) {
      const existing = state.cart.find(i => i.productCode === item.productCode)
      if (existing) {
        existing.quantity += item.quantity || 1
      } else {
        state.cart.push({ ...item, quantity: item.quantity || 1 })
      }
      localStorage.setItem('cart', JSON.stringify(state.cart))
    },
    UPDATE_CART_QUANTITY(state, { productCode, quantity }) {
      const item = state.cart.find(i => i.productCode === productCode)
      if (item) item.quantity = quantity
      localStorage.setItem('cart', JSON.stringify(state.cart))
    },
    REMOVE_FROM_CART(state, productCode) {
      state.cart = state.cart.filter(i => i.productCode !== productCode)
      localStorage.setItem('cart', JSON.stringify(state.cart))
    },
    CLEAR_CART(state) {
      state.cart = []
      localStorage.removeItem('cart')
    }
  },
  actions: {
    // 登录成功（名称改为 login，与 LoginForm.vue 一致）
    login({ commit }, { token, userInfo }) {
      commit('SET_TOKEN', token)
      commit('SET_USER_INFO', userInfo)
    },
    // 登出
    logout({ commit }) {
      commit('CLEAR_USER')
    },
    // ========== 购物车 ==========
    addToCart({ commit }, item) {
      commit('ADD_TO_CART', item)
    },
    updateCartQuantity({ commit }, payload) {
      commit('UPDATE_CART_QUANTITY', payload)
    },
    removeFromCart({ commit }, productCode) {
      commit('REMOVE_FROM_CART', productCode)
    },
    clearCart({ commit }) {
      commit('CLEAR_CART')
    }
  },
  getters: {
    isLoggedIn: state => !!state.token,
    userInfo: state => state.userInfo,
    // ========== 购物车 ==========
    cart: state => state.cart,
    cartCount: state => state.cart.reduce((sum, i) => sum + i.quantity, 0),
    cartTotal: state => state.cart.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0)
  }
})