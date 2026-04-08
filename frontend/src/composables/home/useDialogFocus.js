import { nextTick } from 'vue'

export function useDialogFocus() {
  let lastActiveElement = null

  const focusFirst = () => {
    const dialogCandidates = Array.from(document.querySelectorAll('.el-dialog'))
      .filter(dialogEl => typeof dialogEl?.querySelector === 'function')
      .filter(dialogEl => dialogEl.getClientRects().length > 0)

    const dialogEl = dialogCandidates[dialogCandidates.length - 1]
    if (!dialogEl) {
      return
    }

    const selector = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    const firstFocusable = dialogEl.querySelector(selector)
    if (firstFocusable?.focus) {
      firstFocusable.focus()
    }
  }

  const handleDialogOpen = () => {
    lastActiveElement = document.activeElement
    nextTick(() => {
      focusFirst()
    })
  }

  const handleDialogClose = () => {
    if (typeof lastActiveElement?.focus === 'function') {
      lastActiveElement.focus()
    }
    lastActiveElement = null
  }

  return {
    handleDialogOpen,
    handleDialogClose
  }
}
