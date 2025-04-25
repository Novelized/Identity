<template>
  <div>
    <h1>Secured Page</h1>
    <p>This page can only be accessed by authenticated users.</p>
    <p>Authentication Status:</p>
    <pre style="word-wrap: break-word; white-space: pre-wrap;">{{ isAuthenticated ? 'Successfully authenticated ✅' : 'Loading...' }}</pre>
    <button @click="goHome">Go Home</button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { isAuthenticated } from '../auth';

const router = useRouter();

const goHome = () => {
  router.push({ name: 'Home' });
};
</script>

<style scoped>
div {
  padding: 20px;
  text-align: center;
}
pre {
  background-color: #f4f4f4;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-top: 5px;
}
button {
  margin-top: 15px;
  padding: 10px 20px;
  font-size: 1em;
  cursor: pointer;
}
</style> 