import { createRouter, createWebHistory } from 'vue-router';
import Home from '../components/Home.vue';
import Callback from '../components/Callback.vue';
import Secured from '../components/Secured.vue';
import Login from '../views/Login.vue';
import { isAuthenticated, isLoading } from '../auth'; // Removed initializeAuth import, not needed here

/**
 * Defines the application routes.
 * @type {Array<import('vue-router').RouteRecordRaw>}
 */
const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { requiresAuth: true },
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: Callback,
    meta: { requiresAuth: false }
  },
  {
    path: '/secured',
    name: 'Secured',
    component: Secured,
    meta: { requiresAuth: true },
  },
];

/**
 * Creates and configures the Vue Router instance.
 */
const router = createRouter({
  history: createWebHistory(),
  routes,
});

/**
 * Navigation guard to protect routes requiring authentication.
 * It checks the authentication status before allowing access to protected routes.
 * If auth is loading, it waits. If not authenticated, it redirects to Login.
 * If authenticated, it redirects away from Login.
 * @param {import('vue-router').RouteLocationNormalized} to - The target route object.
 * @param {import('vue-router').RouteLocationNormalized} from - The current route object being navigated away from.
 * @param {import('vue-router').NavigationGuardNext} next - Function to resolve the navigation hook.
 */
router.beforeEach(async (to, from, next) => {
  // Ensure auth is initialized before checking requiresAuth
  // This prevents race conditions on initial load or page refresh
  while (isLoading.value) {
    console.log('Router guard waiting for auth init...');
    await new Promise(resolve => setTimeout(resolve, 50));
  }

  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const authed = isAuthenticated.value;

  console.log(`Routing to: ${to.path}, requiresAuth: ${requiresAuth}, isAuthenticated: ${authed}`);

  if (requiresAuth && !authed) {
    console.log('Redirecting to Login because route requires auth and user is not authenticated.');
    next({ name: 'Login' });
  } else if (to.name === 'Login' && authed) {
    console.log('Redirecting to Home because user is already authenticated and tried to access Login.');
    next({ name: 'Home' });
  } else {
    next();
  }
});

export default router; 