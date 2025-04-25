<template>
  <div>
    <h2>Login Required</h2>
    <p>You need to log in to access the requested page.</p>
    <div v-if="errorMessage" style="color: red; margin-bottom: 15px;">
      {{ errorMessage }}
    </div>
    <button @click="handleLogin">Log In</button>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';
import { login } from '../auth';

const route = useRoute();
const errorMessage = route.query.error;

const handleLogin = () => {
  login();
};
</script>

<style scoped>
div {
  padding: 20px;
  text-align: center;
}
button {
  padding: 10px 20px;
  font-size: 1em;
  cursor: pointer;
}
</style> 