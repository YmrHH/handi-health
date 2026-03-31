本次修改已经同步到 frontend/src/views/patient/Detail.vue 源码。

如果你当前运行的是开发环境（npm run dev），直接重新启动即可生效。
如果你当前部署直接使用 frontend/dist 静态文件，请重新执行一次前端构建：

npm install
npm run build

原因：当前环境无法联网安装依赖，因此这里只能保证源码已改好；dist 是否立即同步，取决于你本地是否重新构建。
