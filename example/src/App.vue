<script setup>
/**
 * @file Root application component.
 */
import { isAuthenticated, isLoading } from './auth';

/**
 * Component setup logic.
 */
</script>

<template>
  <header>
    <nav>
      <router-link to="/">Home</router-link> |
      <router-link v-if="isAuthenticated" to="/secured">Secured</router-link>
      <span v-else>Secured (Login required)</span>
    </nav>
    <div v-if="isLoading">App Loading...</div>
  </header>
  <hr />
  <main>
    <router-view />
  </main>
</template>

<style scoped>
.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vue:hover {
  filter: drop-shadow(0 0 2em #42b883aa);
}
header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #ccc;
}
nav a {
  margin-right: 10px;
}
</style>
