/**
 * @file Service for interacting with the backend API.
 */
import { getAccessToken } from '../auth';

const API_BASE_URL = 'http://localhost:8080';

/**
 * Fetches messages from the secured backend endpoint.
 * Requires a valid access token.
 *
 * @returns {Promise<Array<object>>} A promise that resolves to an array of message objects.
 * @throws {Error} If the fetch operation fails or the user is not authenticated.
 */
export const getMessages = async () => {
  const token = await getAccessToken(); // Ensure we await the async getAccessToken
  if (!token) {
    console.error('[ApiService] No access token available. Please log in.');
    throw new Error('No access token available. Please log in.');
  }

  console.log('[ApiService] Attempting to fetch /messages with token:', token);

  try {
    const response = await fetch(`${API_BASE_URL}/messages`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    
    // Log the raw response text regardless of status for debugging
    const responseBodyText = await response.text();
    console.debug('[ApiService] Raw response text:', responseBodyText);

    if (!response.ok) {
      console.error(`[ApiService] API Error Response Status: ${response.status} ${response.statusText}`);
      console.error('[ApiService] API Error Response Body:', responseBodyText);
      throw new Error(`Failed to fetch messages: ${response.status} ${response.statusText}`);
    }

    // If response was OK, try to parse the logged text as JSON
    try {
        return JSON.parse(responseBodyText);
    } catch (parseError) {
        console.error('[ApiService] Failed to parse successful response body as JSON:', parseError);
        console.error('[ApiService] Response body was:', responseBodyText);
        throw new Error('Received non-JSON response from server despite OK status.');
    }

  } catch (error) {
    // Avoid logging the error twice if it was thrown above
    if (!(error instanceof Error && error.message.startsWith('Failed to fetch messages'))) {
        console.error('[ApiService] Error during fetch operation:', error);
    }
    // Re-throw the error so the component can handle it (e.g., display an error message)
    throw error;
  }
}; 