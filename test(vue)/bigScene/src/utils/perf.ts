export function rafThrottle<T extends (...args: any[]) => void>(fn: T): T {
  let raf = 0
  let lastArgs: any[] | null = null
  const wrapped = ((...args: any[]) => {
    lastArgs = args
    if (raf) return
    raf = requestAnimationFrame(() => {
      raf = 0
      const a = lastArgs
      lastArgs = null
      if (a) fn(...a)
    })
  }) as T
  return wrapped
}

export function scheduleIdle(cb: () => void, timeout = 700) {
  const w = window as any
  if (typeof w.requestIdleCallback === 'function') {
    const id = w.requestIdleCallback(
      () => cb(),
      { timeout }
    )
    return () => w.cancelIdleCallback?.(id)
  }
  const t = window.setTimeout(cb, Math.min(timeout, 700))
  return () => window.clearTimeout(t)
}

