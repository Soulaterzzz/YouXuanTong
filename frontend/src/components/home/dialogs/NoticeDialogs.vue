<template>
  <el-dialog
    ref="dialogRef"
    @open="handleDialogOpen"
    @closed="handleDialogClose"
    v-model="noticeDialogVisible"
    :title="noticeDialogTitleText"
    :class="noticeDialogMode === 'manage' ? 'notice-dialog notice-dialog--manage' : 'notice-dialog notice-dialog--view'"
    :width="noticeDialogMode === 'manage' ? '860px' : '720px'"
    :align-center="noticeDialogMode === 'view'"
    :top="noticeDialogMode === 'manage' ? '24px' : '15vh'"
    :modal="true"
    :modal-class="noticeDialogMode === 'manage' ? 'notice-dialog-modal' : 'notice-dialog-modal notice-dialog-modal--view'"
    :close-on-click-modal="noticeDialogMode !== 'manage'"
    destroy-on-close
    @close="closeNoticeDialog"
  >
    <div class="notice-dialog-shell">
      <template v-if="noticeDialogMode !== 'manage'">
        <div class="notice-dialog-head">
          <div class="notice-dialog-copy">
            <span class="notice-dialog-kicker">首页通知</span>
            <h3>
              {{
                noticeDialogCurrentNotice?.title
                  || '当前没有通知内容'
              }}
            </h3>
            <p v-if="noticeDialogCurrentNotice">
              {{
                noticeDialogCurrentNotice.publishedAt
                  ? formatNoticeTime(noticeDialogCurrentNotice.publishedAt)
                  : '系统暂无可浏览的通知，首页每次打开仍会自动弹出该窗口。'
              }}
            </p>
            <p v-else>
              系统暂无可浏览的通知，首页每次打开仍会自动弹出该窗口。
            </p>
          </div>
          <div class="notice-dialog-head-meta">
            <div class="notice-dialog-counter">{{ noticeDialogCounterText }}</div>
          </div>
        </div>

        <div v-if="noticeDialogCurrentNotice" class="notice-dialog-card">
          <div class="notice-dialog-card-head">
            <el-tag effect="plain" type="danger">第 {{ noticeDialogIndex + 1 }} 条</el-tag>
            <span class="notice-dialog-card-total">{{ noticeDialogDisplayList.length }} 条通知</span>
          </div>
          <div class="notice-dialog-content">
            {{ noticeDialogCurrentNotice.content || '-' }}
          </div>
        </div>

        <el-empty v-else description="暂无通知公告" :image-size="92" />

        <div v-if="noticeDialogDisplayList.length > 1" class="notice-dialog-strip">
          <button
            v-for="(item, index) in noticeDialogDisplayList"
            :key="item.id || `${item.title}-${index}`"
            type="button"
            class="notice-dialog-chip"
            :class="{ active: index === noticeDialogIndex }"
            @click="setNoticeDialogIndex(index)"
          >
            <span class="notice-dialog-chip-order">{{ index + 1 }}</span>
            <span class="notice-dialog-chip-text">{{ item.title }}</span>
            <el-icon class="notice-dialog-chip-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </template>

      <section v-if="isAdmin && noticeDialogMode === 'manage'" class="notice-dialog-manage-shell">
        <div class="notice-dialog-manage-toolbar">
          <el-button type="primary" plain @click="addNoticeManageRow">
            <el-icon><Plus /></el-icon>
            新建通知
          </el-button>
          <span class="notice-dialog-manage-tip-inline">点击编辑会打开单独弹窗，保存后再调整排序。</span>
        </div>

        <div v-if="noticeManageList.length" class="notice-dialog-manage-list">
          <article
            v-for="(item, index) in noticeManageList"
            :key="item.id || `${item.title}-${index}`"
            class="notice-dialog-manage-item"
            @focusin="setNoticeDialogIndex(index)"
          >
            <div class="notice-dialog-manage-item-head">
              <div class="notice-dialog-manage-order">第 {{ index + 1 }} 条</div>
              <div class="notice-dialog-manage-actions">
                <el-button text @click="handleNoticeManageRowEdit(item.id)">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button text @click="moveNoticeManageRow(index, -1)" :disabled="index === 0">
                  <el-icon><ArrowUp /></el-icon>
                  上移
                </el-button>
                <el-button text @click="moveNoticeManageRow(index, 1)" :disabled="index === noticeManageList.length - 1">
                  <el-icon><ArrowDown /></el-icon>
                  下移
                </el-button>
                <el-button text type="danger" @click="removeNoticeManageRow(index)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
            <div class="notice-dialog-manage-summary">
              <h4>{{ item.title || '未命名通知' }}</h4>
              <p>{{ item.content || '暂无正文内容，点击编辑补充通知内容。' }}</p>
            </div>
          </article>
        </div>

        <el-empty
          v-else
          class="notice-dialog-manage-empty"
          description="暂无通知，点击“新建通知”开始编辑"
          :image-size="96"
        />

        <div class="notice-dialog-manage-tip">
          点击编辑进入单独弹窗修改内容，保存并发布后首页列表会按当前顺序同步刷新。
        </div>
      </section>

      <el-dialog
        v-model="noticeManageEditDialogVisible"
        :title="noticeManageEditDialogTitle"
        width="640px"
        align-center
        append-to-body
        destroy-on-close
        :close-on-click-modal="false"
        @close="closeNoticeManageEditDialog"
        @closed="handleNoticeManageEditDialogClosed"
      >
        <div class="notice-manage-edit-dialog">
          <div class="notice-manage-edit-dialog-head">
            <el-icon><Edit /></el-icon>
            <span>
              {{
                noticeManageEditIndex >= 0
                  ? '修改当前通知标题和正文后保存。'
                  : '填写标题和正文后保存，新通知会加入管理列表。'
              }}
            </span>
          </div>

          <el-form class="notice-manage-edit-form" :model="noticeManageEditForm" label-position="top">
            <el-form-item label="通知标题">
              <el-input
                ref="noticeManageEditTitleInput"
                v-model="noticeManageEditForm.title"
                maxlength="30"
                show-word-limit
                placeholder="请输入通知标题"
                clearable
              />
            </el-form-item>
            <el-form-item label="通知内容">
              <el-input
                v-model="noticeManageEditForm.content"
                type="textarea"
                :rows="6"
                maxlength="500"
                show-word-limit
                resize="none"
                placeholder="请输入通知内容"
              />
            </el-form-item>
          </el-form>
        </div>

        <template #footer>
          <span class="dialog-footer notice-dialog-footer">
            <el-button @click="closeNoticeManageEditDialog">取消</el-button>
            <el-button type="primary" @click="submitNoticeManageEditDialog">
              保存
            </el-button>
          </span>
        </template>
      </el-dialog>
    </div>

    <template #footer>
      <span v-if="isAdmin && noticeDialogMode === 'manage'" class="dialog-footer notice-dialog-footer">
        <el-button @click="closeNoticeManageDialog" :disabled="noticeManageSubmitting">取消</el-button>
        <el-button type="primary" :loading="noticeManageSubmitting" @click="submitNoticeManageList">
          保存并发布
        </el-button>
      </span>
      <span v-else class="dialog-footer notice-dialog-footer">
        <el-button
          @click="showPreviousNotice"
          :disabled="noticeDialogIndex <= 0 || !noticeDialogDisplayList.length"
        >
          上一条
        </el-button>
        <el-button
          @click="showNextNotice"
          :disabled="noticeDialogIndex >= noticeDialogDisplayList.length - 1 || !noticeDialogDisplayList.length"
        >
          下一条
        </el-button>
        <el-button type="primary" @click="closeNoticeDialog">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import { ArrowDown, ArrowRight, ArrowUp, Delete, Edit, Plus } from '@element-plus/icons-vue'

import { formatNoticeTime } from '@/utils/home/notice.js'
import { useHomeDialogBridge } from '@/composables/home/useHomeDialogBridge.js'

export default {
  name: 'NoticeDialogs',
  components: {
    ArrowDown,
    ArrowRight,
    ArrowUp,
    Delete,
    Edit,
    Plus
  },
  setup() {
    const { bridge } = useHomeDialogBridge()
    bridge.formatNoticeTime = formatNoticeTime
    return bridge
  }
}
</script>
