export function buildQueryParams(source) {
  const params = new URLSearchParams()
  Object.entries(source || {}).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '' || value === 'all') {
      return
    }
    params.append(key, value)
  })
  return params.toString()
}

export function resolveDownloadFileName(disposition, fallbackFileName) {
  if (!disposition) {
    return fallbackFileName
  }

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch (error) {
      return utf8Match[1]
    }
  }

  const asciiMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (asciiMatch?.[1]) {
    return asciiMatch[1]
  }

  return fallbackFileName
}
