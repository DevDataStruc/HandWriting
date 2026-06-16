/**
 * 通用下载工具：把 dataURL / Blob / URL 下载到本地
 */

export function triggerDownload(url: string, filename: string): void {
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

export function downloadDataURL(dataUrl: string, filename: string): void {
  triggerDownload(dataUrl, filename)
}

export async function downloadBlob(blob: Blob, filename: string): Promise<void> {
  const url = URL.createObjectURL(blob)
  try {
    triggerDownload(url, filename)
  } finally {
    // 立即释放
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
}

/** 把一个 PNG URL 包装为 <svg> 字符串（用于"导出 SVG"） */
export async function pngUrlToSvgString(
  pngUrl: string,
  width?: number,
  height?: number
): Promise<string> {
  // 加载图片以获取真实尺寸
  const img = await loadImage(pngUrl)
  const w = width ?? img.naturalWidth ?? 600
  const h = height ?? img.naturalHeight ?? 400
  const encoded = await imageToDataUrl(img)
  return `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><image width="${w}" height="${h}" xlink:href="${encoded}" /></svg>`
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = (e) => reject(e)
    img.src = src
  })
}

function imageToDataUrl(img: HTMLImageElement): Promise<string> {
  return new Promise((resolve, reject) => {
    try {
      const canvas = document.createElement('canvas')
      canvas.width = img.naturalWidth
      canvas.height = img.naturalHeight
      const ctx = canvas.getContext('2d')
      if (!ctx) return reject(new Error('canvas 2d context unavailable'))
      ctx.drawImage(img, 0, 0)
      resolve(canvas.toDataURL('image/png'))
    } catch (err) {
      reject(err)
    }
  })
}

/** 把字符串当作 SVG 文件下载 */
export function downloadSvgString(svg: string, filename: string): void {
  const blob = new Blob([svg], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  triggerDownload(url, filename)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
