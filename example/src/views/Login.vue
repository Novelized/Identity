<template>
  <div>
    <h2>Login Required</h2>
    <p>You need to log in to access the requested page.</p>
    <div v-if="loginInitiationError" style="color: red; margin-bottom: 15px;">
      {{ loginInitiationError }}
    </div>
    <div v-else-if="errorMessage" style="color: red; margin-bottom: 15px;">
      {{ errorMessage }}
    </div>
    <button @click="handleLogin" :disabled="isLoggingIn">
      {{ isLoggingIn ? 'Logging in...' : 'Log In' }}
    </button>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';
import { ref } from 'vue';
import { login } from '../auth';

const route = useRoute();
const errorMessage = route.query.error;
const isLoggingIn = ref(false);
const loginInitiationError = ref(null);

const handleLogin = () => {
  isLoggingIn.value = true;
  loginInitiationError.value = null;
  login().catch((err) => {
    console.error("Login initiation failed:", err);
    loginInitiationError.value = 'Failed to start the login process. Please try again.';
    isLoggingIn.value = false;
  });
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