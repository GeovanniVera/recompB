export interface User {
  id: number;
  email: string;
  displayName: string | null;
  telegramLinked: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  expiresAt: string;
  user: User;
}

export interface OtpRequestResponse {
  sent: boolean;
  expiresInSeconds: number;
  resendAfterSeconds: number;
  message: string;
}

export interface ApiError {
  timestamp?: string;
  status: number;
  code: string;
  message: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}
