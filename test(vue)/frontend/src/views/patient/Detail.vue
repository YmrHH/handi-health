<template>
  <div class="patient-detail-page">
    <div class="page-header">
      <!-- 本次为第1步，仅美化标题区和顶部概览区，不改布局。 -->
      <div class="page-title">
        <span class="title-badge" aria-hidden="true">
          <!-- File/User 风格图标（避免引入新依赖） -->
          <svg viewBox="0 0 24 24" fill="none" class="title-badge-icon">
            <path d="M7 3h7l3 3v15a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M14 3v4a2 2 0 0 0 2 2h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M12 13a2.8 2.8 0 1 0-5.6 0A2.8 2.8 0 0 0 12 13Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M6.5 20a6 6 0 0 1 11 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="page-title-text">患者档案与画像</span>
      </div>
      <div class="page-subtitle">展示单个患者的基础信息、风险信息与 AI 分析结果</div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <div v-else-if="detail || profile" class="detail-container">
      <!-- 本次为第1步，仅美化标题区和顶部概览区，不改布局。 -->
      <!-- 顶部摘要条 -->
      <div v-if="detail || profile" class="summary-bar">
        <div class="summary-left">
          <div>
            <div class="summary-name">{{ (profile?.name || detail?.patient?.name) || '--' }}</div>
            <div class="summary-meta">
              <span v-if="detail?.patient?.gender" class="meta-item">
                {{ detail.patient.gender === 'M' ? '男' : (detail.patient.gender === 'F' ? '女' : detail.patient.gender) }}
              </span>
              <span v-if="(profile?.age || detail?.patient?.age) != null" class="meta-item">
                {{ (profile?.age || detail?.patient?.age) }} 岁
              </span>
              <span v-if="detail?.patient?.phone" class="meta-item">{{ detail.patient.phone }}</span>
              <span v-if="(profile?.disease || detail?.patient?.disease)" class="meta-item">
                {{ profile?.disease || detail?.patient?.disease }}
              </span>
              <span v-if="(profile?.syndrome || detail?.patient?.syndrome)" class="meta-item">
                {{ profile?.syndrome || detail?.patient?.syndrome }}
              </span>
            </div>
            <div
              v-if="profile?.mainComplaint"
              class="summary-main-complaint"
            >
              <span class="summary-main-complaint-label">主诉</span>
              <span class="summary-main-complaint-text">
                {{ profile.mainComplaint }}
              </span>
            </div>
          </div>
          <div class="risk-pill-with-text">
            <div class="risk-pill" :class="getRiskClass(profile?.riskLevel || detail?.risk?.riskLevel)">
              {{ profile?.riskText || getRiskText(detail?.risk?.riskLevel) || '风险未知' }}
            </div>
            <div class="risk-action-text">
              {{ getRiskActionText(profile?.riskLevel || detail?.risk?.riskLevel) }}
            </div>
          </div>
        </div>
        <div class="summary-actions">
          <button class="btn btn-ghost" @click="handleBack">← 返回</button>
          <RouterLink v-if="route.query.from === 'followup'" 
                      :to="{ name: 'FollowupWorkbench' }" 
                      class="btn btn-ghost">
            返回进行主动随访
          </RouterLink>
          <RouterLink v-else :to="{ name: 'PatientList' }" class="btn btn-ghost">
            返回患者档案管理
          </RouterLink>
        </div>
      </div>

      <!-- 顶部统计卡（仅在详情数据存在时显示） -->
      <div v-if="detail" class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">待随访任务数</div>
          <div class="stat-value">
            <span>{{ detail.stats?.pendingCount ?? 0 }}</span>
            <span class="stat-badge">PENDING</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">超期任务数</div>
          <div class="stat-value">
            <span>{{ detail.stats?.overdueCount ?? 0 }}</span>
            <span class="stat-badge danger">OVERDUE</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">最近随访时间</div>
          <div class="stat-value">
            <span>{{ detail.stats?.lastFollowupTime || '--' }}</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">近7天告警数</div>
          <div class="stat-value">
            <span>{{ detail.stats?.alert7d ?? 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 第一排三块：基本信息 / 诊断与证型 / 风险与责任人 -->
      <div class="card-grid cols-3">
        <div class="card">
          <div class="card-header-row">
            <span class="card-title">基本信息</span>
            <button type="button" class="btn-mini" @click="openBasicInfoEdit">编辑</button>
          </div>
          <div class="info-item">
            <span class="info-label">姓名：</span>
            <span class="info-value">{{ (profile?.name || detail?.patient?.name) || '——' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">性别：</span>
            <span class="info-value">
              {{ getGenderText(profile ? null : detail?.patient?.gender) }}，{{ (profile?.age || detail?.patient?.age) || '——' }} 岁
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">病案号：</span>
            <span class="info-value">{{ formatCaseId(profile?.patientId || detail?.patient?.id) }}</span>
          </div>
          <div v-if="detail?.patient?.idCard" class="info-item">
            <span class="info-label">身份证：</span>
            <span class="info-value">{{ maskId(detail.patient.idCard) }}</span>
          </div>
          <div v-if="detail?.patient?.phone" class="info-item">
            <span class="info-label">手机号：</span>
            <span class="info-value">{{ detail.patient.phone }}</span>
          </div>
          <div v-if="detail?.patient?.address" class="info-item">
            <span class="info-label">住址：</span>
            <span class="info-value">{{ detail.patient.address }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">家族史 / 遗传病史：</span>
            <span class="info-value">
              {{ profile?.familyHistory || detail?.patient?.familyHistory || '——' }}
            </span>
          </div>
        </div>

        <div class="card">
          <div class="card-header-row">
            <span class="card-title">诊断与证型</span>
            <button type="button" class="btn-mini" @click="openDiagnosisEdit">编辑</button>
          </div>
          <div class="info-item">
            <span class="info-label">主病种：</span>
            <span class="info-value">
              <span class="tag">
                {{ (profile?.disease || detail?.patient?.disease) || '——' }}
              </span>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">证型：</span>
            <span class="info-value">
              <span class="tag tag-tcm">
                {{ (profile?.syndrome || detail?.patient?.syndrome) || '——' }}
              </span>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">体质：</span>
            <span class="info-value">
              <span class="tag tag-plain">
                {{ profile?.constitution || detail?.patient?.constitution || '——' }}
              </span>
            </span>
          </div>
        </div>

        <div class="card risk-owner-card">
          <div class="card-header-row">
            <span class="card-title">风险与责任人</span>
            <button type="button" class="btn-mini" @click="openRiskOwnerEdit">编辑</button>
          </div>
          <div class="info-item">
            <span class="info-label">当前风险：</span>
            <span :class="getRiskLevelClass(profile?.riskLevel || detail?.risk?.riskLevel)">
              {{ profile?.riskText || getRiskText(detail?.risk?.riskLevel) || '——' }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">责任医生：</span>
            <span class="info-value doctor-badge">{{ profile?.doctor || detail?.patient?.responsibleDoctor || '——' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">最近预警：</span>
            <span class="info-value">{{ profile?.lastAlertText || '——' }}</span>
          </div>
        </div>
      </div>

      <!-- 编辑：风险与责任人 -->
      <Teleport to="body">
        <div v-if="showRiskOwnerEdit" class="modal-overlay" @click="closeRiskOwnerEdit">
          <div class="modal-content edit-modal" @click.stop>
            <div class="modal-header">
              <h3>编辑 · 风险与责任人</h3>
              <button type="button" class="modal-close" @click="closeRiskOwnerEdit">×</button>
            </div>
            <div class="modal-body">
              <div class="edit-form">
                <div class="form-row">
                  <div class="form-label">当前风险等级</div>
                  <select v-model="riskOwnerForm.riskLevel" class="input">
                    <option value="">——</option>
                    <option value="HIGH">高危</option>
                    <option value="MID">中危</option>
                    <option value="LOW">低危</option>
                  </select>
                </div>
                <div class="form-row">
                  <div class="form-label">责任医生</div>
                  <div ref="doctorSelectRef" class="doctor-select" @click="focusDoctorInput">
                    <input
                      ref="doctorInputRef"
                      v-model="riskOwnerForm.responsibleDoctor"
                      class="input"
                      type="text"
                      placeholder="全部责任医生 / 输入关键字"
                      @input.stop="onDoctorInput"
                      @focus="onDoctorFocus"
                      @click.stop
                    />
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-ghost" @click="closeRiskOwnerEdit">取消</button>
              <button type="button" class="btn btn-primary" @click="saveRiskOwnerEdit">保存</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 责任医生下拉浮层（Teleport 到 body，避免被弹窗裁剪） -->
      <Teleport to="body">
        <div
          v-if="showDoctorDropdown"
          ref="doctorDropdownRef"
          class="doctor-dropdown-floating"
          :style="{ ...doctorDropdownStyle, position: 'fixed', zIndex: '1000001', pointerEvents: 'auto' }"
        >
          <div class="dropdown">
            <div v-if="doctorLoading" class="dropdown-item muted">加载中...</div>
            <button
              v-for="name in doctorFiltered"
              :key="name"
              type="button"
              class="dropdown-item"
              @mousedown.stop.prevent="selectDoctor(name)"
            >
              {{ name }}
            </button>
            <div v-if="!doctorLoading && !doctorFiltered.length" class="dropdown-item muted">无匹配结果</div>
          </div>
        </div>
      </Teleport>

      <!-- 日常自测（独占一行） -->
      <div class="card self-check-card">
        <div class="card-header-row">
          <span class="card-title">日常自测</span>
        </div>
        <div class="self-check-body">
          <div class="self-check-left">
            <div class="self-check-panel">
              <div class="panel-title">基础信息</div>
              <div class="panel-row">
                <span class="panel-label">身高</span>
                <span class="panel-value">{{ selfCheckExtra.height }}</span>
              </div>
              <div class="panel-row">
                <span class="panel-label">体重</span>
                <span class="panel-value">{{ selfCheckExtra.weight }}</span>
              </div>
              <div class="panel-row">
                <span class="panel-label">BMI</span>
                <span class="panel-value">{{ selfCheckExtra.bmi }}</span>
              </div>
            </div>
          </div>
          <div class="self-check-right">
            <div class="self-check-grid">
              <div class="self-check-col">
                <div class="info-item">
                  <span class="info-label">今日血压：</span>
                  <span class="info-value">{{ latestSelfCheck.bp || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">今日心率：</span>
                  <span class="info-value">{{ latestSelfCheck.hr || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">今日血氧：</span>
                  <span class="info-value">{{ latestSelfCheck.spo2 || '-' }}</span>
                </div>
              </div>
              <div class="self-check-col">
                <div class="info-item">
                  <span class="info-label">睡眠情况：</span>
                  <span class="info-value">{{ selfCheckExtra.sleep }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">食欲情况：</span>
                  <span class="info-value">{{ selfCheckExtra.appetite }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">大便情况：</span>
                  <span class="info-value">{{ selfCheckExtra.stool }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">其他症状：</span>
                  <span class="info-value">{{ selfCheckExtra.otherSymptom }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 体格检查（移动到日常自测下方，独占一行） -->
      <div class="card">
        <div class="card-header-row">
          <span class="card-title">体格检查</span>
        </div>
        <div class="info-item">
          <span class="info-label">展示说明：</span>
          <span class="info-value">{{ questionnaireNoteText }}</span>
        </div>
        <!-- 直接在卡片中翻页查看（每一步一个页），不再弹窗 -->
        <div v-if="loadingQuestionnaire" class="questionnaire-loading">加载中...</div>
        <template v-else>
          <div v-if="questionnaireCurrentSection" class="questionnaire-step in-card">
            <div class="step-title">{{ questionnaireCurrentSection.title }}</div>
            <div v-if="questionnaireCurrentSection.desc" class="step-desc">{{ questionnaireCurrentSection.desc }}</div>
            <div
              v-for="row in questionnaireCurrentSection.rows"
              :key="`${questionnaireCurrentSection.key}-${row.key}`"
              class="q-row"
            >
              <span class="q-label">{{ row.label }}：</span>
              <span class="q-value-block">
                <span class="q-value">{{ row.value }}</span>
                <span v-if="row.help" class="q-help">{{ row.help }}</span>
              </span>
            </div>
          </div>
          <div v-else class="info-item">
            <span class="info-label">康养指标情况：</span>
            <span class="info-value">暂无小程序康养指标数据</span>
          </div>

          <div v-if="questionnaireTotalSteps > 1" class="questionnaire-pager in-card">
            <button
              type="button"
              class="btn btn-ghost"
              :disabled="questionnaireStepIndex <= 0"
              @click="goQuestionnaireStep(questionnaireStepIndex - 1)"
            >
              上一步
            </button>
            <div class="pager-center">
              <button
                v-for="p in questionnaireStepPageNumbers"
                :key="String(p)"
                type="button"
                class="page-btn"
                :class="{ active: p === questionnaireStepIndex + 1, disabled: p === '...' }"
                :disabled="p === '...' || p === questionnaireStepIndex + 1"
                @click="p !== '...' && goQuestionnaireStep((p as number) - 1)"
              >
                {{ p }}
              </button>
            </div>
            <button
              type="button"
              class="btn btn-ghost"
              :disabled="questionnaireStepIndex >= questionnaireTotalSteps - 1"
              @click="goQuestionnaireStep(questionnaireStepIndex + 1)"
            >
              下一步
            </button>
          </div>
        </template>
      </div>

      <!-- 中医四诊与证候 / AI 风险评估 / 体格检查 / 检查报告（按截图样式 2×2 展示） -->
      <div class="card-grid cols-2">
        <!-- 中医四诊与证候 -->
        <div class="card tcm-card">
          <div class="card-header-row">
            <span class="card-title">中医四诊与证候</span>
            <button type="button" class="btn-mini" @click="openTcmEdit">编辑</button>
          </div>
          <div class="info-item">
            <span class="info-label">主诉：</span>
            <span class="info-value pill-value">{{ tcmDiagnosis?.mainComplaint || profile?.mainComplaint || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">舌象：</span>
            <span class="info-value pill-value">{{ tcmDiagnosis?.tongueDescription || profile?.tongue || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">脉象：</span>
            <span class="info-value pill-value">{{ tcmDiagnosis?.pulseDescription || profile?.pulse || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">证候总结：</span>
            <span class="info-value pill-value tcm-summary">{{ tcmDiagnosis?.tcmSummary || profile?.tcmSummary || '—' }}</span>
          </div>
        </div>

        <!-- AI 风险评估与建议 -->
        <div class="card">
          <div class="card-header-row">
            <span class="card-title">AI 风险评估与建议</span>
          </div>
          <div class="info-item">
            <span class="info-label">风险评分：</span>
            <span class="info-value pill-value">{{ profile?.aiScore || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">重点关注：</span>
            <span class="info-value pill-value">{{ profile?.aiFocus || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">建议：</span>
            <span class="info-value pill-value">{{ profile?.aiAdvice || '—' }}</span>
          </div>
        </div>

        <!-- 检查报告 -->
        <div class="card">
          <div class="card-header-row">
            <span class="card-title">检查报告</span>
          </div>
          <div class="info-item">
            <span class="info-label">关键检查：</span>
            <span class="info-value pill-value">{{ profile?.examSummary || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">影像 / 检查链接：</span>
            <span class="info-value pill-value">{{ profile?.examReportLink || '—' }}</span>
          </div>
        </div>

        <!-- 最近指标趋势（放入 2×2 网格空位，不独占整行） -->
        <div class="card">
          <div class="card-title">最近指标趋势（日常自测：血压 / 心率 / 血氧）</div>
          <div ref="chartContainer" class="chart-container"></div>
          <div v-if="metrics.latestDate" class="latest-indicator">
            最近一次指标：{{ metrics.latestDate }} {{ latestIndicatorText }}
          </div>
          <div v-else-if="!loadingMetrics && metrics.dates.length === 0" class="latest-indicator">
            暂无指标数据
          </div>
        </div>
      </div>

      <!-- 第五排：生活习惯与既往病史 + 中西医治疗记录 -->
      <div class="card-grid cols-2">
        <div class="card">
          <div class="card-header-row">
            <span class="card-title">生活习惯与既往病史</span>
            <button type="button" class="btn-mini" @click="openLifestyleEdit">编辑</button>
          </div>
          <div class="info-item">
            <span class="info-label">生活习惯：</span>
            <span class="info-value">
              {{ profile?.lifestyle || (detail as any)?.patient?.lifestyle || '——' }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">既往病史：</span>
            <span class="info-value">
              {{ profile?.pastHistory || detail?.patient?.pastHistory || '——' }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">遗传病史：</span>
            <span class="info-value">
              {{ profile?.familyHistory || detail?.patient?.familyHistory || '——' }}
            </span>
          </div>
        </div>

        <div class="card">
          <div class="card-title">中西医结合治疗记录</div>
          <div class="info-item">
            <span class="info-label">中医治疗：</span>
            <span class="info-value">
              {{ profile?.tcmTreatment || '——' }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">西医治疗：</span>
            <span class="info-value">
              {{ profile?.westernTreatment || '——' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 编辑：基本信息 -->
      <Teleport to="body">
        <div v-if="showBasicInfoEdit" class="modal-overlay" @click="closeBasicInfoEdit">
          <div class="modal-content edit-modal" @click.stop>
            <div class="modal-header">
              <h3>编辑 · 基本信息</h3>
              <button type="button" class="modal-close" @click="closeBasicInfoEdit">×</button>
            </div>
            <div class="modal-body">
              <div class="edit-form">
                <div class="form-row">
                  <div class="form-label">姓名</div>
                  <input v-model="basicInfoForm.name" class="input" type="text" placeholder="请输入姓名" />
                </div>
                <div class="form-row">
                  <div class="form-label">性别</div>
                  <select v-model="basicInfoForm.gender" class="input">
                    <option value="">——</option>
                    <option value="M">男</option>
                    <option value="F">女</option>
                    <option value="男">男</option>
                    <option value="女">女</option>
                  </select>
                </div>
                <div class="form-row">
                  <div class="form-label">年龄</div>
                  <input v-model.number="basicInfoForm.age" class="input" type="number" min="0" max="150" placeholder="请输入年龄" />
                </div>
                <div class="form-row">
                  <div class="form-label">身份证号</div>
                  <input v-model="basicInfoForm.idCard" class="input" type="text" maxlength="18" placeholder="请输入身份证号" />
                </div>
                <div class="form-row">
                  <div class="form-label">手机号</div>
                  <input v-model="basicInfoForm.phone" class="input" type="tel" maxlength="11" placeholder="请输入手机号" />
                </div>
                <div class="form-row">
                  <div class="form-label">住址</div>
                  <input v-model="basicInfoForm.address" class="input" type="text" placeholder="请输入住址" />
                </div>
                <div class="form-row">
                  <div class="form-label">家族史 / 遗传病史</div>
                  <textarea v-model="basicInfoForm.familyHistory" rows="3" class="input" placeholder="请输入家族史 / 遗传病史"></textarea>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-ghost" @click="closeBasicInfoEdit">取消</button>
              <button type="button" class="btn btn-primary" @click="saveBasicInfoEdit">保存</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 编辑：诊断与证型 -->
      <Teleport to="body">
        <div v-if="showDiagnosisEdit" class="modal-overlay" @click="closeDiagnosisEdit">
          <div class="modal-content edit-modal" @click.stop>
            <div class="modal-header">
              <h3>编辑 · 诊断与证型</h3>
              <button type="button" class="modal-close" @click="closeDiagnosisEdit">×</button>
            </div>
            <div class="modal-body">
              <div class="edit-form">
                <div class="form-row">
                  <div class="form-label">主病种</div>
                  <input v-model="diagnosisForm.disease" class="input" type="text" placeholder="请输入主病种" />
                </div>
                <div class="form-row">
                  <div class="form-label">证型</div>
                  <input v-model="diagnosisForm.syndrome" class="input" type="text" placeholder="请输入证型" />
                </div>
                <div class="form-row">
                  <div class="form-label">体质</div>
                  <input v-model="diagnosisForm.constitution" class="input" type="text" placeholder="请输入体质" />
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-ghost" @click="closeDiagnosisEdit">取消</button>
              <button type="button" class="btn btn-primary" @click="saveDiagnosisEdit">保存</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 编辑：生活习惯与既往病史 -->
      <Teleport to="body">
        <div v-if="showLifestyleEdit" class="modal-overlay" @click="closeLifestyleEdit">
          <div class="modal-content edit-modal" @click.stop>
            <div class="modal-header">
              <h3>编辑 · 生活习惯与既往病史</h3>
              <button type="button" class="modal-close" @click="closeLifestyleEdit">×</button>
            </div>
            <div class="modal-body">
              <div class="edit-form">
                <div class="form-row">
                  <div class="form-label">生活习惯</div>
                  <textarea v-model="lifestyleForm.lifestyle" rows="4" class="input" placeholder="请输入生活习惯"></textarea>
                </div>
                <div class="form-row">
                  <div class="form-label">既往病史</div>
                  <textarea v-model="lifestyleForm.pastHistory" rows="4" class="input" placeholder="请输入既往病史"></textarea>
                </div>
                <div class="form-row">
                  <div class="form-label">遗传病史</div>
                  <textarea v-model="lifestyleForm.familyHistory" rows="3" class="input" placeholder="请输入遗传病史"></textarea>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-ghost" @click="closeLifestyleEdit">取消</button>
              <button type="button" class="btn btn-primary" @click="saveLifestyleEdit">保存</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 编辑：中医四诊与证候 -->
      <Teleport to="body">
        <div v-if="showTcmEdit" class="modal-overlay" @click="closeTcmEdit">
          <div class="modal-content edit-modal" @click.stop>
            <div class="modal-header">
              <h3>编辑 · 中医四诊与证候</h3>
              <button type="button" class="modal-close" @click="closeTcmEdit">×</button>
            </div>
            <div class="modal-body">
              <div class="edit-form">
                <div class="form-row">
                  <div class="form-label">主诉</div>
                  <textarea v-model="tcmForm.mainComplaint" rows="3" class="input" placeholder="请输入主诉"></textarea>
                </div>
                <div class="form-row">
                  <div class="form-label">舌象</div>
                  <textarea v-model="tcmForm.tongueDescription" rows="3" class="input" placeholder="请输入舌象描述"></textarea>
                </div>
                <div class="form-row">
                  <div class="form-label">脉象</div>
                  <textarea v-model="tcmForm.pulseDescription" rows="3" class="input" placeholder="请输入脉象描述"></textarea>
                </div>
                <div class="form-row">
                  <div class="form-label">证候总结</div>
                  <textarea v-model="tcmForm.tcmSummary" rows="4" class="input" placeholder="请输入证候总结"></textarea>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-ghost" @click="closeTcmEdit">取消</button>
              <button type="button" class="btn btn-primary" @click="saveTcmEdit">保存</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 补充信息（来自详情页），避免与上方“基本信息/诊断与证型”重复 -->
      <div v-if="detail" class="card">
        <div class="card-header">
          <span class="card-title">补充信息</span>
          <span class="card-subtitle">住院/医保/紧急联系人等</span>
        </div>
        <div class="card-body">
          <div class="info-grid">
            <div v-if="detail.patient?.emergencyContact" class="info-item">
              <span class="info-label">紧急联系人：</span>
              <span class="info-value">{{ detail.patient.emergencyContact }}</span>
            </div>
            <div v-if="detail.patient?.emergencyPhone" class="info-item">
              <span class="info-label">紧急联系电话：</span>
              <span class="info-value">{{ detail.patient.emergencyPhone }}</span>
            </div>
            <div v-if="detail.patient?.medicalRecordNo" class="info-item">
              <span class="info-label">病历号：</span>
              <span class="info-value">{{ detail.patient.medicalRecordNo }}</span>
            </div>
            <div v-if="detail.patient?.admissionDate" class="info-item">
              <span class="info-label">入院日期：</span>
              <span class="info-value">{{ detail.patient.admissionDate }}</span>
            </div>
            <div v-if="detail.patient?.dischargeDate" class="info-item">
              <span class="info-label">出院日期：</span>
              <span class="info-value">{{ detail.patient.dischargeDate }}</span>
            </div>
            <div v-if="detail.patient?.insuranceType" class="info-item">
              <span class="info-label">医保类型：</span>
              <span class="info-value">{{ detail.patient.insuranceType }}</span>
            </div>
            <div v-if="detail.patient?.insuranceNo" class="info-item">
              <span class="info-label">医保号：</span>
              <span class="info-value">{{ detail.patient.insuranceNo }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 当前待随访任务（仅在详情数据存在时显示） -->
      <div v-if="detail" class="card">
        <div class="card-header">
          <span class="card-title">当前待随访任务</span>
          <span class="card-subtitle">followup_task（PENDING）</span>
        </div>
        <div class="card-body">
          <div v-if="highlightPending" class="pending-highlight">
            <span class="pill" :class="{ overdue: highlightPending.overdue }">
              最近待随访：{{ highlightPending.visitDate || '--' }} {{ highlightPending.visitTime || '' }} ·
              {{ highlightPending.symptom || '' }} · {{ highlightPending.doctor || '' }}
              <span v-if="highlightPending.overdue" class="pill-overdue-text">超期</span>
            </span>
          </div>
          <table class="table-simple">
            <thead>
              <tr>
                <th>计划日期</th>
                <th>时间</th>
                <th>症状主诉</th>
                <th>系统证型</th>
                <th>责任医生</th>
                <th>优先级</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!pendingTasks.length">
                <td colspan="7" class="empty-tip">暂无待随访任务</td>
              </tr>
              <tr
                v-for="item in pendingTasks"
                :key="item.id"
                :class="{ 'overdue-row': item.overdue }"
              >
                <td>{{ item.visitDate || '' }}</td>
                <td>{{ item.visitTime || '' }}</td>
                <td :title="item.symptom || ''">{{ item.symptom || '' }}</td>
                <td>{{ item.systemSyndrome || '' }}</td>
                <td>{{ item.doctor || '' }}</td>
                <td>
                  <span
                    class="priority-pill"
                    :class="getPriorityClass(item.priority, item.overdue)"
                  >
                    {{ getPriorityText(item.priority, item.overdue) }}
                  </span>
                </td>
                <td>
                  <span class="status-pill status-warning">待随访</span>
                  <span v-if="item.overdue" class="pill overdue">超期</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 最近随访记录（仅在详情数据存在时显示） -->
      <div v-if="detail" class="card">
        <div class="card-header">
          <span class="card-title">最近随访记录（5条）</span>
          <span class="card-subtitle">followup_record</span>
        </div>
        <div class="card-body">
          <div v-if="!recentFollowups.length" class="empty-tip">暂无随访记录</div>
          <div v-else class="timeline">
            <div v-for="item in recentFollowups" :key="item.id" class="timeline-item">
              <div class="timeline-header">
                <span class="timeline-title">{{ item.followupTime || '' }}</span>
                <span>
                  {{ item.followupType || '' }} · {{ item.staffName || '' }}
                  <span
                    class="priority-pill small"
                    :class="getPriorityClass(item.priority, false)"
                    v-if="item.priority"
                  >
                    {{ getPriorityText(item.priority, false) }}
                  </span>
                </span>
              </div>
              <div class="timeline-content">
                {{ item.contentSummary || '' }}
              </div>
              <div class="timeline-footer">
                结果：{{ item.resultStatus || '' }}；下次计划：{{ item.nextPlanTime || '--' }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 最近告警（仅在详情数据存在时显示） -->
      <div v-if="detail" class="card">
        <div class="card-header">
          <span class="card-title">最近告警（5条）</span>
          <span class="card-subtitle">alert_event</span>
        </div>
        <div class="card-body">
          <table class="table-simple">
            <thead>
              <tr>
                <th>类型</th>
                <th>级别</th>
                <th>持续</th>
                <th>最近时间</th>
                <th>处置状态</th>
                <th>摘要</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!recentAlerts.length">
                <td colspan="6" class="empty-tip">暂无告警</td>
              </tr>
              <tr v-for="item in recentAlerts" :key="item.id">
                <td>{{ item.alertType || '' }}</td>
                <td>
                  <span class="risk-pill" :class="getRiskClass(item.severity)">
                    {{ item.severity || '' }}
                  </span>
                </td>
                <td>{{ item.duration || '' }}</td>
                <td>{{ item.lastTime || '' }}</td>
                <td>{{ item.status || '' }}</td>
                <td :title="item.summary || ''">{{ item.summary || '' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import request from '@/api/request'
import * as echarts from 'echarts'
import { patientApi, type PatientProfileData, type PatientMetricsData, type TCMDiagnosisData } from '@/api/patient'
import { systemApi } from '@/api/system'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadingMetrics = ref(false)
const error = ref('')
const detail = ref<any>(null)
const profile = ref<PatientProfileData | null>(null)
const metrics = ref<PatientMetricsData>({
  dates: [],
  sbp: [],
  dbp: [],
  heartRate: [],
  spo2: []
})
const tcmDiagnosis = ref<TCMDiagnosisData | null>(null)

// 小程序康养指标数据（体格检查卡片直接展示使用）
const loadingQuestionnaire = ref(false)
const questionnaireData = ref<any>(null)


type QuestionnaireRow = {
  key: string
  label: string
  value: string
  help?: string
}

type QuestionnaireSection = {
  key: string
  title: string
  desc?: string
  rows: QuestionnaireRow[]
}

const QUESTIONNAIRE_CHOICE_LABELS: Record<string, Record<string, string>> = {
  chronicDiseaseType: {
    HTN: '高血压',
    DM: '糖尿病',
    HF: '冠心病/心衰',
    COPD: '慢阻肺/哮喘',
    OTHER: '其他/不确定'
  },
  chestTightness: {
    '0': '没有',
    '1': '偶尔有',
    '2': '经常有',
    '3': '明显加重/影响活动'
  },
  legSwelling: {
    '0': '没有',
    '1': '下午有轻微',
    '2': '明显压痕',
    '3': '持续较重浮肿'
  },
  nightAwakenings: {
    '0': '0–1 次',
    '1': '2–3 次',
    '2': '4 次以上',
    '3': '几乎整夜睡不安稳'
  },
  stool: {
    '0': '每天一次、成形',
    '1': '偏稀或次数偏多',
    '2': '偏干或两天一次以上',
    '3': '需用药或非常不规律'
  },
  measuredBloodPressure: {
    '0': '未测',
    '1': '测过一次',
    '2': '早晚各一次',
    '3': '多次/不确定'
  },
  saltIntake: {
    '0': '偏淡/低盐',
    '1': '正常',
    '2': '偏咸',
    '3': '很咸/重口'
  },
  glucoseMeasure: {
    '0': '未测',
    '1': '测过一次',
    '2': '餐前/餐后都测',
    '3': '多次/不确定'
  },
  hypoSign: {
    '0': '没有',
    '1': '轻微，休息可缓解',
    '2': '明显，需要进食缓解',
    '3': '严重，考虑就医'
  },
  orthopnea: {
    '0': '不需要',
    '1': '需要垫高 1 个枕头',
    '2': '需要半卧/多枕',
    '3': '坐起才能缓解'
  },
  nocturia: {
    '0': '0–1 次',
    '1': '2 次',
    '2': '3 次',
    '3': '4 次以上'
  },
  sputum: {
    '0': '无/很少',
    '1': '白色/清稀',
    '2': '黄绿/增多',
    '3': '带血/明显增多'
  },
  wheeze: {
    '0': '没有',
    '1': '轻微，活动时有',
    '2': '明显，影响活动',
    '3': '休息时也有/考虑就医'
  }
}

const FIELD_START_END_HINT: Record<string, [string, string]> = {
  overallFeeling: ['很差', '很好'],
  sleepQuality: ['很差', '很好'],
  mood: ['很差', '很好'],
  appetite: ['很差', '很好'],
  medicationAdherence: ['常漏服', '很按时'],
  dietControl: ['未控制', '控制很好'],
  sugarIntake: ['未控制', '控制很好'],
  inhalerUse: ['不规范', '很规范'],
  fatigueLevel: ['无', '很重'],
  shortnessOfBreath: ['无', '很重'],
  palpitations: ['无', '很重'],
  headacheLevel: ['无', '很重'],
  chestPainScore: ['无', '很重'],
  coughScore: ['无', '很重']
}

const STEP4_DISEASE_FIELDS: Record<string, Array<{ key: string; label: string }>> = {
  HTN: [
    { key: 'headacheLevel', label: '头晕/头痛程度（0-10）' },
    { key: 'measuredBloodPressure', label: '今天是否测量血压' },
    { key: 'saltIntake', label: '今天饮食咸淡情况' }
  ],
  DM: [
    { key: 'glucoseMeasure', label: '今天是否测量血糖' },
    { key: 'hypoSign', label: '是否出现疑似低血糖表现' },
    { key: 'sugarIntake', label: '今天控糖情况（1-5 星）' }
  ],
  HF: [
    { key: 'orthopnea', label: '今晚睡觉是否需要垫高枕头/半卧' },
    { key: 'nocturia', label: '夜间起夜（小便）次数' },
    { key: 'chestPainScore', label: '胸痛/压榨感程度（0-10）' }
  ],
  COPD: [
    { key: 'coughScore', label: '咳嗽频率/程度（0-10）' },
    { key: 'sputum', label: '痰的情况' },
    { key: 'wheeze', label: '是否出现喘鸣/哮喘样发作' },
    { key: 'inhalerUse', label: '今天吸入剂/雾化是否规范（1-5 星）' }
  ],
  OTHER: [{ key: 'otherNote', label: '今天是否有其他特别不适' }]
}

const STEP4_COMMON_FIELDS: Array<{ key: string; label: string }> = [
  { key: 'medicationAdherence', label: '今日用药是否按时（1-5 星）' },
  { key: 'dietControl', label: '今日饮食控制情况（1-5 星）' },
  { key: 'exerciseMinutes', label: '今日运动/步行时长' }
]

function questionnaireToNumber(value: any): number | null {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function questionnaireBlank(value: any): boolean {
  return value === null || value === undefined || String(value).trim() === ''
}

function score5Hint(n: number): string {
  if (n <= 1) return '较差'
  if (n === 2) return '偏差'
  if (n === 3) return '一般'
  if (n === 4) return '较好'
  return '很好'
}

function score10Hint(n: number): string {
  if (n <= 0) return '无明显不适'
  if (n <= 3) return '轻度'
  if (n <= 6) return '中度'
  if (n <= 8) return '较重'
  return '重度'
}

function exerciseHint(n: number): string {
  if (n <= 0) return '今日未运动'
  if (n <= 30) return '轻量活动'
  if (n <= 60) return '适量活动'
  return '活动较多'
}

function formatQuestionnaireValue(field: string, rawValue: any): string {
  if (questionnaireBlank(rawValue)) return '无'
  const optionMap = QUESTIONNAIRE_CHOICE_LABELS[field]
  if (optionMap) {
    const mapped = optionMap[String(rawValue)]
    return mapped || String(rawValue)
  }
  if (['overallFeeling', 'sleepQuality', 'mood', 'appetite', 'medicationAdherence', 'dietControl', 'sugarIntake', 'inhalerUse'].includes(field)) {
    const n = questionnaireToNumber(rawValue)
    if (n == null) return String(rawValue)
    return `${n}/5 星`
  }
  if (['fatigueLevel', 'shortnessOfBreath', 'palpitations', 'headacheLevel', 'chestPainScore', 'coughScore'].includes(field)) {
    const n = questionnaireToNumber(rawValue)
    if (n == null) return String(rawValue)
    return `${n}/10 分`
  }
  if (field === 'exerciseMinutes') {
    const n = questionnaireToNumber(rawValue)
    if (n == null) return String(rawValue)
    return `${n} 分钟`
  }
  if (field === 'height') return `${rawValue} cm`
  if (field === 'weight') return `${rawValue} kg`
  return String(rawValue)
}

function formatQuestionnaireHelp(field: string, rawValue: any): string {
  if (questionnaireBlank(rawValue)) return ''
  if (['overallFeeling', 'sleepQuality', 'mood', 'appetite', 'medicationAdherence', 'dietControl', 'sugarIntake', 'inhalerUse'].includes(field)) {
    const n = questionnaireToNumber(rawValue)
    return n == null ? '' : score5Hint(n)
  }
  if (['fatigueLevel', 'shortnessOfBreath', 'palpitations', 'headacheLevel', 'chestPainScore', 'coughScore'].includes(field)) {
    const n = questionnaireToNumber(rawValue)
    return n == null ? '' : score10Hint(n)
  }
  if (field === 'exerciseMinutes') {
    const n = questionnaireToNumber(rawValue)
    return n == null ? '' : exerciseHint(n)
  }
  const rangeHint = FIELD_START_END_HINT[field]
  if (rangeHint) return `${rangeHint[0]} → ${rangeHint[1]}`
  return ''
}

function makeQuestionnaireRow(key: string, label: string, rawValue: any): QuestionnaireRow {
  return {
    key,
    label,
    value: formatQuestionnaireValue(key, rawValue),
    help: formatQuestionnaireHelp(key, rawValue) || undefined
  }
}

const questionnaireNoteText = computed(() => {
  return questionnaireData.value?.displayNote || '选择题按小程序原选项文字展示；评分题会补充程度说明，便于医护查看。'
})

const questionnaireDiseaseCode = computed(() => {
  const q = questionnaireData.value || {}
  return q.diseaseCode || q.step1?.chronicDiseaseType || null
})

const questionnaireSections = computed<QuestionnaireSection[]>(() => {
  const q = questionnaireData.value || {}
  const s1 = q.step1 || {}
  const s2 = q.step2 || {}
  const s3 = q.step3 || {}
  const s4 = q.step4 || {}
  const diseaseCode = questionnaireDiseaseCode.value || 'OTHER'
  const step4Title = q.step4Title || s4.title || '第4步：用药、饮食与慢病管理'

  const sections: QuestionnaireSection[] = [
    {
      key: 'step1',
      title: '第1步：基础信息与整体感觉',
      rows: [
        makeQuestionnaireRow('chronicDiseaseType', '主要慢性病类型', s1.chronicDiseaseType || q.diseaseCode),
        makeQuestionnaireRow('overallFeeling', '今天整体感觉', s1.overallFeeling),
        makeQuestionnaireRow('fatigueLevel', '今天乏力程度（0-10）', s1.fatigueLevel),
        makeQuestionnaireRow('height', '身高', s1.height),
        makeQuestionnaireRow('weight', '体重', s1.weight)
      ]
    },
    {
      key: 'step2',
      title: '第2步：呼吸与心血管症状',
      rows: [
        makeQuestionnaireRow('shortnessOfBreath', '气短/呼吸困难程度（0-10）', s2.shortnessOfBreath ?? s2.dyspneaLevel),
        makeQuestionnaireRow('chestTightness', '是否有胸闷或胸部不适', s2.chestTightness ?? s2.chestDiscomfort),
        makeQuestionnaireRow('palpitations', '心慌/心悸程度（0-10）', s2.palpitations ?? s2.palpitationsLevel),
        makeQuestionnaireRow('legSwelling', '下肢或脚踝是否有浮肿', s2.legSwelling ?? s2.edema)
      ]
    },
    {
      key: 'step3',
      title: '第3步：睡眠与情绪食欲',
      rows: [
        makeQuestionnaireRow('sleepQuality', '昨晚睡眠质量（1-5 星）', s3.sleepQuality),
        makeQuestionnaireRow('nightAwakenings', '昨晚夜间醒来次数', s3.nightAwakenings),
        makeQuestionnaireRow('mood', '今天情绪状态（1-5 星）', s3.mood ?? s3.emotion),
        makeQuestionnaireRow('appetite', '今天食欲情况（1-5 星）', s3.appetite),
        makeQuestionnaireRow('stool', '最近一周大便情况', s3.stool ?? s3.stoolSituation)
      ]
    }
  ]

  const step4Rows = STEP4_COMMON_FIELDS.map((item) => makeQuestionnaireRow(item.key, item.label, s4[item.key]))
  const diseaseRows = (STEP4_DISEASE_FIELDS[diseaseCode] || STEP4_DISEASE_FIELDS.OTHER).map((item) => makeQuestionnaireRow(item.key, item.label, s4[item.key]))
  const noteRows: QuestionnaireRow[] = []
  if (diseaseCode !== 'OTHER' && !questionnaireBlank(s4.otherNote)) {
    noteRows.push(makeQuestionnaireRow('otherNote', '今天是否有其他特别不适', s4.otherNote))
  }
  noteRows.push(makeQuestionnaireRow('remark', '补充说明', s4.remark ?? s4.extraRemark))

  sections.push({
    key: 'step4',
    title: step4Title,
    desc: '已按小程序对应病种的问题结构展示。',
    rows: [...step4Rows, ...diseaseRows, ...noteRows]
  })

  return sections
})

// 仅新增分页交互：不改数据结构（每一步一个页）
const questionnaireStepIndex = ref(0)
const questionnaireTotalSteps = computed(() => questionnaireSections.value.length || 0)
const questionnaireCurrentSection = computed<QuestionnaireSection | null>(() => {
  const list = questionnaireSections.value
  if (!list.length) return null
  const idx = Math.min(Math.max(0, questionnaireStepIndex.value), list.length - 1)
  return list[idx] || null
})

const questionnaireStepPageNumbers = computed<(number | string)[]>(() => {
  const total = questionnaireTotalSteps.value
  const current = questionnaireStepIndex.value + 1
  const pages: (number | string)[] = []
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
    return pages
  }
  if (current <= 4) {
    pages.push(1, 2, 3, 4, 5, '...', total)
    return pages
  }
  if (current >= total - 3) {
    pages.push(1, '...')
    for (let i = total - 4; i <= total; i++) pages.push(i)
    return pages
  }
  pages.push(1, '...', current - 1, current, current + 1, '...', total)
  return pages
})

function goQuestionnaireStep(idx: number) {
  const total = questionnaireTotalSteps.value
  if (total <= 0) return
  questionnaireStepIndex.value = Math.min(Math.max(0, idx), total - 1)
}

const questionnaireOverviewRows = computed<QuestionnaireRow[]>(() => {
  const sections = questionnaireSections.value
  if (!sections.length) return []
  const step2Rows = sections.find((item) => item.key === 'step2')?.rows || []
  const step4Rows = sections.find((item) => item.key === 'step4')?.rows || []
  const conciseStep4 = step4Rows.filter((row) => row.key !== 'remark' && row.key !== 'otherNote').slice(0, 4)
  return [...step2Rows, ...conciseStep4].slice(0, 8)
})

// 编辑：生活习惯与既往病史
const showLifestyleEdit = ref(false)
const lifestyleForm = ref({
  lifestyle: '',
  pastHistory: '',
  familyHistory: ''
})

// 编辑：基本信息
const showBasicInfoEdit = ref(false)
const basicInfoForm = ref({
  name: '',
  gender: '',
  age: null as number | null,
  idCard: '',
  phone: '',
  address: '',
  familyHistory: ''
})

// 编辑：诊断与证型
const showDiagnosisEdit = ref(false)
const diagnosisForm = ref({
  disease: '',
  syndrome: '',
  constitution: ''
})

// 编辑：风险与责任人
const showRiskOwnerEdit = ref(false)
const riskOwnerForm = ref({
  riskLevel: '',
  responsibleDoctor: ''
})
const doctorSelectRef = ref<HTMLElement | null>(null)
const doctorInputRef = ref<HTMLInputElement | null>(null)
const showDoctorDropdown = ref(false)
const doctorLoading = ref(false)
const doctorAll = ref<string[]>([])
const doctorFiltered = ref<string[]>([])
const doctorDropdownRef = ref<HTMLElement | null>(null)
const doctorDropdownStyle = ref<Record<string, string>>({
  left: '0px',
  top: '0px',
  width: '240px'
})
let doctorLoadedOnce = false

// 编辑：中医四诊与证候（前端弹窗直接修改展示数据）
const showTcmEdit = ref(false)
const tcmForm = ref({
  mainComplaint: '',
  tongueDescription: '',
  pulseDescription: '',
  tcmSummary: ''
})

let chart: echarts.ECharts | null = null
const chartContainer = ref<HTMLElement | null>(null)

const pendingTasks = computed(() => detail.value?.pendingTasks || [])
const highlightPending = computed(() => pendingTasks.value[0] || null)
const recentFollowups = computed(() => detail.value?.recentFollowups || [])
const recentAlerts = computed(() => detail.value?.recentAlerts || [])

const latestIndicatorText = computed(() => {
  if (metrics.value.dates.length === 0) return ''
  const idx = metrics.value.dates.length - 1
  const sbp = metrics.value.sbp[idx]
  const dbp = metrics.value.dbp[idx]
  const hr = metrics.value.heartRate[idx]
  const spo2 = metrics.value.spo2 && metrics.value.spo2.length > idx ? metrics.value.spo2[idx] : undefined
  const base = `血压 ${sbp}/${dbp} mmHg，心率 ${hr} 次/分`
  return spo2 != null ? `${base}，血氧 ${spo2}%` : base
})

const latestSelfCheck = computed(() => {
  if (!metrics.value.dates || metrics.value.dates.length === 0) {
    return { bp: '', hr: '', spo2: '' }
  }
  const idx = metrics.value.dates.length - 1
  const sbp = metrics.value.sbp[idx]
  const dbp = metrics.value.dbp[idx]
  const hr = metrics.value.heartRate[idx]
  const spo2 = metrics.value.spo2 && metrics.value.spo2.length > idx ? metrics.value.spo2[idx] : undefined
  return {
    bp: sbp != null && dbp != null ? `${sbp}/${dbp} mmHg` : '',
    hr: hr != null ? `${hr} 次/分` : '',
    spo2: spo2 != null ? `${spo2}%` : ''
  }
})

const latestDailyMeasurement = ref<any>(null)

const selfCheckExtra = computed(() => {
  const s1 = (questionnaireData.value as any)?.step1 || {}
  const s3 = (questionnaireData.value as any)?.step3 || {}
  const s4 = (questionnaireData.value as any)?.step4 || {}
  const dm = latestDailyMeasurement.value || {}
  const diseaseCode = questionnaireDiseaseCode.value || 'OTHER'
  const symptomField = (() => {
    if (diseaseCode === 'DM') return s4.hypoSign
    if (diseaseCode === 'HF') return s4.orthopnea ?? s4.chestPainScore
    if (diseaseCode === 'COPD') return s4.coughScore ?? s4.wheeze
    return s4.headacheLevel ?? s4.otherNote
  })()
  return {
    height: s1.height ? formatQuestionnaireValue('height', s1.height) : '-',
    weight: s1.weight ? formatQuestionnaireValue('weight', s1.weight) : '-',
    bmi: (() => {
      const h = s1.height == null || s1.height === '' ? null : Number(s1.height)
      const w = s1.weight == null || s1.weight === '' ? null : Number(s1.weight)
      if (!h || !w) return s1.bmi || '-'
      const v = w / Math.pow(h / 100, 2)
      return Number.isFinite(v) ? v.toFixed(1) : (s1.bmi || '-')
    })(),
    sleep: dm.sleepText || formatQuestionnaireValue('sleepQuality', s3.sleepQuality || s3.sleep),
    appetite: dm.appetiteText || formatQuestionnaireValue('appetite', s3.appetite),
    stool: dm.stoolText || formatQuestionnaireValue('stool', s3.stool || s3.stoolSituation),
    otherSymptom: dm.symptoms || formatQuestionnaireValue('remark', symptomField || s4.otherNote || s4.remark)
  }
})

function unwrapResponse(resp: any) {
  if (resp == null) return resp
  if (resp && typeof resp === 'object' && Object.prototype.hasOwnProperty.call(resp, 'data')) {
    return (resp as any).data
  }
  return resp
}

async function reloadByRoute() {
  const patientId = route.params.patientId ? Number(route.params.patientId) : null
  const riskId = route.query.riskId ? Number(route.query.riskId) : null

  error.value = null
  loading.value = true
  detail.value = null as any
  profile.value = null as any

  if (!patientId && !riskId) {
    error.value = '缺少 patientId 或 riskId'
    loading.value = false
    return
  }

  try {
    if (riskId) {
      await loadProfile(riskId)
      if (profile.value?.patientId) {
        await loadDetail(profile.value.patientId)
        await loadTCMDiagnosis(profile.value.patientId)
        await loadQuestionnaireData(profile.value.patientId)
        await loadLatestDailyMeasurement(profile.value.patientId)
      }
    } else if (patientId) {
      await loadDetail(patientId)
      await loadTCMDiagnosis(patientId)
      await loadQuestionnaireData(patientId)
      if (detail.value?.risk?.id) {
        await loadMetrics(patientId)
      }
      await loadLatestDailyMeasurement(patientId)
    }
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
    await nextTick()
    initChart()
  }
}

onMounted(() => {
  void reloadByRoute()
})

watch(
  () => [route.params.patientId, route.query.riskId],
  () => {
    void reloadByRoute()
  }
)

async function loadQuestionnaireData(patientId?: number) {
  const pid = patientId ?? (route.params.patientId ? Number(route.params.patientId) : null) ?? (profile.value?.patientId || null)
  if (!pid) return
  try {
    let resp: any
    try {
      resp = await request.get<any>(`/api/patient/questionnaire?patientId=${pid}`)
    } catch {
      resp = await request.get<any>(`/patient/questionnaire?patientId=${pid}`)
    }
    const data = unwrapResponse(resp)
    questionnaireData.value = (data && (data as any).data) ? (data as any).data : data
  } catch (e) {
    // 允许接口不存在或无数据，前端用“无”兜底展示
    questionnaireData.value = questionnaireData.value ?? null
  }
}

async function loadLatestDailyMeasurement(patientId?: number) {
  const pid = patientId ?? (route.params.patientId ? Number(route.params.patientId) : null) ?? (profile.value?.patientId || null)
  if (!pid) return
  try {
    let resp: any
    try {
      resp = await request.get<any>(`/api/patient/daily-measurement/latest?patientId=${pid}`)
    } catch {
      resp = await request.get<any>(`/patient/daily-measurement/latest?patientId=${pid}`)
    }
    const data = unwrapResponse(resp)
    latestDailyMeasurement.value = (data && (data as any).data) ? (data as any).data : data
  } catch {
    latestDailyMeasurement.value = latestDailyMeasurement.value ?? null
  }
}

// 不再使用弹窗查看，直接在“体格检查”卡片中翻页展示

function openLifestyleEdit() {
  const p = profile.value as any
  const patient = detail.value?.patient as any
  // 仅修复数据回显：不改布局/不改接口。profile 为空时从 detail.patient 兜底
  lifestyleForm.value.lifestyle = p?.lifestyle ?? patient?.lifestyle ?? ''
  lifestyleForm.value.pastHistory = p?.pastHistory ?? patient?.pastHistory ?? ''
  lifestyleForm.value.familyHistory = p?.familyHistory ?? patient?.familyHistory ?? ''
  showLifestyleEdit.value = true
}

function closeLifestyleEdit() {
  showLifestyleEdit.value = false
}

async function saveLifestyleEdit() {
  const pid = (detail.value?.patient?.id ?? profile.value?.patientId) as number | undefined
  if (!pid) {
    alert('缺少 patientId，无法保存')
    return
  }

  const lifestyle = String(lifestyleForm.value.lifestyle || '')
  const pastHistory = String(lifestyleForm.value.pastHistory || '')
  const familyHistory = String(lifestyleForm.value.familyHistory || '')

  const res = await patientApi.updateLifestyle({
    patientId: Number(pid),
    lifestyle,
    pastHistory,
    familyHistory
  })
  if (!res.success) {
    alert('保存失败：' + (res.message || '未知错误'))
    return
  }

  const p = profile.value as any
  if (p) {
    p.lifestyle = lifestyleForm.value.lifestyle
    p.pastHistory = lifestyleForm.value.pastHistory
    p.familyHistory = lifestyleForm.value.familyHistory
  }
  if (detail.value?.patient) {
    ;(detail.value.patient as any).lifestyle = lifestyleForm.value.lifestyle
    detail.value.patient.pastHistory = lifestyleForm.value.pastHistory
    detail.value.patient.familyHistory = lifestyleForm.value.familyHistory
  }
  showLifestyleEdit.value = false
}

function openBasicInfoEdit() {
  const p = profile.value as any
  const patient = detail.value?.patient as any
  basicInfoForm.value.name = p?.name ?? patient?.name ?? ''
  basicInfoForm.value.gender = patient?.gender ?? ''
  basicInfoForm.value.age = (p?.age ?? patient?.age) ?? null
  basicInfoForm.value.idCard = patient?.idCard ?? ''
  basicInfoForm.value.phone = patient?.phone ?? ''
  basicInfoForm.value.address = patient?.address ?? ''
  basicInfoForm.value.familyHistory = p?.familyHistory ?? patient?.familyHistory ?? ''
  showBasicInfoEdit.value = true
}

function closeBasicInfoEdit() {
  showBasicInfoEdit.value = false
}

async function saveBasicInfoEdit() {
  const pid = (detail.value?.patient?.id ?? profile.value?.patientId) as number | undefined
  if (!pid) {
    alert('缺少 patientId，无法保存')
    return
  }

  const res = await patientApi.updateBasicInfo({
    patientId: Number(pid),
    name: String(basicInfoForm.value.name || ''),
    gender: String(basicInfoForm.value.gender || ''),
    age: basicInfoForm.value.age ?? null,
    idCard: String(basicInfoForm.value.idCard || ''),
    phone: String(basicInfoForm.value.phone || ''),
    address: String(basicInfoForm.value.address || ''),
    familyHistory: String(basicInfoForm.value.familyHistory || '')
  })
  if (!res.success) {
    alert('保存失败：' + (res.message || '未知错误'))
    return
  }

  const patient = detail.value?.patient as any
  if (patient) {
    patient.name = basicInfoForm.value.name
    patient.gender = basicInfoForm.value.gender
    patient.age = basicInfoForm.value.age
    patient.idCard = basicInfoForm.value.idCard
    patient.phone = basicInfoForm.value.phone
    patient.address = basicInfoForm.value.address
    patient.familyHistory = basicInfoForm.value.familyHistory
  }
  if (profile.value) {
    ;(profile.value as any).name = basicInfoForm.value.name
    ;(profile.value as any).age = basicInfoForm.value.age ?? (profile.value as any).age
    ;(profile.value as any).familyHistory = basicInfoForm.value.familyHistory
  }
  showBasicInfoEdit.value = false
}

function openDiagnosisEdit() {
  const p = profile.value as any
  const patient = detail.value?.patient as any
  diagnosisForm.value.disease = p?.disease ?? patient?.disease ?? ''
  diagnosisForm.value.syndrome = p?.syndrome ?? patient?.syndrome ?? ''
  diagnosisForm.value.constitution = p?.constitution ?? patient?.constitution ?? ''
  showDiagnosisEdit.value = true
}

function closeDiagnosisEdit() {
  showDiagnosisEdit.value = false
}

async function saveDiagnosisEdit() {
  const pid = (detail.value?.patient?.id ?? profile.value?.patientId) as number | undefined
  if (!pid) {
    alert('缺少 patientId，无法保存')
    return
  }

  const res = await patientApi.updateDiagnosis({
    patientId: Number(pid),
    disease: String(diagnosisForm.value.disease || ''),
    syndrome: String(diagnosisForm.value.syndrome || ''),
    constitution: String(diagnosisForm.value.constitution || '')
  })
  if (!res.success) {
    alert('保存失败：' + (res.message || '未知错误'))
    return
  }

  const patient = detail.value?.patient as any
  if (patient) {
    patient.disease = diagnosisForm.value.disease
    patient.syndrome = diagnosisForm.value.syndrome
    patient.constitution = diagnosisForm.value.constitution
  }
  if (profile.value) {
    ;(profile.value as any).disease = diagnosisForm.value.disease
    ;(profile.value as any).syndrome = diagnosisForm.value.syndrome
    ;(profile.value as any).constitution = diagnosisForm.value.constitution
  }
  showDiagnosisEdit.value = false
}

async function openRiskOwnerEdit() {
  const p = profile.value as any
  const patient = detail.value?.patient as any
  const risk = detail.value?.risk as any
  riskOwnerForm.value.riskLevel = p?.riskLevel ?? risk?.riskLevel ?? ''
  riskOwnerForm.value.responsibleDoctor = p?.doctor ?? patient?.responsibleDoctor ?? ''
  // 打开弹窗时不自动展开下拉；仅在点击/聚焦输入框时再展开
  showDoctorDropdown.value = false
  showRiskOwnerEdit.value = true
}

function closeRiskOwnerEdit() {
  showRiskOwnerEdit.value = false
  showDoctorDropdown.value = false
}

async function saveRiskOwnerEdit() {
  const patient = detail.value?.patient as any
  const risk = detail.value?.risk as any
  const pid = (patient?.id ?? profile.value?.patientId) as number | undefined
  const riskLevel = String(riskOwnerForm.value.riskLevel || '').trim()
  const responsibleDoctor = String(riskOwnerForm.value.responsibleDoctor || '').trim()
  if (!pid) {
    alert('缺少 patientId，无法保存')
    return
  }

  const res = await patientApi.updateRiskOwner({
    patientId: Number(pid),
    riskLevel,
    responsibleDoctor
  })
  if (!res.success) {
    alert('保存失败：' + (res.message || '未知错误'))
    return
  }

  // 保存成功后回写本地展示
  if (risk) risk.riskLevel = riskLevel
  if (patient) patient.responsibleDoctor = responsibleDoctor
  if (profile.value) {
    ;(profile.value as any).riskLevel = riskLevel
    ;(profile.value as any).doctor = responsibleDoctor
    ;(profile.value as any).riskText = getRiskText(riskLevel)
  }
  showRiskOwnerEdit.value = false
  showDoctorDropdown.value = false
}

async function ensureDoctorLoaded() {
  if (doctorLoadedOnce && doctorAll.value.length) return
  doctorLoading.value = true
  try {
    const res = await systemApi.getOrgUser()
    // 兼容多层包裹：data / data.data
    const layer1: any = res && res.success && res.data ? (res.data as any) : null
    const payload: any = (layer1 && typeof layer1 === 'object' && 'data' in layer1) ? (layer1 as any).data : layer1

    // 兼容字段命名：userList / user_list
    const raw: any[] = Array.isArray(payload?.userList)
      ? payload.userList
      : (Array.isArray(payload?.user_list) ? payload.user_list : [])

    const names = raw
      .map((u: any) => {
        return String(
          u?.userName ??
          u?.name ??
          u?.realName ??
          u?.username ??
          u?.loginName ??
          u?.login_name ??
          ''
        ).trim()
      })
      .filter(Boolean)
    doctorAll.value = Array.from(new Set(names))
    doctorLoadedOnce = true
  } catch {
    doctorAll.value = doctorAll.value || []
    doctorLoadedOnce = true
  } finally {
    doctorLoading.value = false
  }
}

function applyDoctorFilter() {
  const kw = String(riskOwnerForm.value.responsibleDoctor || '').trim().toLowerCase()
  if (!kw) {
    doctorFiltered.value = doctorAll.value.slice()
    return
  }
  doctorFiltered.value = doctorAll.value.filter((n) => n.toLowerCase().includes(kw))
}

async function onDoctorFocus() {
  showDoctorDropdown.value = true
  updateDoctorDropdownPosition()
  await ensureDoctorLoaded()
  applyDoctorFilter()
}

function onDoctorInput() {
  showDoctorDropdown.value = true
  updateDoctorDropdownPosition()
  applyDoctorFilter()
}

function selectDoctor(name: string) {
  riskOwnerForm.value.responsibleDoctor = name
  showDoctorDropdown.value = false
}

function focusDoctorInput() {
  if (doctorInputRef.value) doctorInputRef.value.focus()
}

function updateDoctorDropdownPosition() {
  const el = doctorInputRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  doctorDropdownStyle.value = {
    left: `${rect.left}px`,
    top: `${rect.bottom + 6}px`,
    width: `${rect.width}px`,
    minWidth: `${rect.width}px`
  }
}

function handleGlobalPointerDownForDoctor(e: PointerEvent) {
  if (!showRiskOwnerEdit.value) return
  const root = doctorSelectRef.value
  const drop = doctorDropdownRef.value
  const target = e.target as Node | null
  if (root && target && root.contains(target)) return
  if (drop && target && drop.contains(target)) return
  showDoctorDropdown.value = false
}

function openTcmEdit() {
  tcmForm.value.mainComplaint = tcmDiagnosis.value?.mainComplaint ?? profile.value?.mainComplaint ?? ''
  tcmForm.value.tongueDescription = tcmDiagnosis.value?.tongueDescription ?? (profile.value as any)?.tongue ?? ''
  tcmForm.value.pulseDescription = tcmDiagnosis.value?.pulseDescription ?? (profile.value as any)?.pulse ?? ''
  tcmForm.value.tcmSummary = tcmDiagnosis.value?.tcmSummary ?? (profile.value as any)?.tcmSummary ?? ''
  showTcmEdit.value = true
}

function closeTcmEdit() {
  showTcmEdit.value = false
}

async function saveTcmEdit() {
  // 优先写到 tcmDiagnosis（展示用），同时回写 profile 的旧字段，保证页面其他位置也一致
  const pid = (profile.value?.patientId || detail.value?.patient?.id || 0) as number
  if (!pid) {
    alert('缺少 patientId，无法保存')
    return
  }

  if (!tcmDiagnosis.value) {
    tcmDiagnosis.value = {
      patientId: profile.value?.patientId || detail.value?.patient?.id || 0,
      mainComplaint: '',
      tongueDescription: '',
      pulseDescription: '',
      tcmSummary: '',
      physicalExam: '',
      lifestyle: ''
    } as any
  }

  ;(tcmDiagnosis.value as any).mainComplaint = tcmForm.value.mainComplaint
  ;(tcmDiagnosis.value as any).tongueDescription = tcmForm.value.tongueDescription
  ;(tcmDiagnosis.value as any).pulseDescription = tcmForm.value.pulseDescription
  ;(tcmDiagnosis.value as any).tcmSummary = tcmForm.value.tcmSummary

  // 落库：有 id 则 update，否则 create
  const payload = {
    ...(tcmDiagnosis.value as any),
    patientId: pid,
    mainComplaint: tcmForm.value.mainComplaint,
    tongueDescription: tcmForm.value.tongueDescription,
    pulseDescription: tcmForm.value.pulseDescription,
    tcmSummary: tcmForm.value.tcmSummary
  } as any

  console.log('保存中医四诊数据，payload:', payload)
  const apiRes = (payload.id ? await patientApi.updateTCMDiagnosis(payload) : await patientApi.createTCMDiagnosis(payload)) as any
  console.log('保存中医四诊数据响应:', apiRes)
  if (!apiRes || apiRes.success === false) {
    alert('保存失败：' + ((apiRes && apiRes.message) || '未知错误'))
    return
  }

  // 用后端返回的数据覆盖本地对象，确保拿到 id（避免下次仍走 create）
  try {
    const returned = (apiRes && typeof apiRes === 'object') ? (apiRes.data ?? apiRes) : null
    if (returned && typeof returned === 'object') {
      tcmDiagnosis.value = { ...(tcmDiagnosis.value as any), ...(returned as any), patientId: pid }
      console.log('用后端返回的数据更新tcmDiagnosis.value:', tcmDiagnosis.value)
    } else {
      console.log('后端返回数据格式不符合预期，重新加载中医四诊数据')
      await loadTCMDiagnosis(pid)
    }
  } catch (error) {
    console.error('处理后端返回数据时出错，重新加载中医四诊数据:', error)
    await loadTCMDiagnosis(pid)
  }

  if (profile.value) {
    ;(profile.value as any).mainComplaint = tcmForm.value.mainComplaint
    ;(profile.value as any).tongue = tcmForm.value.tongueDescription
    ;(profile.value as any).pulse = tcmForm.value.pulseDescription
    ;(profile.value as any).tcmSummary = tcmForm.value.tcmSummary
  }

  showTcmEdit.value = false
}

onUnmounted(() => {
  if (chart) {
    chart.dispose()
    chart = null
  }
})

onMounted(() => {
  // 使用捕获阶段，避免被弹窗内部 @click.stop 阻断冒泡，导致无法收起
  window.addEventListener('pointerdown', handleGlobalPointerDownForDoctor, true)
  window.addEventListener('resize', updateDoctorDropdownPosition)
  window.addEventListener('scroll', updateDoctorDropdownPosition, true)
})

onUnmounted(() => {
  window.removeEventListener('pointerdown', handleGlobalPointerDownForDoctor, true)
  window.removeEventListener('resize', updateDoctorDropdownPosition)
  window.removeEventListener('scroll', updateDoctorDropdownPosition, true)
})

watch(() => metrics.value, async () => {
  await nextTick()
  initChart()
}, { deep: true })

async function loadProfile(riskId: number) {
  const result = await patientApi.getProfile(riskId)
  if (result.success && result.data) {
    profile.value = result.data
    await loadMetrics(riskId)
    return
  }
  const msg = result.message || '加载患者档案失败'
  error.value = msg
  throw new Error(msg)
}

async function loadMetrics(riskId: number) {
  loadingMetrics.value = true
  try {
    const result = await patientApi.getMetrics(riskId)
    if (result.success && result.data) {
      metrics.value = result.data
      // 数据加载完成后，等待DOM更新再初始化图表
      await nextTick()
      initChart()
      return
    }
    const msg = result.message || '加载指标数据失败'
    error.value = msg
    throw new Error(msg)
  } catch (error) {
    console.error('加载指标数据失败:', error)
    throw error
  } finally {
    loadingMetrics.value = false
  }
}

async function loadDetail(patientId: number) {
  try {
    let resp: any
    try {
      resp = await request.get<any>(`/api/patient/detail?patientId=${patientId}`, { timeout: 120000 })
    } catch {
      resp = await request.get<any>(`/patient/detail?patientId=${patientId}`, { timeout: 120000 })
    }
    const data = unwrapResponse(resp)
    detail.value = data
    if (!detail.value || !detail.value.patient) {
      const msg = '患者详情接口返回结构异常'
      error.value = msg
      throw new Error(msg)
    }
  } catch (e: any) {
    console.error('加载患者详情失败:', e)
    const rawMsg = e?.message || '加载患者详情失败'
    const msg = rawMsg.includes('资源不存在') ? '患者不存在或患者ID无效' : rawMsg
    error.value = msg
    throw e
  }
}

async function loadTCMDiagnosis(patientId: number) {
  try {
    console.log('加载中医四诊数据，patientId:', patientId)
    const response = await patientApi.getLatestTCMDiagnosis(patientId)
    console.log('中医四诊数据响应:', response)
    if (response.success && response.data) {
      tcmDiagnosis.value = response.data
      console.log('更新tcmDiagnosis.value:', tcmDiagnosis.value)
    } else {
      console.log('中医四诊数据为空:', response)
      tcmDiagnosis.value = null
    }
  } catch (error) {
    console.error('加载中医四诊数据失败:', error)
    // 中医四诊数据加载失败不影响页面其他部分的显示
  }
}

function initChart() {
  if (!chartContainer.value) return

  if (!chart) {
    chart = echarts.init(chartContainer.value)
  }

  if (!metrics.value.dates || metrics.value.dates.length === 0) {
    chart.setOption({
      title: {
        text: '暂无日常自测数据',
        left: 'center',
        top: 'middle',
        textStyle: { fontSize: 14, color: '#999' }
      }
    })
    return
  }

  chart.setOption({
    // 本次为第4步，仅美化时间轴和趋势图，不改布局。
    // 仅美化：统一图表配色与 tooltip / 坐标轴风格（不改数据/类型）
    color: ['#667eea', '#f6ad55', '#90cdf4', '#764ba2'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 23, 42, 0.86)',
      borderColor: 'rgba(255, 255, 255, 0.08)',
      borderWidth: 1,
      textStyle: { color: '#e5e7eb', fontSize: 12, lineHeight: 18 },
      padding: [10, 12],
      extraCssText: 'border-radius:12px; box-shadow:0 12px 26px rgba(15,23,42,0.26);'
    },
    legend: { data: ['收缩压', '舒张压', '心率', ...(metrics.value.spo2 && metrics.value.spo2.length ? ['血氧'] : [])], textStyle: { color: '#64748b' } },
    grid: { left: 56, right: 18, top: 44, bottom: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: metrics.value.dates,
      boundaryGap: false,
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
      axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '数值',
      nameLocation: 'middle',
      nameGap: 44,
      nameTextStyle: { color: '#94a3b8', fontSize: 12 },
      axisLine: { lineStyle: { color: 'rgba(148,163,184,0.55)' } },
      axisTick: { lineStyle: { color: 'rgba(148,163,184,0.35)' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.14)' } }
    },
    series: [
      {
        name: '收缩压',
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.6 },
        itemStyle: { borderWidth: 2, borderColor: '#ffffff' },
        areaStyle: { opacity: 0.10 },
        data: metrics.value.sbp
      },
      {
        name: '舒张压',
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.6 },
        itemStyle: { borderWidth: 2, borderColor: '#ffffff' },
        areaStyle: { opacity: 0.08 },
        data: metrics.value.dbp
      },
      {
        name: '心率',
        type: 'line',
        smooth: 0.35,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { width: 2.6 },
        itemStyle: { borderWidth: 2, borderColor: '#ffffff' },
        areaStyle: { opacity: 0.06 },
        data: metrics.value.heartRate
      },
      ...(metrics.value.spo2 && metrics.value.spo2.length
        ? [{
            name: '血氧',
            type: 'line',
            smooth: 0.35,
            symbol: 'circle',
            symbolSize: 7,
            lineStyle: { width: 2.6 },
            itemStyle: { borderWidth: 2, borderColor: '#ffffff' },
            areaStyle: { opacity: 0.06 },
            data: metrics.value.spo2
          } as any]
        : [])
    ]
  })
}


function getRiskClass(level?: string | null): string {
  const text = (level || '').toUpperCase()
  if (text.includes('HIGH') || text.includes('高')) return 'risk-high'
  if (text.includes('MED') || text.includes('中')) return 'risk-mid'
  if (text.includes('LOW') || text.includes('低')) return 'risk-low'
  return 'risk-mid'
}

function getRiskLevelClass(level?: string | null): string {
  if (!level) return 'badge'
  const text = level.toUpperCase()
  if (text.includes('HIGH') || text.includes('高')) return 'badge badge-danger'
  if (text.includes('MED') || text.includes('中')) return 'badge badge-warning'
  if (text.includes('LOW') || text.includes('低')) return 'badge badge-success'
  return 'badge'
}

function getRiskText(level?: string | null): string {
  if (!level) return '——'
  const text = level.toUpperCase()
  if (text.includes('HIGH')) return '高危'
  if (text.includes('MED')) return '中危'
  if (text.includes('LOW')) return '低危'
  return level
}

function getRiskActionText(level?: string | null): string {
  if (!level) return '风险等级待评估'
  const text = level.toUpperCase()
  if (text.includes('HIGH') || text.includes('高')) return '高风险 · 建议立即评估并处理'
  if (text.includes('MED') || text.includes('中')) return '中风险 · 建议定期随访与动态观察'
  if (text.includes('LOW') || text.includes('低')) return '低风险 · 建议常规管理与健康教育'
  return '风险等级待评估'
}

function getGenderText(gender?: string | null): string {
  if (!gender) return '——'
  if (gender === 'M') return '男'
  if (gender === 'F') return '女'
  return gender
}

function formatCaseId(id?: number | string | null) {
  if (id === undefined || id === null) return '--'
  const num = Number(id)
  if (Number.isNaN(num)) return String(id)
  return `1${String(Math.floor(num)).padStart(3, '0')}`
}

function maskId(idCard?: string | null): string {
  if (!idCard) return '——'
  return idCard.replace(/^(.{4}).+(.{4})$/, '$1****$2')
}

function handleBack() {
  router.back()
}

function getPriorityText(priority?: string | null, overdue?: boolean): string {
  if (overdue) return '高 · 超期'
  if (!priority) return '普通'
  const text = priority.toUpperCase()
  if (text.includes('HIGH') || text.includes('高')) return '高'
  if (text.includes('LOW') || text.includes('低')) return '低'
  if (text.includes('MED') || text.includes('MID') || text.includes('中')) return '中'
  return priority
}

function getPriorityClass(priority?: string | null, overdue?: boolean): string {
  if (overdue) return 'priority-high'
  if (!priority) return 'priority-normal'
  const text = priority.toUpperCase()
  if (text.includes('HIGH') || text.includes('高')) return 'priority-high'
  if (text.includes('LOW') || text.includes('低')) return 'priority-low'
  if (text.includes('MED') || text.includes('MID') || text.includes('中')) return 'priority-mid'
  return 'priority-normal'
}
</script>

<style scoped>
.patient-detail-page {
  /* 固定页面宽度：不足时允许横向滚动 */
  width: 1120px;
  min-width: 1120px;
  margin: 0 auto;
  background: transparent;
  padding: 20px;
  box-sizing: border-box;
  overflow-x: auto;
  overflow-y: visible;
}

/* =========================
   仅美化，不改布局（标题区）
   ========================= */
.page-header {
  padding: 6px 0 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  margin-bottom: 16px;
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.page-title-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.title-badge {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.22);
  flex-shrink: 0;
}

.title-badge-icon {
  width: 22px;
  height: 22px;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.detail-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 20px 28px;
}

/* 本次为第2步，仅美化详情卡片和字段层级，不改布局。 */
.card {
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid rgba(226, 232, 240, 0.70);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.08), 0 4px 12px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.22s ease, border-color 0.22s ease;
  position: relative;
  overflow: hidden;
  margin-bottom: 16px;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.78;
}

.card:hover {
  /* 仅美化：hover 不位移，不改变布局关系 */
  box-shadow: 0 14px 36px rgba(102, 126, 234, 0.10), 0 8px 18px rgba(15, 23, 42, 0.06);
  border-color: rgba(203, 213, 225, 0.78);
}

.card-header {
  padding: 16px 20px;
  border-bottom: 2px solid rgba(226, 232, 240, 0.3);
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border-radius: 16px 16px 0 0;
  margin: -20px -24px 20px -24px;
}

.card-title {
  font-weight: 700;
  font-size: 17px;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.btn-mini {
  appearance: none;
  border: 2px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.9);
  color: #4a5568;
  border-radius: 10px;
  height: 34px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-mini:hover {
  border-color: rgba(102, 126, 234, 0.9);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.btn-mini:focus,
.btn-mini:focus-visible {
  outline: none;
  border-color: rgba(102, 126, 234, 0.9);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.14);
}

.pill-value {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  min-height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(248, 250, 252, 0.9);
  color: #4a5568;
  font-weight: 600;
  line-height: 1.2;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  /* 仅美化：遮罩半透明 + blur（不改交互） */
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

/* =========================
   本次为第5步，仅做收尾美化，不改布局。
   适用范围：编辑弹窗/编辑表单、空状态、细节统一（focus/圆角/动效）
   ========================= */

.modal-content {
  width: 92%;
  max-width: 920px;
  max-height: 90vh;
  overflow: auto;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

/* 体格检查卡片内嵌分页展示 */
.questionnaire-step.in-card {
  margin-top: 10px;
  margin-bottom: 12px;
}

.questionnaire-loading {
  color: #718096;
  padding: 20px 0;
  text-align: center;
}

.questionnaire-meta {
  color: #718096;
  font-size: 13px;
  margin: 0 0 12px 0;
}
.questionnaire-note {
  margin: 0 0 12px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.08);
  color: #4a5568;
  font-size: 13px;
  line-height: 1.6;
}


.questionnaire-step {
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 12px;
  background: rgba(248, 250, 252, 0.6);
}

.questionnaire-step .step-title {
  font-weight: 800;
  color: #2d3748;
  margin-bottom: 6px;
  font-size: 13px;
}

.step-desc {
  color: #718096;
  font-size: 12px;
  margin-bottom: 8px;
}

.q-row {
  display: flex;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px dashed rgba(226, 232, 240, 0.9);
}

.q-row:last-child {
  border-bottom: none;
}

.q-label {
  width: 190px;
  color: #4a5568;
  font-weight: 700;
  flex-shrink: 0;
}

.q-value-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.q-value {
  color: #2d3748;
  font-weight: 600;
}

.q-help {
  color: #718096;
  font-size: 12px;
  line-height: 1.5;
}

.value-help-inline {
  color: #718096;
  font-size: 12px;
  font-weight: 500;
}

/* 仅新增分页：不改布局，仅用于体格检查弹窗的 step 分页 */
.questionnaire-pager {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: auto;
}

.questionnaire-pager.in-card {
  margin-top: 10px;
}

.questionnaire-pager .pager-center {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.questionnaire-pager .page-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  background: rgba(255, 255, 255, 0.92);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.22s ease;
}

.questionnaire-pager .page-btn:hover:not(:disabled):not(.disabled) {
  border-color: rgba(102, 126, 234, 0.55);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.questionnaire-pager .page-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: rgba(102, 126, 234, 0.55);
  color: #ffffff;
}

.questionnaire-pager .page-btn:disabled,
.questionnaire-pager .page-btn.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.edit-modal {
  max-width: 720px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-modal .modal-body {
  padding: 20px 24px 16px;
}

.edit-modal .modal-header {
  padding: 16px 24px;
}

.edit-modal .modal-footer {
  padding: 12px 24px 18px;
}

.modal-header {
  position: relative;
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 18px;
  appearance: none;
  border: none;
  background: transparent;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  color: #a0aec0;
  padding: 0;
}

.modal-close:hover {
  color: #4a5568;
}

.modal-close:focus {
  outline: none;
}

.form-row .form-label {
  font-size: 13px;
  font-weight: 800;
  color: #2d3748;
  margin-bottom: 8px;
}

.edit-form .input {
  width: 100%;
  /* 统一输入控件：8px 体系（仅视觉，不改交互/校验） */
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.9);
  box-sizing: border-box;
  outline: none;
  resize: vertical;
  transition: all 0.2s ease;
}

.edit-form .input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  background: #ffffff;
}

.edit-form .input:focus-visible {
  outline: none;
}

/* 统一“突兀蓝色边框”：按钮 focus 视觉收束 */
.btn:focus-visible,
.btn-mini:focus-visible,
.modal-close:focus-visible,
.questionnaire-pager .page-btn:focus-visible {
  outline: none;
}

/* 空状态：仅统一视觉（不新增业务信息，不改结构） */
.card-body > .empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 26px 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.7);
  border: 1px dashed rgba(203, 213, 225, 0.85);
  color: #64748b;
  font-weight: 600;
}

.card-body > .empty-tip::before {
  content: '';
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.95) 0%, rgba(118, 75, 162, 0.95) 100%);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.14);
}

/* 风险与责任人弹窗：责任医生（搜索 + 下拉） */
.doctor-select {
  position: relative;
}

.doctor-select .input {
  padding-right: 28px;
}

.doctor-select::after {
  content: '';
  position: absolute;
  right: 12px;
  top: 50%;
  margin-top: -3px;
  border-width: 6px 5px 0 5px;
  border-style: solid;
  border-color: #a0aec0 transparent transparent transparent;
  pointer-events: none;
}

.doctor-dropdown-floating {
  position: fixed;
  z-index: 1000001;
  pointer-events: auto;
}

.doctor-dropdown-floating .dropdown {
  position: relative;
  left: 0;
  right: 0;
  top: 0;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  max-height: 240px;
  overflow-y: auto;
}

.doctor-dropdown-floating .dropdown-item {
  width: 100%;
  text-align: left;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #2d3748;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.doctor-dropdown-floating .dropdown-item:hover {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.12) 0%, rgba(118, 75, 162, 0.12) 100%);
  color: #4c51bf;
}

.doctor-dropdown-floating .dropdown-item.muted {
  cursor: default;
  font-weight: 500;
  color: #a0aec0;
}

.doctor-dropdown-floating .dropdown-item.muted:hover {
  background: transparent;
  color: #a0aec0;
}

.card-subtitle {
  font-size: 12px;
  color: #718096;
  font-weight: 500;
  margin-left: 12px;
}

.card-body {
  padding: 0;
}

.summary-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  /* 本次为第1步，仅美化标题区和顶部概览区，不改布局。 */
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.08), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  transform: scale(1);
  transform-origin: center center;
  will-change: transform, box-shadow;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.28s ease;
  z-index: 2;
}

.summary-bar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.38;
  transition: opacity 0.28s ease;
  z-index: 1;
  pointer-events: none;
}

.summary-bar > * {
  position: relative;
  z-index: 2;
}

.summary-bar:hover {
  transform: scale(1.016);
  border-color: rgba(203, 213, 225, 0.75);
  box-shadow:
    0 16px 36px rgba(102, 126, 234, 0.12),
    0 8px 18px rgba(15, 23, 42, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.10);
}

.summary-bar:hover::before {
  opacity: 0.94;
}
.self-check-card {
  margin-top: 20px;
}

.self-check-body {
  display: flex;
  gap: 20px;
  align-items: stretch;
}

.self-check-left {
  width: 260px;
  flex-shrink: 0;
}

.self-check-right {
  flex: 1;
}

.self-check-grid {
  display: flex;
  gap: 24px;
}

.self-check-col {
  flex: 1;
}

.self-check-panel {
  background: #f7fafc;
  border-radius: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.self-check-panel .panel-title {
  font-size: 13px;
  font-weight: 800;
  color: #2d3748;
  margin-bottom: 10px;
}

.panel-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 4px 0;
}

.panel-label {
  color: #4a5568;
}

.panel-value {
  color: #2d3748;
  font-weight: 600;
}

.summary-left {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 20px;
  align-items: center;
}

.summary-name {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.2px;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.summary-main-complaint {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(254, 215, 215, 0.7), rgba(252, 129, 129, 0.2));
  border: 1px solid rgba(252, 129, 129, 0.6);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 620px;
}

.summary-main-complaint-label {
  font-size: 12px;
  font-weight: 700;
  color: #c53030;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
}

.summary-main-complaint-text {
  font-size: 14px;
  color: #742a2a;
  font-weight: 600;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 8px;
  font-size: 13px;
}

/* =========================
   本次为第3步，仅美化风险与重点信息块，不改布局。
   适用范围：风险与责任人 / 日常自测 / 中医四诊证候强调
   ========================= */

/* 风险与责任人：pill badge（仅作用于该卡片） */
.risk-owner-card .badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  min-height: 28px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.2px;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
}

.risk-owner-card .badge-danger {
  background: linear-gradient(135deg, rgba(254, 202, 202, 0.95) 0%, rgba(254, 226, 226, 0.85) 100%);
  color: #991b1b;
  border: 1px solid rgba(248, 113, 113, 0.55);
}

.risk-owner-card .badge-warning {
  background: linear-gradient(135deg, rgba(254, 249, 195, 0.95) 0%, rgba(253, 230, 138, 0.55) 100%);
  color: #92400e;
  border: 1px solid rgba(245, 158, 11, 0.45);
}

.risk-owner-card .badge-success {
  background: linear-gradient(135deg, rgba(209, 250, 229, 0.92) 0%, rgba(187, 247, 208, 0.68) 100%);
  color: #065f46;
  border: 1px solid rgba(16, 185, 129, 0.38);
}

.risk-owner-card .badge-danger {
  box-shadow: 0 10px 22px rgba(239, 68, 68, 0.12), 0 6px 14px rgba(15, 23, 42, 0.06);
}

.risk-owner-card .doctor-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  min-height: 28px;
  border-radius: 999px;
  background: rgba(102, 126, 234, 0.08);
  border: 1px solid rgba(102, 126, 234, 0.18);
  color: #334155;
  font-weight: 600;
  line-height: 1.2;
}

/* 日常自测：保持网格结构，仅做信息块视觉强化 */
.self-check-card .self-check-panel {
  background: rgba(248, 250, 252, 0.75);
  border-radius: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(226, 232, 240, 0.85);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.04);
}

.self-check-card .panel-label {
  color: #64748b;
  font-weight: 600;
  font-size: 13px;
}

.self-check-card .panel-value {
  color: #0f172a;
  font-weight: 800;
  font-size: 14px;
}

.self-check-card .self-check-col .info-item {
  border-bottom: none;
  padding: 10px 12px;
  margin-bottom: 10px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.75);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.03);
  align-items: center;
}

.self-check-card .self-check-col .info-item:last-child {
  margin-bottom: 0;
}

.self-check-card .self-check-col .info-label {
  min-width: 96px;
  color: #64748b;
  font-weight: 600;
}

.self-check-card .self-check-col .info-label::before {
  width: 3px;
  height: 12px;
  opacity: 0.75;
}

.self-check-card .self-check-col .info-value {
  color: #0f172a;
  font-weight: 800;
  letter-spacing: 0.1px;
}

/* 中医四诊 / 证候强调块：仅作用于中医卡片 */
.tcm-card .pill-value {
  display: inline-flex;
  align-items: flex-start;
  padding: 8px 12px;
  min-height: 32px;
  border-radius: 12px;
  background: rgba(124, 58, 237, 0.06);
  border: 1px solid rgba(124, 58, 237, 0.14);
  color: #1f2937;
  font-weight: 600;
  line-height: 1.7;
  white-space: normal;
}

.tcm-card .tcm-summary {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.10) 0%, rgba(102, 126, 234, 0.08) 100%);
  border-color: rgba(102, 126, 234, 0.22);
  color: #111827;
  box-shadow: 0 10px 22px rgba(102, 126, 234, 0.08);
}

.risk-pill-with-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.summary-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.risk-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.risk-high {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.risk-mid {
  background: linear-gradient(135deg, #fefcbf 0%, #faf089 100%);
  color: #d69e2e;
  border: 1px solid #f6e05e;
}

.risk-low {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #2f855a;
  border: 1px solid #68d391;
}

.risk-action-text {
  font-size: 12px;
  color: #4a5568;
  max-width: 220px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 16px 20px;
  box-shadow: 0 4px 12px rgba(15, 35, 52, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
  transform: scale(1);
  transform-origin: center center;
  will-change: transform, box-shadow;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.28s ease;
}

.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  opacity: 0.38;
  transition: opacity 0.28s ease;
  z-index: 1;
  pointer-events: none;
}

.stat-card > * {
  position: relative;
  z-index: 2;
}

.stat-card:hover {
  /* 四列密集统计卡：hover 更克制 */
  transform: scale(1.016);
  box-shadow:
    0 16px 36px rgba(102, 126, 234, 0.12),
    0 8px 18px rgba(15, 23, 42, 0.06);
  border-color: rgba(203, 213, 225, 0.75);
}

.stat-card:hover::before {
  opacity: 0.94;
}

.stat-label {
  font-size: 13px;
  color: #718096;
  margin-bottom: 8px;
  font-weight: 500;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #2d3748;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.stat-badge {
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 12px;
  background: linear-gradient(135deg, #bee3f8 0%, #90cdf4 100%);
  color: #2c5282;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-badge.danger {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
}

.table-simple {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  overflow: hidden;
}

.table-simple th,
.table-simple td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
}

.table-simple th {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  font-weight: 600;
  color: #2d3748;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table-simple tbody tr {
  transition: all 0.2s ease;
}

.table-simple tbody tr:hover {
  background: rgba(102, 126, 234, 0.05);
  /* 仅美化：hover 不缩放，不改变行高 */
}

.overdue-row {
  background: rgba(254, 226, 226, 0.3);
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.pill.overdue {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.pill-overdue-text {
  margin-left: 8px;
  color: #c53030;
  font-weight: 700;
}

.priority-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.priority-pill.small {
  padding: 2px 8px;
  font-size: 10px;
}

.priority-high {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.priority-mid {
  background: linear-gradient(135deg, #fefcbf 0%, #faf089 100%);
  color: #b7791f;
  border: 1px solid #ecc94b;
}

.priority-low {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #2f855a;
  border: 1px solid #68d391;
}

.priority-normal {
  background: rgba(226, 232, 240, 0.9);
  color: #4a5568;
  border: 1px solid rgba(203, 213, 224, 0.9);
}

.empty-tip {
  text-align: center;
  color: #718096;
  padding: 20px 0;
  font-style: italic;
}

/* =========================
   本次为第4步，仅美化时间轴和趋势图，不改布局。
   ========================= */

.timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  position: relative;
  padding-left: 18px;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 16px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.95) 0%, rgba(118, 75, 162, 0.95) 100%);
  box-shadow: 0 10px 20px rgba(102, 126, 234, 0.18);
  border: 2px solid rgba(255, 255, 255, 0.95);
}

.timeline-item::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 30px;
  bottom: -12px;
  width: 2px;
  background: linear-gradient(180deg, rgba(102, 126, 234, 0.22) 0%, rgba(148, 163, 184, 0.14) 100%);
}

.timeline-item:last-child::after {
  display: none;
}

.timeline-profile {
  list-style: none;
  padding: 0;
  margin: 0;
}

.timeline-profile .timeline-item {
  padding: 12px 0;
  border-left: 3px solid #e2e8f0;
  padding-left: 20px;
  margin-left: 12px;
  position: relative;
  transition: all 0.2s ease;
}

.timeline-profile .timeline-item:hover {
  border-left-color: #667eea;
}

.timeline-profile .timeline-item::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 16px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: 3px solid #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.timeline-profile .timeline-item.empty {
  border-left: none;
  padding-left: 0;
  margin-left: 0;
  color: #718096;
  font-style: italic;
}

.timeline-profile .timeline-item.empty::before {
  display: none;
}

.timeline-profile .timeline-date {
  color: #4a5568;
  font-size: 12px;
  margin-right: 12px;
  font-weight: 600;
}

.timeline-profile .timeline-desc {
  color: #2d3748;
  font-size: 14px;
  font-weight: 500;
}

.chart-container {
  width: 100%;
  height: 280px;
  /* 仅美化：毛玻璃图表容器（不改尺寸/位置） */
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 10px;
  border: 1px solid rgba(226, 232, 240, 0.70);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.08), 0 4px 12px rgba(15, 23, 42, 0.04);
}

.latest-indicator {
  margin-top: 12px;
  font-size: 13px;
  color: #4a5568;
  font-weight: 500;
  padding: 8px 12px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 8px;
}

.card-grid {
  display: grid;
  gap: 20px;
  margin-bottom: 20px;
}

.card-grid.cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.card-grid.cols-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.card-grid .card {
  margin-bottom: 0;
}

/* 本次为第2步，仅美化详情卡片和字段层级，不改布局。 */
.info-item {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.3);
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  /* 仅美化：label 统一层级 */
  color: #6b7280;
  font-size: 13px;
  min-width: 100px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.info-label::before {
  content: '';
  width: 3px;
  height: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
  margin-right: 8px;
}

.info-value {
  /* 仅美化：value 统一层级 */
  color: #1f2937;
  font-size: 14px;
  flex: 1;
  font-weight: 600;
  line-height: 1.7;
  word-break: break-word;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(237, 242, 247, 0.9);
  color: #2d3748;
}

.tag-plain {
  background: rgba(237, 242, 247, 0.8);
  color: #4a5568;
}

.tag-tcm {
  background: linear-gradient(135deg, #faf5ff 0%, #e9d8fd 100%);
  color: #6b46c1;
}

.tag-tcm-detail {
  background: rgba(237, 242, 247, 0.9);
  color: #2d3748;
}

.tag-main-complaint {
  background: linear-gradient(135deg, #fff5f5 0%, #fed7d7 100%);
  color: #c53030;
}

.tongue-image {
  max-width: 100px;
  max-height: 100px;
  margin-left: 10px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.card-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid rgba(226, 232, 240, 0.9);
  color: #4a5568;
  backdrop-filter: blur(10px);
}

.btn-ghost:hover {
  border-color: rgba(102, 126, 234, 0.6);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.btn-primary:hover {
  /* 仅美化：hover 不位移 */
  box-shadow: 0 12px 26px rgba(102, 126, 234, 0.26);
}

.btn:focus,
.btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.14);
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.badge-danger {
  background: linear-gradient(135deg, #fed7d7 0%, #feb2b2 100%);
  color: #c53030;
  border: 1px solid #fc8181;
}

.badge-warning {
  background: linear-gradient(135deg, #fefcbf 0%, #faf089 100%);
  color: #d69e2e;
  border: 1px solid #f6e05e;
}

.badge-success {
  background: linear-gradient(135deg, #c6f6d5 0%, #9ae6b4 100%);
  color: #2f855a;
  border: 1px solid #68d391;
}

.link-primary {
  color: #667eea;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s ease;
  position: relative;
}

.link-primary::after {
  content: '';
  position: absolute;
  width: 0;
  height: 2px;
  bottom: -2px;
  left: 0;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.3s ease;
}

.link-primary:hover {
  color: #764ba2;
}

.link-primary:hover::after {
  width: 100%;
}

.timeline-item {
  padding: 16px 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.2s ease;
}

.timeline-item:hover {
  /* 仅美化：hover 不位移 */
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.10);
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  color: #4a5568;
  font-weight: 500;
}

.timeline-title {
  font-weight: 700;
  color: #2d3748;
  font-size: 14px;
}

.timeline-content {
  font-size: 14px;
  color: #2d3748;
  margin-bottom: 8px;
  line-height: 1.5;
}

.timeline-footer {
  font-size: 12px;
  color: #718096;
  font-weight: 500;
}

.loading {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
  font-size: 16px;
  font-weight: 500;
}

.loading::before {
  content: '';
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 3px solid #e2e8f0;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 12px;
  vertical-align: middle;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error {
  padding: 16px 20px;
  background: rgba(254, 226, 226, 0.9);
  color: #c53030;
  border-radius: 12px;
  border: 1px solid #fc8181;
  font-weight: 500;
  backdrop-filter: blur(10px);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .patient-detail-page {
    padding: 12px;
  }
  
  .detail-container {
    padding: 0 12px 20px;
  }
  
  .card-grid.cols-2,
  .card-grid.cols-3 {
    grid-template-columns: 1fr;
  }
  
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  
  .summary-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .summary-actions {
    justify-content: center;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
  }
  
  .chart-container {
    height: 240px;
  }
}

@media (max-width: 480px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  
  .summary-name {
    font-size: 20px;
  }
  
  .card {
    padding: 16px 20px;
  }
}
</style>


