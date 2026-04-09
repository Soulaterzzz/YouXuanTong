import { inject, ref, watch } from 'vue'
import { useDialogFocus } from './useDialogFocus.js'

function defineBridgeProperty(target, key, getter, setter) {
  Object.defineProperty(target, key, {
    enumerable: true,
    configurable: true,
    get: getter,
    set: setter
  })
}

function shouldSkipKey(key) {
  return typeof key === 'string' && (key.startsWith('$') || key.startsWith('_'))
}

function bridgeSourceKeys(source, bridge, home) {
  if (!source) {
    return
  }

  Reflect.ownKeys(source).forEach((key) => {
    if (shouldSkipKey(key)) {
      return
    }
    if (key === 'dialogRef' || key === 'handleDialogOpen' || key === 'handleDialogClose') {
      return
    }
    if (Object.prototype.hasOwnProperty.call(bridge, key)) {
      return
    }

    defineBridgeProperty(
      bridge,
      key,
      () => home[key],
      (value) => {
        home[key] = value
      }
    )
  })
}

export function useHomeDialogBridge(syncRefKeys = []) {
  const home = inject('homeDialogContext')
  if (!home) throw new Error('Home dialog bridge requires homeDialogContext')
  const parentRefs = home.dialogRefs || (home.dialogRefs = {})
  const dialogRef = ref(null)

  const { handleDialogOpen, handleDialogClose } = useDialogFocus()
  const syncRefs = Object.fromEntries(syncRefKeys.map(key => [key, ref(null)]))

  const syncParentRef = (key, value) => {
    parentRefs[key] = value
  }

  syncRefKeys.forEach((key) => {
    watch(syncRefs[key], value => syncParentRef(key, value), { immediate: true })
  })

  const bridge = {
    dialogRef,
    handleDialogOpen,
    handleDialogClose
  }

  syncRefKeys.forEach((key) => {
    defineBridgeProperty(
      bridge,
      key,
      () => syncRefs[key],
      (value) => {
        syncRefs[key].value = value
      }
    )
  })

  bridgeSourceKeys(home, bridge, home)
  bridgeSourceKeys(home.$data, bridge, home)

  return {
    home,
    dialogRef,
    handleDialogOpen,
    handleDialogClose,
    bridge,
    ...syncRefs
  }
}
