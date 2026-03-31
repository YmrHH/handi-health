import type { EChartsOption } from 'echarts'

export function baseGrid() {
  return { left: 42, right: 18, top: 36, bottom: 28 }
}

export function axisStyle() {
  return {
    axisLine: { lineStyle: { color: 'rgba(114,180,205,0.42)' } },
    axisLabel: { color: 'rgba(39,85,113,0.92)' },
    splitLine: { lineStyle: { color: 'rgba(158,169,230,0.22)' } }
  }
}

export function tooltipStyle(): EChartsOption['tooltip'] {
  return {
    trigger: 'axis',
    backgroundColor: 'rgba(255,255,255,0.92)',
    borderColor: 'rgba(114,180,205,0.34)',
    borderWidth: 1,
    textStyle: { color: 'rgba(20,52,79,0.96)' }
  }
}

export function legendStyle(): EChartsOption['legend'] {
  return {
    bottom: 0,
    textStyle: { color: 'rgba(39,85,113,0.92)', fontSize: 11 }
  }
}

export function colorPalette() {
  return ['#5fc7d8', '#7fd6e3', '#8cbce3', '#9ea9e6', '#e6bc79', '#ee8d99', '#78c4a0', '#efc56f']
}

