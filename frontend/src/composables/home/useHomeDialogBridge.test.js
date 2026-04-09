import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  injectMock: vi.fn(),
  watchMock: vi.fn(),
  handleDialogOpen: vi.fn(),
  handleDialogClose: vi.fn()
}))

vi.mock('vue', async () => {
  const actual = await vi.importActual('vue')
  return {
    ...actual,
    inject: mocks.injectMock,
    watch: mocks.watchMock
  }
})

vi.mock('./useDialogFocus.js', () => ({
  useDialogFocus: () => ({
    handleDialogOpen: mocks.handleDialogOpen,
    handleDialogClose: mocks.handleDialogClose
  })
}))

import { useHomeDialogBridge } from './useHomeDialogBridge.js'

function createHomeContext() {
  const data = {
    productDialogVisible: false,
    productDialogTitle: '新增产品',
    userDialogVisible: false,
    userDialogTitle: '新增用户',
    noticeDialogVisible: false,
    rechargeDialogVisible: false
  }

  const target = {
    $data: data,
    dialogRefs: {},
    openProductDialog: vi.fn(),
    openUserDialog: vi.fn()
  }

  return new Proxy(target, {
    ownKeys() {
      return ['dialogRefs', '$data', 'openProductDialog', 'openUserDialog']
    },
    get(t, prop, receiver) {
      if (prop in t) {
        return Reflect.get(t, prop, receiver)
      }
      if (prop in data) {
        return data[prop]
      }
      return undefined
    },
    set(t, prop, value, receiver) {
      if (prop in data) {
        data[prop] = value
        return true
      }
      return Reflect.set(t, prop, value, receiver)
    },
    getOwnPropertyDescriptor(t, prop) {
      if (prop === 'dialogRefs' || prop === '$data' || prop === 'openProductDialog' || prop === 'openUserDialog') {
        return {
          configurable: true,
          enumerable: true,
          writable: true,
          value: t[prop]
        }
      }
      return undefined
    }
  })
}

describe('useHomeDialogBridge', () => {
  beforeEach(() => {
    mocks.injectMock.mockReset()
    mocks.watchMock.mockReset()
    mocks.handleDialogOpen.mockReset()
    mocks.handleDialogClose.mockReset()
  })

  it('会把 Home 的 data 字段桥接到弹窗组件', () => {
    const home = createHomeContext()
    mocks.injectMock.mockReturnValue(home)

    const { bridge } = useHomeDialogBridge()

    expect(bridge.productDialogVisible).toBe(false)
    expect(bridge.productDialogTitle).toBe('新增产品')
    expect(bridge.userDialogVisible).toBe(false)
    expect(bridge.userDialogTitle).toBe('新增用户')
    expect(bridge.noticeDialogVisible).toBe(false)
    expect(bridge.rechargeDialogVisible).toBe(false)
    expect(bridge.openProductDialog).toBe(home.openProductDialog)

    bridge.productDialogVisible = true
    bridge.userDialogVisible = true

    expect(home.productDialogVisible).toBe(true)
    expect(home.$data.productDialogVisible).toBe(true)
    expect(home.userDialogVisible).toBe(true)
    expect(home.$data.userDialogVisible).toBe(true)
  })
})
