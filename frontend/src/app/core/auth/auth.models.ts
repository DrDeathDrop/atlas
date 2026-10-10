export type Role = 'ADMIN' | 'DISPATCHER' | 'FIELD_OPERATOR' | 'ANALYST' | 'VIEWER';

export interface CurrentUser {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  enabled: boolean;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}
