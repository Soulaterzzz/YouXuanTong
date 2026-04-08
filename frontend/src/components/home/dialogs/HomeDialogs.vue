<template>
  <ProductDialogs />
  <InsuranceDialogs />
  <RechargeDialogs />
  <UserDialogs />
  <NoticeDialogs />
</template>

<script>
import { defineAsyncComponent } from 'vue'

const ProductDialogs = defineAsyncComponent(() => import('./ProductDialogs.vue'))
const InsuranceDialogs = defineAsyncComponent(() => import('./InsuranceDialogs.vue'))
const RechargeDialogs = defineAsyncComponent(() => import('./RechargeDialogs.vue'))
const UserDialogs = defineAsyncComponent(() => import('./UserDialogs.vue'))
const NoticeDialogs = defineAsyncComponent(() => import('./NoticeDialogs.vue'))

export default {
  name: 'HomeDialogs',
  components: {
    ProductDialogs,
    InsuranceDialogs,
    RechargeDialogs,
    UserDialogs,
    NoticeDialogs
  }
}
</script>
