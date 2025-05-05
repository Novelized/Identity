import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { initializeAuth, isLoading } from './auth'

/**
 * Initializes the authentication service and then mounts the Vue application.
 */
async function startup() {
    // Initialize authentication service first
    await initializeAuth();

    // Now create and mount the app
    const app = createApp(App);
    app.use(router);
    app.mount('#app');
}

// Start the application initialization process
startup();
