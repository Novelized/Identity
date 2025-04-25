import { UserManager, WebStorageStateStore, Log } from 'oidc-client-ts';
import { ref, computed } from 'vue';

/**
 * Reactive reference to the authenticated user object.
 * Null if the user is not authenticated.
 * @type {import('vue').Ref<import('oidc-client-ts').User | null>}
 */
export const user = ref(null);

/**
 * Reactive reference indicating if the authentication state is currently being loaded.
 * @type {import('vue').Ref<boolean>}
 */
export const isLoading = ref(true);

/**
 * Computed property indicating if the user is authenticated.
 * @type {import('vue').ComputedRef<boolean>}
 */
export const isAuthenticated = computed(() => !!user.value && !user.value.expired);

// Promise setup for initialization completion
let resolveAuthReady;
/**
 * A promise that resolves when the initial authentication check is complete.
 */
export const authReady = new Promise(resolve => {
  resolveAuthReady = resolve;
});

/**
 * OIDC Configuration fetched from environment variables.
 * Note: Vite requires environment variables to be prefixed with VITE_
 * @type {import('oidc-client-ts').UserManagerSettings}
 */
const settings = {
  authority: import.meta.env.VITE_AUTH_AUTHORITY,
  client_id: import.meta.env.VITE_AUTH_CLIENT_ID,
  redirect_uri: `${window.location.origin}/callback`,
  response_type: 'code',
  scope: import.meta.env.VITE_AUTH_SCOPE,
  post_logout_redirect_uri: window.location.origin,
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  automaticSilentRenew: false,
  loadUserInfo: true,
};

/**
 * The UserManager instance responsible for handling OIDC protocol logic.
 * @type {UserManager}
 */
const userManager = new UserManager(settings);

console.log(`[Auth] UserManager configured with Client ID: ${settings.client_id}`);

// --- Logging ---
Log.setLevel(Log.DEBUG);
Log.setLogger(console);

// --- Event Handlers ---

/**
 * Handles the event when a user is successfully loaded.
 * @param {import('oidc-client-ts').User} loadedUser - The loaded user object.
 */
userManager.events.addUserLoaded((loadedUser) => {
  console.log('User loaded:', { sub: loadedUser?.profile?.sub });
  user.value = loadedUser;
  // If loading is still true, set it to false. This might happen if user is loaded
  // very quickly before initializeAuth finishes its own check.
  if (isLoading.value) {
    isLoading.value = false;
  }
  // Ensure the authReady promise resolves if it hasn't already
  if (resolveAuthReady) {
      resolveAuthReady();
      resolveAuthReady = null;
  }
});

/**
 * Handles the event when the user is unloaded (logged out).
 */
userManager.events.addUserUnloaded(() => {
  console.log('User unloaded/logged out');
  user.value = null;
  isLoading.value = false; // Ensure loading is false on logout
  // Ensure the authReady promise resolves if it hasn't already (e.g., if logout happens during init)
  if (resolveAuthReady) {
      resolveAuthReady();
      resolveAuthReady = null;
  }
});

/**
 * Handles the event when the access token is expiring.
 */
userManager.events.addAccessTokenExpiring(() => {
  console.log('Access token expiring. Manual renewal would be needed.');
  // userManager.signinSilent().catch(err => console.error("Manual silent renew failed", err));
});

/**
 * Handles the event when the access token has expired.
 */
userManager.events.addAccessTokenExpired(() => {
  console.log('Access token expired');
  user.value = null; // Clear user state as token is expired
  // Router guard will handle redirection if needed
});

// --- Public Methods ---

/**
 * Initiates the OIDC login redirect flow.
 * @returns {Promise<void>}
 */
export const login = () => {
  isLoading.value = true;
  // Redirects the main window to the authorization server
  return userManager.signinRedirect();
};

/**
 * Initiates the OIDC logout redirect flow.
 * @returns {Promise<void>}
 */
export const logout = () => {
  if (user.value) {
    isLoading.value = true;
    return userManager.signoutRedirect();
  }
  return Promise.resolve();
};

/**
 * Handles the OIDC redirect callback.
 * Should be called from the callback component.
 * @returns {Promise<import('oidc-client-ts').User | null>} The authenticated user or null.
 */
export const handleCallback = async () => {
  isLoading.value = true;
  console.log(`[Auth] Handling callback. Using Client ID: ${userManager.settings.client_id}`);
  try {
    // Processes the response from the authorization server in the main window
    const userReturned = await userManager.signinRedirectCallback();
    console.log('Callback processed, user:', { sub: userReturned?.profile?.sub });
    // user.value = userReturned; // Already handled by addUserLoaded event
    isLoading.value = false;
    return userReturned;
  } catch (error) {
    console.error('Error handling OIDC callback:', error);
    user.value = null;
    isLoading.value = false;
    throw error; // Re-throw error for the component to handle
  }
};

/**
 * Gets the current access token if the user is authenticated and the token is not expired.
 * @returns {Promise<string | null>} The access token or null.
 */
export const getAccessToken = async () => {
  const currentUser = await userManager.getUser();
  if (!currentUser || currentUser.expired) {
    // If no user or user token is expired, return null
    return null;
  }
  // Otherwise, return the valid access token
  return currentUser.access_token;
};

/**
 * Initializes the authentication service.
 * Tries to load the user from storage. No silent sign-in attempt.
 * Should be called once when the app starts.
 * @returns {Promise<void>}
 */
export const initializeAuth = async () => {
    console.log('Initializing auth (no silent sign-in)...');
    console.log(`[Auth] Initializing with configured Client ID: ${settings.client_id}`);
    isLoading.value = true;
    try {
        const loadedUser = await userManager.getUser();
        if (loadedUser && !loadedUser.expired) {
            // Log only sub claim for security
            console.log('User found in storage:', { sub: loadedUser?.profile?.sub });
            user.value = loadedUser; // addUserLoaded event might also fire
        } else {
            console.log('No valid user found in storage.');
            if (loadedUser && loadedUser.expired) {
                console.log('Stored user is expired.');
            }
            user.value = null;
            // Clean up potentially expired user state only if a user object was found but expired
            if (loadedUser) {
                await userManager.removeUser();
            }
        }
    } catch (error) {
        console.error("Error during auth initialization:", error);
        user.value = null;
    } finally {
        isLoading.value = false;
        // Log only sub claim for security
        console.log('Auth initialization complete. isLoading:', isLoading.value, 'user:', { sub: user.value?.profile?.sub });
        // Resolve the promise to signal auth is ready
        if (resolveAuthReady) {
            resolveAuthReady();
            resolveAuthReady = null; // Ensure it's only resolved once
        }
    }
};
