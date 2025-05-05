export type FirebaseErrorCode = (typeof FirebaseErrorCodeEnum)[keyof typeof FirebaseErrorCodeEnum];
export const FirebaseErrorCodeEnum = {
  EmailExists: 'auth/email-already-in-use',
  NetworkFailed: 'auth/network-request-failed',
  InvalidCreds: 'auth/invalid-credential',
} as const satisfies Record<string, string>

export type FirebaseErrorCodeMessage = (typeof FirebaseErrorCodeMessageEnum)[keyof typeof FirebaseErrorCodeMessageEnum];
export const FirebaseErrorCodeMessageEnum = {
  [FirebaseErrorCodeEnum.EmailExists]: 'Email already registered.',
  [FirebaseErrorCodeEnum.NetworkFailed]: 'Network error. Please check you internet connection.',
  [FirebaseErrorCodeEnum.InvalidCreds]: 'Invalid credentials. Please try again.',
} as const satisfies Record<string, string>

export type FirebaseAuthError = { code: FirebaseErrorCode, message: string };
