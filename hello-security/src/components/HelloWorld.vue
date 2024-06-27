<template>
  <div class="hello">
    <button @click="getQrUrl">GET URL</button>
    <div style="margin-top: 50px;"><qrcode-vue v-if="url" :value="url" :size="200" level="H" /></div>
  </div>
</template>

<script setup>
import { onBeforeUnmount } from 'vue'
import QrcodeVue from 'qrcode.vue'
import { ref } from 'vue'
import axiosIns from '@/request/http'
import router from '@/router'

const url = ref("")
let timer = null

const getQrUrl = () => {
  axiosIns.get("/getQrUrl").then(e => {
    if (e.data && e.data.code == 0) {
      url.value = e.data.data
      timer = setInterval(() => {
        setTimeout(() => {
          check()
        }, 0);
      }, 1000);
    }
  })
}

const check = () => {
  console.log(router);
  axiosIns.get(`/checkUrl?url=${url.value}`).then(e => {
    if (e.data && e.data.code == 0) {
      const status = e.data.data
      if (status) {
        clearInterval(timer);
        router.push('/fuck')
      } else {
        console.log("请求中...");
      }
      return
    }
    url.value = ""
    clearInterval(timer);
  })
}

onBeforeUnmount(() => {
  clearInterval(timer);
})
</script>

<style scoped></style>
