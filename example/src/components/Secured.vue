<template>
  <div>
    <h1>Secured Page</h1>
    <p>This page can only be accessed by authenticated users.</p>
    <p>Your Access Token:</p>
    <pre style="word-wrap: break-word; white-space: pre-wrap;">{{ accessToken || 'Loading...' }}</pre>
    <button @click="goHome">Go Home</button>
  </div>
</template>

<script setup>
/**
 * @file Secured component accessible only by authenticated users.
 * Demonstrates retrieving the access token.
 */
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getAccessToken } from '../auth';

/**
 * Vue Router instance.
 */
const router = useRouter();

/**
 * Reactive reference to store the access token.
 * @type {import('vue').Ref<string | null>}
 */
const accessToken = ref(null);

/**
 * Navigates back to the Home page.
 */
const goHome = () => {
  router.push({ name: 'Home' });
};

/**
 * Lifecycle hook called after the component is mounted.
 * Fetches the access token.
 */
onMounted(async () => {
  accessToken.value = await getAccessToken();
});

</script> 