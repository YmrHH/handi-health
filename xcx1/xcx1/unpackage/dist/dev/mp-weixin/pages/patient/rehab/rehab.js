"use strict";
const common_vendor = require("../../../common/vendor.js");
const api_patient = require("../../../api/patient.js");
const utils_session = require("../../../utils/session.js");
const _sfc_main = {
  data() {
    return {
      filters: [
        {
          value: "all",
          label: "全部"
        },
        {
          value: "todo",
          label: "待完成"
        },
        {
          value: "done",
          label: "已完成"
        }
      ],
      activeFilter: "todo",
      summary: {
        total: 0,
        done: 0,
        streak: 3,
        // 连续打卡天数 demo
        overdue: 0
      },
      tasks: [],
      visibleTasks: []
    };
  },
  onLoad() {
    this.fetchTasksFromServer();
  },
  methods: {
    // 切换筛选
    onChangeFilter(e) {
      const value = e.currentTarget.dataset.value;
      if (value === this.activeFilter) {
        return;
      }
      this.setData(
        {
          activeFilter: value
        },
        () => {
          this.updateVisibleTasks();
        }
      );
    },
    // 标记完成/取消完成
    onToggleDone(e) {
      const id = e.currentTarget.dataset.id;
      const tasks = this.tasks.map((item) => {
        if (item.id === id) {
          item.status = item.status === "done" ? "todo" : "done";
        }
        return item;
      });
      const cur = tasks.find((t) => t.id === id);
      this.setData(
        {
          tasks
        },
        () => {
          this.updateSummary();
          this.updateVisibleTasks();
          this.saveTaskStatus(id, cur ? cur.status : void 0);
        }
      );
    },
    // 选择难度
    onSetDifficulty(e) {
      const { id, level } = e.currentTarget.dataset;
      const tasks = this.tasks.map((item) => {
        if (item.id === id) {
          item.difficulty = level;
        }
        return item;
      });
      this.setData(
        {
          tasks
        },
        () => {
          this.saveTaskDifficulty(id, level);
        }
      );
    },
    // ===== 数据与统计 ===== //
    fetchTasksFromServer() {
      const now = /* @__PURE__ */ new Date();
      const pad = (n) => n < 10 ? "0" + n : "" + n;
      const dateStr = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
      api_patient.getRehabTasks({ date: dateStr }).then((res) => {
        const list = this.normalizeTasks(Array.isArray(res) ? res : res && res.tasks || []);
        const streak = (res && res.streak) != null ? res.streak : this.summary.streak;
        const overdue = list.filter((item) => item.overdue && item.status !== "done").length;
        this.setData(
          {
            tasks: list,
            summary: { ...this.summary, streak, overdue }
          },
          () => {
            this.updateSummary();
            this.updateVisibleTasks();
            if (overdue > 0) {
              common_vendor.index.showToast({ title: `有 ${overdue} 项任务已超时`, icon: "none" });
            }
          }
        );
      }).catch(() => {
        {
          this.setData({ tasks: [] }, () => {
            this.updateSummary();
            this.updateVisibleTasks();
          });
          return;
        }
      });
    },
    updateSummary() {
      const total = this.tasks.length;
      const done = this.tasks.filter((t) => t.status === "done").length;
      const overdue = this.tasks.filter((t) => t.status !== "done" && t.overdue).length;
      const nextDue = this.tasks.filter((t) => t.status !== "done" && t.dueTime).map((t) => t.dueTime).sort()[0] || "";
      const summary = {
        ...this.summary,
        total,
        done,
        overdue,
        next: nextDue
      };
      this.setData({ summary });
      utils_session.setScopedStorageSync("rehab_summary", summary);
      utils_session.setScopedStorageSync("rehab_tasks_today", {
        date: (/* @__PURE__ */ new Date()).toISOString().slice(0, 10),
        tasks: this.tasks,
        summary
      });
    },
    updateVisibleTasks() {
      const { tasks, activeFilter } = this;
      let list = tasks;
      if (activeFilter === "todo") {
        list = tasks.filter((t) => t.status !== "done");
      } else if (activeFilter === "done") {
        list = tasks.filter((t) => t.status === "done");
      }
      this.setData({
        visibleTasks: list
      });
    },
    normalizeTasks(list) {
      const now = /* @__PURE__ */ new Date();
      const currentMinutes = now.getHours() * 60 + now.getMinutes();
      return (Array.isArray(list) ? list : []).map((item, index) => {
        const dueText = item.dueTime || item.ext4 || "";
        let dueMinutes = null;
        if (dueText && /^\d{1,2}:\d{2}$/.test(dueText)) {
          const parts = dueText.split(":");
          dueMinutes = Number(parts[0]) * 60 + Number(parts[1]);
        }
        const statusRaw = String(item.status || "").toLowerCase();
        const status = statusRaw === "done" || statusRaw === "completed" ? "done" : "todo";
        const overdue = status !== "done" && dueMinutes != null && currentMinutes > dueMinutes;
        return {
          id: item.id != null ? item.id : "task_" + index,
          icon: item.icon || item.ext2 || (String(item.title || "").includes("饮食") ? "🥗" : "🚶"),
          title: item.title || "今日康养任务",
          category: item.category || item.ext1 || "康养",
          duration: Number(item.duration || item.ext3 || 15),
          desc: item.desc || item.description || "请按建议完成今日康养安排。",
          status,
          difficulty: item.difficulty || "just",
          dueTime: dueText || "20:00",
          deadlineText: item.deadlineText || (overdue ? "已超过建议完成时间" : `建议在 ${dueText || "20:00"} 前完成`),
          overdue
        };
      });
    },
    // 保存任务状态
    saveTaskStatus(id, status) {
      if (!id)
        return;
      if (!status)
        return;
      api_patient.updateRehabTaskStatus(id, status).catch(() => {
        {
          common_vendor.index.showToast({ title: "同步任务状态失败", icon: "none" });
        }
      });
    },
    // 保存难度反馈
    saveTaskDifficulty(id, level) {
      if (!id)
        return;
      api_patient.updateRehabTaskDifficulty(id, level).catch(() => {
        {
          common_vendor.index.showToast({ title: "同步难度反馈失败", icon: "none" });
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.summary.done),
    b: common_vendor.t($data.summary.total),
    c: common_vendor.t($data.summary.streak),
    d: common_vendor.f($data.filters, (item, index, i0) => {
      return {
        a: common_vendor.t(item.label),
        b: item.value,
        c: common_vendor.n("filter-tab " + ($data.activeFilter === item.value ? "active" : "")),
        d: common_vendor.o((...args) => $options.onChangeFilter && $options.onChangeFilter(...args), index),
        e: index
      };
    }),
    e: $data.summary.overdue > 0
  }, $data.summary.overdue > 0 ? {
    f: common_vendor.t($data.summary.overdue)
  } : {}, {
    g: common_vendor.f($data.visibleTasks, (item, index, i0) => {
      return {
        a: common_vendor.t(item.icon),
        b: common_vendor.t(item.title),
        c: common_vendor.t(item.category),
        d: common_vendor.t(item.duration),
        e: common_vendor.t(item.desc),
        f: common_vendor.t(item.deadlineText || "建议在 " + (item.dueTime || "20:00") + " 前完成"),
        g: common_vendor.n(item.overdue ? "task-deadline overdue" : "task-deadline"),
        h: common_vendor.t(item.status === "done" ? "已完成" : item.overdue ? "已超时" : "待完成"),
        i: common_vendor.n("status-tag " + (item.status === "done" ? "status-done" : item.overdue ? "status-overdue" : "status-todo")),
        j: common_vendor.n("diff-btn " + (item.difficulty === "hard" ? "active" : "")),
        k: item.id,
        l: common_vendor.o((...args) => $options.onSetDifficulty && $options.onSetDifficulty(...args), index),
        m: common_vendor.n("diff-btn " + (item.difficulty === "just" ? "active" : "")),
        n: item.id,
        o: common_vendor.o((...args) => $options.onSetDifficulty && $options.onSetDifficulty(...args), index),
        p: common_vendor.n("diff-btn " + (item.difficulty === "easy" ? "active" : "")),
        q: item.id,
        r: common_vendor.o((...args) => $options.onSetDifficulty && $options.onSetDifficulty(...args), index),
        s: common_vendor.t(item.status === "done" ? "修改状态" : "标记完成"),
        t: common_vendor.n("task-btn " + (item.status === "done" ? "task-btn-done" : "")),
        v: item.id,
        w: common_vendor.o((...args) => $options.onToggleDone && $options.onToggleDone(...args), index),
        x: index
      };
    }),
    h: $data.visibleTasks.length === 0
  }, $data.visibleTasks.length === 0 ? {} : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render]]);
wx.createPage(MiniProgramPage);
//# sourceMappingURL=../../../../.sourcemap/mp-weixin/pages/patient/rehab/rehab.js.map
