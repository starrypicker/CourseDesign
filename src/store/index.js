
import { createStore } from 'vuex'

const store = createStore({
  state: {
    // 用户信息
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    token: localStorage.getItem('token') || '',
    role: localStorage.getItem('role') || '',
    // 购物车
    cart: JSON.parse(localStorage.getItem('cart') || '[]')
  },

  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.role === 'admin',
    userInfo: state => state.userInfo,
    cart: state => state.cart,
    cartCount: state => state.cart.reduce((sum, item) => sum + item.quantity, 0),
    cartTotal: state => state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)
  },

  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    SET_ROLE(state, role) {
      state.role = role
      localStorage.setItem('role', role)
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    // 购物车相关
    ADD_TO_CART(state, product) {
      const exist = state.cart.find(item => item.id === product.id)
      if (exist) {
        exist.quantity += 1
      } else {
        state.cart.push({ ...product, quantity: 1 })
      }
      localStorage.setItem('cart', JSON.stringify(state.cart))
    },
    REMOVE_FROM_CART(state, productId) {
      state.cart = state.cart.filter(item => item.id !== productId)
      localStorage.setItem('cart', JSON.stringify(state.cart))
    },
    UPDATE_CART_QUANTITY(state, { productId, quantity }) {
      const item = state.cart.find(i => i.id === productId)
      if (item) {
        item.quantity = quantity
        if (quantity <= 0) {
          state.cart = state.cart.filter(i => i.id !== productId)
        }
      }
      localStorage.setItem('cart', JSON.stringify(state.cart))
    },
    CLEAR_CART(state) {
      state.cart = []
      localStorage.setItem('cart', JSON.stringify(state.cart))
    },
    // 退出登录
    LOGOUT(state) {
      state.token = ''
      state.role = ''
      state.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('userInfo')
    }
  },

  actions: {
    login({ commit }, { token, role, userInfo }) {
      commit('SET_TOKEN', token)
      commit('SET_ROLE', role)
      commit('SET_USER_INFO', userInfo)
    },
    logout({ commit }) {
      commit('LOGOUT')
      commit('CLEAR_CART')
    },
    addToCart({ commit }, product) {
      commit('ADD_TO_CART', product)
    },
    removeFromCart({ commit }, productId) {
      commit('REMOVE_FROM_CART', productId)
    },
    updateCartQuantity({ commit }, payload) {
      commit('UPDATE_CART_QUANTITY', payload)
    },
    clearCart({ commit }) {
      commit('CLEAR_CART')
    }
  }
})

export default store
