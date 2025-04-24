<template>
  <div>
    <h2>Login Required</h2>
    <p>You need to log in to access the requested page.</p>
    <button @click="handleLogin">Log In</button>
  </div>
</template>

<script setup>
import { login } from '../auth';

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