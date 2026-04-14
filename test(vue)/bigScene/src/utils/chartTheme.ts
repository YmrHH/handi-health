import type { EChartsOption } from 'echarts'

export function baseGrid() {
  return { left: 34, right: 18, top: 32, bottom: 22, containLabel: true }
}

export function axisStyle() {
  return {
    axisLine: { show: false, lineStyle: { color: 'rgba(114,180,205,0.09)' } },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(92,130,156,0.68)', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(114,180,205,0.04)' } }
  }
}

export function tooltipStyle(): EChartsOption['tooltip'] {
  return {
    trigger: 'axis',
    backgroundColor: 'rgba(248,253,255,0.66)',
    borderColor: 'rgba(255,255,255,0.06)',
    borderWidth: 0,
    padding: [8, 10],
    textStyle: { color: 'rgba(39,85,113,0.84)', fontSize: 10 },
    extraCssText: 'backdrop-filter: blur(12px);-webkit-backdrop-filter: blur(12px);box-shadow:0 8px 18px rgba(79,153,182,0.05);border-radius:12px;'
  }
}

export function legendStyle(): EChartsOption['legend'] {
  return {
    bottom: 0,
    itemWidth: 7,
    itemHeight: 4,
    itemGap: 8,
    textStyle: { color: 'rgba(92,130,156,0.66)', fontSize: 9 }
  }
}

export function colorPalette() {
  return ['#2aa9c9', '#62b6d8', '#7ecce0', '#9ac7de', '#9db7c7', '#e7b979', '#efb487', '#71c8a1', '#ee9ca6']
}

