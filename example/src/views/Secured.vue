<template>
  <div>
    <h1>Secured Page</h1>
    <p>This page can only be accessed by authenticated users.</p>
    <p>Authentication Status:</p>
    <pre class="auth-status">{{ isAuthenticated ? `Successfully authenticated as ${user?.profile?.name || 'Unknown User'} ✅` : 'Loading authentication status...' }}</pre>
    <button @click="goHome">Go Home</button>
    <!-- Display navigation error if it occurs -->
    <div v-if="navigationError" style="color: red; margin-top: 15px;">
      {{ navigationError }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { isAuthenticated, user } from '../auth';

const router = useRouter();
const navigationError = ref(null); // Ref to store navigation errors

const goHome = async () => {
  navigationError.value = null; // Clear previous errors before trying
  try {
    await router.push({ name: 'Home' });
  } catch (error) {
    console.error('Navigation failed:', error);
    navigationError.value = 'Failed to navigate to Home. Please try again.';
  }
};
</script>

<style scoped>
div {
  padding: 20px;
  text-align: center;
}

/* Replaced inline style with a class */
.auth-status {
  background-color: #f4f4f4;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-top: 5px;
  word-wrap: break-word;
  white-space: pre-wrap;
}

button {
  margin-top: 15px;
  padding: 10px 20px;
  font-size: 1em;
  cursor: pointer;
}
</style> 