<template>
  <!-- 通知详情弹窗 -->
  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose" v-model="noticeDetailDialogVisible" :title="noticeDetail.title" width="600px">
    <div class="notice-detail-content">
      <div class="notice-detail-meta">
        <el-icon><Clock /></el-icon>
        <span>发布时间：{{ noticeDetail.publishedAt ? new Date(noticeDetail.publishedAt).toLocaleString('zh-CN', { hour12: false }) : '未知' }}</span>
      </div>
      <el-divider />
      <div class="notice-detail-body">
        {{ noticeDetail.content }}
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button type="primary" @click="noticeDetailDialogVisible = false">确定</el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
    v-model="noticePopupDialogVisible"
    title="首页通知栏"
    width="980px"
    :close-on-click-modal="false"
    class="notice-popup-dialog"
  >
    <div class="notice-popup-shell">
      <aside class="notice-popup-list">
        <div class="notice-popup-list-header">
          <span>最新通知</span>
          <el-tag type="info" effect="plain">{{ publishedNoticeList.length }}</el-tag>
        </div>
        <div v-if="publishedNoticeList.length" class="notice-popup-items">
          <button
            v-for="(item, index) in publishedNoticeList"
            :key="item.id || index"
            type="button"
            class="notice-popup-item"
            :class="{ active: index === noticePopupSelectedIndex }"
            @click="selectNoticePopupItem(index)"
          >
            <div class="notice-popup-index">{{ index + 1 }}</div>
            <div class="notice-popup-item-body">
              <h4>{{ item.title }}</h4>
              <p>{{ item.content }}</p>
            </div>
          </button>
        </div>
        <el-empty v-else description="暂无通知公告" :image-size="84" />
      </aside>

      <section class="notice-popup-detail" v-if="noticePopupCurrent">
        <div class="notice-popup-eyebrow">首页弹窗公告</div>
        <h3>{{ noticePopupCurrent.title }}</h3>
        <div class="notice-popup-meta">
          <el-icon><Clock /></el-icon>
          <span>发布时间：{{ noticePopupCurrent.publishedAt ? new Date(noticePopupCurrent.publishedAt).toLocaleString('zh-CN', { hour12: false }) : '未知' }}</span>
        </div>
        <div class="notice-popup-content">
          {{ noticePopupCurrent.content }}
        </div>
      </section>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="closeNoticePopup">关闭</el-button>
        <el-button
          type="primary"
          :disabled="!noticePopupCurrent"
          @click="openNoticeFromPopup"
        >
          查看详情
        </el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog ref="dialogRef" @open="handleDialogOpen" @closed="handleDialogClose"
    v-model="noticePublishDialogVisible"
    title="发布通知"
    width="780px"
    :close-on-click-modal="false"
    class="notice-publish-dialog"
  >
    <div class="notice-admin-layout">
      <div class="notice-editor-panel">
        <el-form label-position="top">
          <el-form-item label="通知标题">
            <el-input
              v-model="noticeDraft.title"
              maxlength="30"
              show-word-limit
              placeholder="例如：系统升级提醒"
            />
          </el-form-item>
          <el-form-item label="通知内容">
            <el-input
              v-model="noticeDraft.content"
              type="textarea"
              :rows="5"
              maxlength="200"
              show-word-limit
              placeholder="请输入首页要展示的通知内容"
            />
          </el-form-item>
        </el-form>
        <div class="notice-editor-actions">
          <el-button @click="resetNoticeDraft">重置输入</el-button>
          <el-button type="primary" @click="saveNoticeDraftItem">
            {{ editingNoticeIndex > -1 ? '更新通知' : '加入列表' }}
          </el-button>
        </div>
      </div>

      <div class="notice-list-panel">
        <div class="notice-list-header">
          <span>待发布通知（{{ noticeDraftList.length }}）</span>
          <div class="notice-list-actions">
            <el-button text @click="clearNoticeDraftList" :disabled="!noticeDraftList.length">
              清空列表
            </el-button>
          </div>
        </div>

        <div v-if="noticeDraftList.length" class="notice-list">
          <div v-for="(item, index) in noticeDraftList" :key="item.id || index" class="notice-list-item">
            <div class="notice-list-order">{{ index + 1 }}</div>
            <div class="notice-list-content">
              <h4>{{ item.title }}</h4>
              <p>{{ item.content }}</p>
            </div>
            <div class="notice-list-item-actions">
              <el-button text type="primary" @click="editNoticeItem(index)">编辑</el-button>
              <el-button text type="danger" @click="removeNoticeItem(index)">删除</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无待发布通知" :image-size="80" />
      </div>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="noticePublishDialogVisible = false">取消</el-button>
        <el-button type="success" @click="publishNotices" :disabled="!noticeDraftList.length">发布通知</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { Clock } from '@element-plus/icons-vue'
import { useHomeDialogBridge } from '@/composables/home/useHomeDialogBridge.js'

export default {
  name: 'NoticeDialogs',
  components: {
    Clock
  },
  setup() {
    const { bridge } = useHomeDialogBridge()
    return bridge
  }
}
</script>
