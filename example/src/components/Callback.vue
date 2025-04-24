<template>
  <div>Processing login callback...</div>
</template>

<script setup>
/**
 * @file Callback component that handles the OIDC redirect callback.
 */
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { handleCallback } from '../auth';

/**
 * Vue Router instance.
 */
const router = useRouter();

/**
 * Lifecycle hook called after the component is mounted.
 * Processes the OIDC callback and redirects the user.
 */
onMounted(async () => {
  try {
    await handleCallback();
    console.log('Callback successful, redirecting to home.');
    router.push({ name: 'Home' });
  } catch (error) {
    console.error('Callback error:', error);
    router.push({ name: 'Home' });
  }
});
</script> 