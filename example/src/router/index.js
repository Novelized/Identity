import { createRouter, createWebHistory } from 'vue-router';
import Home from '../components/Home.vue';
import Callback from '../components/Callback.vue';
import Secured from '../views/Secured.vue';
import Login from '../views/Login.vue';
import { isAuthenticated, authReady } from '../auth';

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
 * It waits for auth initialization, then checks status before allowing access.
 * If not authenticated for a protected route, it redirects to Login.
 * If authenticated, it redirects away from Login.
 * @param {import('vue-router').RouteLocationNormalized} to - The target route object.
 * @param {import('vue-router').RouteLocationNormalized} from - The current route object being navigated away from.
 * @param {import('vue-router').NavigationGuardNext} next - Function to resolve the navigation hook.
 */
router.beforeEach(async (to, from, next) => {
  // Wait for the initial auth check to complete before evaluating the route
  await authReady;
  console.log('Auth is ready, proceeding with route guard check.');

  try {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
    const authed = isAuthenticated.value;

    console.log(`Routing to: ${to.path}, requiresAuth: ${requiresAuth}, isAuthenticated: ${authed}`);

    if (requiresAuth && !authed) {
      console.log('Redirecting to Login because route requires auth and user is not authenticated.');
      // Pass the intended destination as a query parameter for redirection after login
      next({ name: 'Login', query: { redirect: to.fullPath } });
    } else if (to.name === 'Login' && authed) {
      console.log('Redirecting to Home because user is already authenticated and tried to access Login.');
      next({ name: 'Home' });
    } else {
      next();
    }
  } catch (error) {
    console.error('Error in navigation guard logic:', error);
    // Fallback: Redirect to login page in case of unexpected errors in the guard
    next({ name: 'Login' });
  }
});

export default router; 