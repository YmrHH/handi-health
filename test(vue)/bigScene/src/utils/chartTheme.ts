import type { EChartsOption } from 'echarts'

export function baseGrid() {
  return { left: 36, right: 20, top: 34, bottom: 24, containLabel: true }
}

export function axisStyle() {
  return {
    axisLine: { show: false, lineStyle: { color: 'rgba(114,180,205,0.2)' } },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(92,130,156,0.88)', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgba(114,180,205,0.1)' } }
  }
}

export function tooltipStyle(): EChartsOption['tooltip'] {
  return {
    trigger: 'axis',
    backgroundColor: 'rgba(250,254,255,0.9)',
    borderColor: 'rgba(255,255,255,0.22)',
    borderWidth: 0,
    padding: [8, 10],
    textStyle: { color: 'rgba(39,85,113,0.92)', fontSize: 11 },
    extraCssText: 'backdrop-filter: blur(12px);-webkit-backdrop-filter: blur(12px);box-shadow:0 12px 28px rgba(0,103,96,0.08);border-radius:10px;'
  }
}

export function legendStyle(): EChartsOption['legend'] {
  return {
    bottom: 0,
    itemWidth: 10,
    itemHeight: 6,
    textStyle: { color: 'rgba(92,130,156,0.88)', fontSize: 10 }
  }
}

export function colorPalette() {
  return ['#4fa8c7', '#68aad2', '#5fc7d8', '#8cbce3', '#9db7c7', '#e6bc79', '#f0b36a', '#78c4a0', '#ee8d99']
}

