<template>
  <div class="page home-page">
    <section class="workbench-layout">
      <div class="workbench-main">
        <section class="focus-panel surface-panel">
          <div class="panel-head">
            <div class="panel-copy">
              <span class="panel-kicker">{{ isAdmin ? '运营重点' : '销售重点' }}</span>
              <h2>{{ isAdmin ? '业务数据' : '控制台' }}</h2>
            </div>
          </div>

          <div class="focus-card-grid">
            <article v-for="card in focusCards" :key="card.key" class="focus-card" :class="card.tone">
              <div class="focus-card-top">
                <div class="focus-card-icon">
                  <el-icon><component :is="card.icon" /></el-icon>
                </div>
                <span class="focus-card-kicker">{{ card.kicker }}</span>
              </div>

              <div class="focus-card-body">
                <h3>{{ card.title }}</h3>
                <div class="focus-card-value">{{ card.value }}</div>
                <p>{{ card.description }}</p>
              </div>

              <div class="focus-card-foot">
                <span class="focus-card-note">{{ card.note }}</span>
                <el-button :type="card.buttonType" plain @click="$emit(card.event)">
                  {{ card.actionLabel }}
                </el-button>
              </div>
            </article>
          </div>
        </section>
      </div>

      <aside class="notice-panel surface-panel">
        <div class="panel-head notice-panel-head">
          <span class="panel-kicker">通知公告</span>
          <div class="notice-panel-meta">
            <el-tag effect="plain" type="info">{{ noticeCount }} 条</el-tag>
            <el-tag v-if="latestPublishedNotice" effect="plain" type="danger">最新发布</el-tag>
            <el-tag effect="plain" type="info">{{ publishedNoticeTimeText }}</el-tag>
          </div>
        </div>

        <section v-if="latestPublishedNotice" class="notice-banner" aria-live="polite">
          <div class="notice-banner-copy">
            <el-tag effect="plain" type="danger">首条通知</el-tag>
            <button type="button" class="notice-banner-title" @click="$emit('open-notice-dialog', 0)">
              {{ latestPublishedNotice.title }}
            </button>
            <p>{{ formatNoticeTime(latestPublishedNotice.publishedAt) }}</p>
          </div>
          <el-button
            type="primary"
            plain
            class="notice-banner-action"
            @click="isAdmin ? $emit('open-notice-manage') : $emit('open-notice-dialog', 0)"
          >
            {{ isAdmin ? '管理通知' : '浏览' }}
          </el-button>
        </section>

        <section v-else class="notice-banner notice-banner-empty" aria-live="polite">
          <div class="notice-banner-copy">
            <el-tag effect="plain" type="info">通知公告</el-tag>
            <h3>当前没有通知内容</h3>
            <p>系统仍会在每次进入首页时弹出通知窗口。</p>
          </div>
        </section>

        <div class="notice-list">
          <button
            v-for="(item, index) in noticePreviewList"
            :key="item.id || `${item.title}-${index}`"
            type="button"
            class="notice-list-item"
            @click="$emit('open-notice-dialog', index)"
          >
            <div class="notice-list-order">{{ index + 1 }}</div>
            <div class="notice-list-content">
              <div class="notice-list-meta">
                <h3>{{ item.title }}</h3>
                <span>{{ formatNoticeTime(item.publishedAt) }}</span>
              </div>
            </div>
            <el-icon class="notice-list-arrow"><ArrowRight /></el-icon>
          </button>

          <el-empty v-if="!noticePreviewList.length" description="暂无通知公告" :image-size="88" />
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  ArrowRight,
  Goods,
  Collection,
  Coin,
  Wallet
} from '@element-plus/icons-vue'

import { formatNoticeTime, sortNoticeListByPublishedAtAsc } from '@/utils/home/notice.js'
import { formatMoney } from '@/utils/home/product.js'

const props = defineProps({
  isAdmin: { type: Boolean, default: false },
  currentUser: { type: String, default: '访客' },
  balance: { type: Number, default: 0 },
  stats: { type: Object, required: true },
  publishedNoticeTimeText: { type: String, default: '' },
  publishedNoticeList: { type: Array, default: () => [] }
})

const noticeCount = computed(() => {
  return Array.isArray(props.publishedNoticeList) ? props.publishedNoticeList.length : 0
})

const sortedNoticeList = computed(() => {
  const list = Array.isArray(props.publishedNoticeList) ? props.publishedNoticeList : []
  return sortNoticeListByPublishedAtAsc(list)
})

const latestPublishedNotice = computed(() => {
  return sortedNoticeList.value[0] || null
})

const noticePreviewList = computed(() => {
  return sortedNoticeList.value.slice(0, 6)
})

const focusCards = computed(() => {
  if (props.isAdmin) {
    return [
      {
        key: 'products',
        kicker: '产品维护',
        title: '在售产品',
        value: String(Number(props.stats.totalProducts || 0)),
        description: '编辑上架、下架和批量激活都从这里进入。',
        note: `今日新增 ${Number(props.stats.todayNewOrders || 0)} 单`,
        actionLabel: '进入产品中心',
        event: 'goto-product',
        icon: Goods,
        buttonType: 'primary',
        tone: 'tone-primary'
      },
      {
        key: 'pending',
        kicker: '保单待办',
        title: '待处理订单',
        value: String(Number(props.stats.pendingOrders || 0)),
        description: '优先处理审核、承保和生效流程。',
        note: `本月累计 ${Number(props.stats.monthOrders || 0)} 单`,
        actionLabel: '进入保险清单',
        event: 'goto-insurance',
        icon: Collection,
        buttonType: 'success',
        tone: 'tone-success'
      },
      {
        key: 'expense',
        kicker: '资金流水',
        title: '费用记录',
        value: String(Number(props.stats.totalExpenses || 0)),
        description: '查看充值和消费明细，核对资金流转。',
        note: '适合运营和财务复核',
        actionLabel: '查看费用清单',
        event: 'goto-expense',
        icon: Coin,
        buttonType: 'warning',
        tone: 'tone-warning'
      }
    ]
  }

  return [
    {
      key: 'products',
      kicker: '产品入口',
      title: '在售产品',
      value: String(Number(props.stats.totalProducts || 0)),
      description: '快速筛选并进入产品详情或激活流程。',
      note: '先看产品，再处理保单',
      actionLabel: '浏览产品',
      event: 'goto-product',
      icon: Goods,
      buttonType: 'primary',
      tone: 'tone-primary'
    },
    {
      key: 'policies',
      kicker: '保单入口',
      title: '有效保单',
      value: String(Number(props.stats.totalPolicies || 0)),
      description: '查看当前保单、状态和处理进度。',
      note: `累计消费 ¥${formatMoney(props.stats.totalExpenses || 0)}`,
      actionLabel: '查看保单',
      event: 'goto-insurance',
      icon: Collection,
      buttonType: 'success',
      tone: 'tone-success'
    },
    {
      key: 'balance',
      kicker: '账户入口',
      title: '账户余额',
      value: `¥${formatMoney(props.balance || 0)}`,
      description: '随时确认余额并查看消费明细。',
      note: '余额变化直接影响后续操作',
      actionLabel: '消费明细',
      event: 'goto-recharge',
      icon: Wallet,
      buttonType: 'warning',
      tone: 'tone-warning'
    }
  ]
})

defineEmits([
  'goto-product',
  'goto-insurance',
  'goto-expense',
  'goto-recharge',
  'goto-user-admin',
  'open-notice-dialog',
  'open-notice-manage'
])
</script>
