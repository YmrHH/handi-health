import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
  TitleComponent
} from 'echarts/components'
import { BarChart, GaugeChart, LineChart, PieChart, RadarChart } from 'echarts/charts'

// 按需注册：仅注册当前项目真实使用的图表/组件/渲染器
use([CanvasRenderer, GridComponent, TooltipComponent, LegendComponent, TitleComponent, BarChart, LineChart, PieChart, GaugeChart, RadarChart])

export { init, graphic, type ECharts, type EChartsOption } from 'echarts/core'

