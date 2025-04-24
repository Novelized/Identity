<template>
  <div class="home-container">
    <h1>Home</h1>
    <div v-if="isLoadingAuth">Loading authentication status...</div>
    <div v-else>
      <div v-if="isAuthenticated">
        <p>Welcome, {{ user?.profile?.name || 'User' }}!</p>
        <button @click="logout" class="action-button">Logout</button>
        <router-link to="/secured" custom v-slot="{ navigate }">
          <button @click="navigate" role="link" :disabled="false" class="action-button">
            Go to Secured Page
          </button>
        </router-link>

        <div class="messages-section">
          <h2>Messages from Backend</h2>
          <div v-if="isLoadingMessages">Loading messages...</div>
          <div v-else-if="messagesError" class="error-message">
            Error loading messages: {{ messagesError }}
          </div>
          <ul v-else-if="messages.length > 0" class="message-list">
            <li v-for="(msg, index) in messages" :key="index" class="message-item">
              {{ msg.text }}
            </li>
          </ul>
          <p v-else>No messages found.</p>
        </div>

      </div>
      <div v-else>
        <p>You are not logged in.</p>
        <button @click="login">Login</button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * @file Home component providing login/logout actions and displaying user status.
 */
import { ref, onMounted, watch } from 'vue';
import { user, isAuthenticated, login, logout, isLoading as isLoadingAuth } from '../auth';
import { getMessages } from '../services/apiService';

const messages = ref([]);
const isLoadingMessages = ref(false);
const messagesError = ref(null);

// Function to fetch messages
const fetchMessages = async () => {
  if (!isAuthenticated.value) {
    // Don't fetch if not authenticated
    messages.value = [];
    messagesError.value = null;
    return;
  }

  isLoadingMessages.value = true;
  messagesError.value = null;
  try {
    messages.value = await getMessages();
  } catch (error) {
    console.error("Failed to load messages:", error);
    messagesError.value = error.message || 'Unknown error';
  } finally {
    isLoadingMessages.value = false;
  }
};

// Fetch messages when the component mounts if already authenticated
onMounted(() => {
  if (isAuthenticated.value) {
    fetchMessages();
  }
});

// Watch for changes in authentication status
watch(isAuthenticated, (newValue, oldValue) => {
  if (newValue === true && oldValue === false) {
    // User just logged in
    fetchMessages();
  } else if (newValue === false) {
    // User just logged out
    messages.value = [];
    messagesError.value = null;
  }
});
</script>

<style scoped>
.home-container {
  font-family: sans-serif;
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

h1 {
  color: #333;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
  margin-bottom: 20px;
}

/* Add spacing between adjacent action buttons */
.action-button + .action-button,
.action-button + router-link > .action-button {
  margin-left: 8px;
}

/* --- Styling for the Messages Card --- */
.messages-section {
  margin-top: 30px;
}

.messages-section h2 {
  margin-top: 0;
}

.error-message {
  color: #d9534f;
  background-color: #f2dede;
  border: 1px solid #ebccd1;
  padding: 10px;
  border-radius: 4px;
}

pre {
  background-color: #f0f0f0;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}
</style> 