<template>
  <div class="data-mgmt">
    <div class="data-mgmt__grid">
      <!-- 文件上传区 -->
      <div class="data-mgmt__left">
        <ChartCard title="数据文件上传">
          <div class="upload-area">
            <div
              class="upload-zone"
              :class="{ 'upload-zone--drag': isDragging, 'upload-zone--has-file': selectedFile }"
              @click="triggerFileInput"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="handleDrop"
            >
              <input
                ref="fileInputRef"
                type="file"
                accept=".csv,.xlsx,.xls"
                style="display: none"
                @change="handleFileSelect"
              />
              <div v-if="!selectedFile" class="upload-zone__placeholder">
                <div class="upload-icon">+</div>
                <div class="upload-text">点击或拖拽文件到此处上传</div>
                <div class="upload-hint">支持 CSV / Excel 文件，最大 100MB</div>
              </div>
              <div v-else class="upload-zone__file">
                <div class="file-icon">📄</div>
                <div class="file-info">
                  <div class="file-name">{{ selectedFile.name }}</div>
                  <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
                </div>
                <button class="btn btn--text" @click.stop="clearFile">移除</button>
              </div>
            </div>

            <div class="upload-actions">
              <button
                class="btn btn--default"
                :disabled="!selectedFile || uploading"
                @click="handleUpload"
              >
                {{ uploading ? `解析中 ${uploadProgress}%` : '仅解析预览' }}
              </button>
              <button
                class="btn btn--primary"
                :disabled="!selectedFile || uploading"
                @click="handleImport"
              >
                {{ importing ? `导入中 ${uploadProgress}%` : '上传并导入数据库' }}
              </button>
            </div>

            <div v-if="uploadMessage" class="upload-message" :class="`upload-message--${uploadMessageType}`">
              {{ uploadMessage }}
            </div>

            <!-- 上传文件解析预览 -->
            <div v-if="uploadPreviewData.length" class="upload-preview">
              <div class="upload-preview__title">解析预览（{{ uploadPreviewData.length }} 行）</div>
              <div class="table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th v-for="col in uploadPreviewColumns" :key="col">{{ col }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, idx) in uploadPreviewData" :key="idx">
                      <td v-for="col in uploadPreviewColumns" :key="col">{{ formatValue(row[col]) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </ChartCard>

        <ChartCard title="数据库数据预览">
          <template #extra>
            <select v-model="previewTable" class="control-select" @change="loadPreview">
              <option value="cars">cars 表</option>
              <option value="generic_data">generic_data 表</option>
            </select>
          </template>
          <div class="preview-controls">
            <label class="control-label">显示行数：</label>
            <select v-model="previewLimit" class="control-select" @change="loadPreview">
              <option :value="20">20 行</option>
              <option :value="50">50 行</option>
              <option :value="100">100 行</option>
            </select>
            <button class="btn btn--default btn--sm" @click="loadPreview">刷新</button>
          </div>
          <div class="table-wrapper" v-if="previewData.length">
            <table class="data-table">
              <thead>
                <tr>
                  <th v-for="col in previewColumns" :key="col">{{ col }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in previewData" :key="idx">
                  <td v-for="col in previewColumns" :key="col">{{ formatValue(row[col]) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else-if="!previewLoading" class="empty-tip">暂无数据</div>
        </ChartCard>
      </div>

      <!-- 报表管理区 -->
      <div class="data-mgmt__right">
        <ChartCard title="全维度分析报表">
          <div class="report-actions">
            <button class="btn btn--default" :disabled="reportLoading" @click="generateReport">
              {{ reportLoading ? '生成中（请稍候，约30-60秒）...' : '生成报表(JSON)' }}
            </button>
            <button class="btn btn--primary" :disabled="saveLoading" @click="saveReport">
              {{ saveLoading ? '保存中（请稍候，约30-60秒）...' : '生成并保存CSV' }}
            </button>
            <button class="btn btn--default btn--sm" @click="loadReportList">刷新列表</button>
          </div>

          <div v-if="reportMessage" class="upload-message" :class="`upload-message--${reportMessageType}`">
            {{ reportMessage }}
            <button v-if="savedCsvFilename" class="btn btn--primary btn--sm" @click="downloadReport(savedCsvFilename)">
              下载 CSV
            </button>
          </div>

          <!-- 生成的 JSON 报表预览 -->
          <div v-if="reportData" class="report-preview">
            <div class="report-preview__title">
              <span>报表预览（部分）</span>
              <button class="btn btn--primary btn--sm" @click="downloadJsonReport">下载完整 JSON</button>
            </div>
            <div class="report-preview__content">
              <pre>{{ JSON.stringify(reportData, null, 2).slice(0, 2000) }}{{ JSON.stringify(reportData).length > 2000 ? '\n... (更多内容请下载查看)' : '' }}</pre>
            </div>
          </div>
        </ChartCard>

        <ChartCard title="已保存的报表列表">
          <div v-if="reportList.length" class="report-list">
            <div v-for="item in reportList" :key="item.filename || item.name" class="report-item">
              <div class="report-item__icon">📊</div>
              <div class="report-item__info">
                <div class="report-item__name">{{ item.filename || item.name }}</div>
                <div class="report-item__meta">
                  <span v-if="item.size">{{ formatFileSize(item.size) }}</span>
                  <span v-if="item.modified || item.createTime">{{ item.modified || item.createTime }}</span>
                </div>
              </div>
              <button class="btn btn--default btn--sm" @click="downloadReport(item.filename || item.name)">
                下载
              </button>
            </div>
          </div>
          <div v-else-if="!reportListLoading" class="empty-tip">暂无已保存的报表</div>
        </ChartCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ChartCard from '@/components/ChartCard.vue'
import { dataApi } from '@/api'

// 文件上传
const fileInputRef = ref(null)
const selectedFile = ref(null)
const isDragging = ref(false)
const uploading = ref(false)
const importing = ref(false)
const uploadProgress = ref(0)
const uploadMessage = ref('')
const uploadMessageType = ref('info')

// 上传文件解析预览
const uploadPreviewData = ref([])
const uploadPreviewHeaders = ref([])

const uploadPreviewColumns = computed(() => {
  if (uploadPreviewHeaders.value.length) return uploadPreviewHeaders.value
  if (!uploadPreviewData.value.length) return []
  const keys = []
  uploadPreviewData.value.forEach((row) => {
    Object.keys(row).forEach((k) => !keys.includes(k) && keys.push(k))
  })
  return keys
})

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleFileSelect = (e) => {
  const file = e.target.files[0]
  if (file) selectedFile.value = file
}

const handleDrop = (e) => {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (file) selectedFile.value = file
}

const clearFile = () => {
  selectedFile.value = null
  if (fileInputRef.value) fileInputRef.value.value = ''
  uploadPreviewData.value = []
  uploadPreviewHeaders.value = []
  uploadMessage.value = ''
}

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  while (bytes >= 1024 && i < units.length - 1) {
    bytes /= 1024
    i++
  }
  return `${bytes.toFixed(2)} ${units[i]}`
}

const handleUpload = async () => {
  if (!selectedFile.value) return
  uploading.value = true
  uploadProgress.value = 0
  uploadMessage.value = ''
  uploadPreviewData.value = []
  uploadPreviewHeaders.value = []
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await dataApi.upload(formData, (e) => {
      if (e.total) uploadProgress.value = Math.round((e.loaded * 100) / e.total)
    })
    // 后端返回 { success, filename, headers, rowCount, preview }
    if (res?.success === false) {
      uploadMessage.value = '解析失败：' + (res.error || '未知错误')
      uploadMessageType.value = 'error'
    } else {
      uploadMessage.value = `文件解析成功，共 ${res.rowCount ?? 0} 行，预览前 ${Math.min(20, res.preview?.length || 0)} 行`
      uploadMessageType.value = 'success'
      // 提取预览数据
      if (Array.isArray(res)) {
        uploadPreviewData.value = res
      } else if (res?.preview) {
        uploadPreviewData.value = res.preview
        uploadPreviewHeaders.value = res.headers || []
      } else if (res?.rows) {
        uploadPreviewData.value = res.rows
      } else if (res?.data) {
        uploadPreviewData.value = res.data
      }
    }
  } catch (e) {
    uploadMessage.value = '上传失败：' + (e.message || '未知错误')
    uploadMessageType.value = 'error'
  } finally {
    uploading.value = false
  }
}

const handleImport = async () => {
  if (!selectedFile.value) return
  importing.value = true
  uploadProgress.value = 0
  uploadMessage.value = ''
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await dataApi.import(formData, (e) => {
      if (e.total) uploadProgress.value = Math.round((e.loaded * 100) / e.total)
    })
    uploadMessage.value = '导入成功：' + (res?.importedCount != null ? `${res.importedCount} 条数据已写入数据库` : '数据已写入数据库')
    uploadMessageType.value = 'success'
    // 刷新预览
    loadPreview()
  } catch (e) {
    uploadMessage.value = '导入失败：' + (e.message || '未知错误')
    uploadMessageType.value = 'error'
  } finally {
    importing.value = false
  }
}

// 数据预览
const previewTable = ref('cars')
const previewLimit = ref(50)
const previewData = ref([])
const previewLoading = ref(false)

const previewColumns = computed(() => {
  if (!previewData.value.length) return []
  const keys = []
  previewData.value.forEach((row) => {
    Object.keys(row).forEach((k) => !keys.includes(k) && keys.push(k))
  })
  return keys
})

const loadPreview = async () => {
  previewLoading.value = true
  try {
    const res = await dataApi.preview({ table: previewTable.value, limit: previewLimit.value })
    previewData.value = Array.isArray(res) ? res : (res?.rows || res?.data || [])
  } catch (e) {
    previewData.value = []
  } finally {
    previewLoading.value = false
  }
}

const formatValue = (val) => {
  if (val === null || val === undefined) return '-'
  return String(val)
}

// 报表
const reportLoading = ref(false)
const saveLoading = ref(false)
const reportListLoading = ref(false)
const reportData = ref(null)
const reportList = ref([])
const reportMessage = ref('')
const reportMessageType = ref('info')
const savedCsvFilename = ref('')

const generateReport = async () => {
  reportLoading.value = true
  reportMessage.value = ''
  savedCsvFilename.value = ''
  try {
    const res = await dataApi.generateReport()
    // 后端返回 { success, report }，提取 report 内容
    reportData.value = res?.report || res
    reportMessage.value = '报表生成成功，可点击下方按钮下载完整 JSON'
    reportMessageType.value = 'success'
  } catch (e) {
    reportMessage.value = '报表生成失败：' + (e.message || '未知错误')
    reportMessageType.value = 'error'
  } finally {
    reportLoading.value = false
  }
}

const saveReport = async () => {
  saveLoading.value = true
  reportMessage.value = ''
  savedCsvFilename.value = ''
  try {
    const res = await dataApi.saveReport()
    // 后端返回 { success, filePath, message }，从 filePath 提取文件名
    const filename = res?.filePath ? res.filePath.split(/[\\/]/).pop() : null
    savedCsvFilename.value = filename || ''
    reportMessage.value = filename ? `报表已保存：${filename}` : '报表已保存'
    reportMessageType.value = 'success'
    loadReportList()
  } catch (e) {
    reportMessage.value = '保存失败：' + (e.message || '未知错误')
    reportMessageType.value = 'error'
  } finally {
    saveLoading.value = false
  }
}

const loadReportList = async () => {
  reportListLoading.value = true
  try {
    const res = await dataApi.reportList()
    // 后端返回 { success, reports: [...] }
    reportList.value = Array.isArray(res) ? res : (res?.reports || res?.files || res?.data || [])
  } catch (e) {
    reportList.value = []
  } finally {
    reportListLoading.value = false
  }
}

const downloadJsonReport = () => {
  const json = JSON.stringify(reportData.value, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `analysis_report_${Date.now()}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const downloadReport = async (filename) => {
  try {
    const blob = await dataApi.downloadReport(filename)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    reportMessage.value = '下载失败：' + (e.message || '未知错误')
    reportMessageType.value = 'error'
  }
}

// 初始化加载
loadPreview()
loadReportList()
</script>

<style scoped>
.data-mgmt {
  padding: 4px;
}

.data-mgmt__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.data-mgmt__left,
.data-mgmt__right {
  min-width: 0;
}

@media (max-width: 1024px) {
  .data-mgmt__grid {
    grid-template-columns: 1fr;
  }
}

.upload-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-zone {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 32px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.upload-zone:hover {
  border-color: #5470c6;
  background: #f0f7ff;
}

.upload-zone--drag {
  border-color: #5470c6;
  background: #e6f0ff;
}

.upload-zone--has-file {
  border-style: solid;
  background: #f0fdf4;
  border-color: #91cc75;
}

.upload-zone__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  font-size: 36px;
  color: #9ca3af;
  font-weight: 300;
  line-height: 1;
}

.upload-text {
  font-size: 14px;
  color: #4b5563;
}

.upload-hint {
  font-size: 12px;
  color: #9ca3af;
}

.upload-zone__file {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
}

.file-icon {
  font-size: 32px;
}

.file-info {
  flex: 1;
  text-align: left;
}

.file-name {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
  word-break: break-all;
}

.file-size {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.upload-actions {
  display: flex;
  gap: 8px;
}

.upload-actions .btn {
  flex: 1;
}

.btn {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  border-color: #5470c6;
  color: #5470c6;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.btn--primary {
  background: #5470c6;
  color: #fff;
  border-color: #5470c6;
}

.btn--primary:hover:not(:disabled) {
  background: #4060b0;
  color: #fff;
}

.btn--default {
  background: #fff;
}

.btn--sm {
  padding: 4px 10px;
  font-size: 12px;
}

.btn--text {
  border: none;
  background: transparent;
  color: #6b7280;
  padding: 4px 8px;
}

.btn--text:hover {
  color: #ee6666;
}

.upload-message {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
}

.upload-message--info {
  background: #e0f2fe;
  color: #0369a1;
}

.upload-message--success {
  background: #dcfce7;
  color: #166534;
}

.upload-message--error {
  background: #fee2e2;
  color: #991b1b;
}

.upload-preview {
  margin-top: 4px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
}

.upload-preview__title {
  padding: 8px 12px;
  background: #f5f7fa;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e0e0e0;
}

.upload-preview .table-wrapper {
  max-height: 360px;
}

.preview-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 4px;
}

.control-label {
  font-size: 13px;
  color: #6b7280;
}

.control-select {
  padding: 5px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;
  color: #374151;
  background: #fff;
  cursor: pointer;
}

.control-select:focus {
  outline: none;
  border-color: #5470c6;
}

.table-wrapper {
  overflow: auto;
  max-height: 420px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead {
  position: sticky;
  top: 0;
  background: #f5f7fa;
  z-index: 1;
}

.data-table th,
.data-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

.data-table th {
  font-weight: 600;
  color: #374151;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.empty-tip {
  padding: 40px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.report-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.report-preview {
  margin-top: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.report-preview__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.report-preview__content {
  padding: 12px;
  max-height: 300px;
  overflow: auto;
  background: #fafafa;
}

.report-preview__content pre {
  margin: 0;
  font-size: 12px;
  color: #4b5563;
  white-space: pre-wrap;
  word-break: break-all;
}

.report-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.report-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  transition: all 0.2s;
}

.report-item:hover {
  border-color: #5470c6;
  background: #f0f7ff;
}

.report-item__icon {
  font-size: 24px;
}

.report-item__info {
  flex: 1;
  min-width: 0;
}

.report-item__name {
  font-size: 13px;
  color: #1f2937;
  font-weight: 500;
  word-break: break-all;
}

.report-item__meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}
</style>
