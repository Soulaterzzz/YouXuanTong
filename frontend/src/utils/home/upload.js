function buildResult(valid, message = '') {
  return { valid, message }
}

export function validateProductImageFile(file, hasProductId) {
  if (!hasProductId) {
    return buildResult(false, '请先保存产品后再上传图片')
  }

  const isImage = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'].includes(file?.type)
  const isLt10M = (file?.size || 0) / 1024 / 1024 < 10

  if (!isImage) {
    return buildResult(false, '只能上传 JPG/PNG/GIF/WEBP 格式的图片!')
  }
  if (!isLt10M) {
    return buildResult(false, '图片大小不能超过 10MB!')
  }
  return buildResult(true)
}

export function validateProductTemplateFile(file, hasProductId) {
  if (!hasProductId) {
    return buildResult(false, '请先保存产品后再上传模板文件')
  }

  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain'
  ]
  const isAllowed = allowedTypes.includes(file?.type) || /\.(pdf|doc|docx|xls|xlsx|txt)$/i.test(file?.name || '')
  const isLt10M = (file?.size || 0) / 1024 / 1024 < 10

  if (!isAllowed) {
    return buildResult(false, '只能上传 PDF、Word、Excel、TXT 格式的文件!')
  }
  if (!isLt10M) {
    return buildResult(false, '文件大小不能超过 10MB!')
  }
  return buildResult(true)
}

export function validateProductInsuranceBatchFile(file) {
  const isXlsx = /\.(xls|xlsx)$/i.test(file?.name || '')
  const isLt10M = (file?.size || 0) / 1024 / 1024 < 10

  if (!isXlsx) {
    return buildResult(false, '只能上传 .xls 或 .xlsx 格式的Excel文件')
  }
  if (!isLt10M) {
    return buildResult(false, '文件大小不能超过 10MB')
  }
  return buildResult(true)
}
