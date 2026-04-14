import type { EChartsOption } from 'echarts'

export function baseGrid() {
  return { left: 34, right: 18, top: 32, bottom: 22, containLabel: true }
}

export function axisStyle() {
  return {
    axisLine: { show: false, lineStyle: { color: 'rgba(114,180,205,0.09)' } },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(92,130,156,0.74)', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(114,180,205,0.055)' } }
  }
}

export function tooltipStyle(): EChartsOption['tooltip'] {
  return {
    trigger: 'axis',
    backgroundColor: 'rgba(248,253,255,0.78)',
    borderColor: 'rgba(255,255,255,0.1)',
    borderWidth: 0,
    padding: [8, 10],
    textStyle: { color: 'rgba(39,85,113,0.88)', fontSize: 10 },
    extraCssText: 'backdrop-filter: blur(10px);-webkit-backdrop-filter: blur(10px);box-shadow:0 8px 18px rgba(79,153,182,0.07);border-radius:10px;'
  }
}

export function legendStyle(): EChartsOption['legend'] {
  return {
    bottom: 0,
    itemWidth: 8,
    itemHeight: 5,
    itemGap: 10,
    textStyle: { color: 'rgba(92,130,156,0.72)', fontSize: 9 }
  }
}

export function colorPalette() {
  return ['#4fa8c7', '#68aad2', '#5fc7d8', '#8cbce3', '#9db7c7', '#e6bc79', '#f0b36a', '#78c4a0', '#ee8d99']
}

