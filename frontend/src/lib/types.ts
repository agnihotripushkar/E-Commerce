export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface User {
  id: string;
  email: string;
  role: string;
  createdAt: string;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  price: number;
  stock: number;
  category: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  productId: number;
  quantity: number;
}

export interface OrderLineItem {
  productId: number;
  quantity: number;
  unitPrice: number;
}

export type OrderStatus = 'PENDING' | 'PAID' | 'PAYMENT_FAILED' | 'CANCELLED';

export interface Order {
  id: string;
  userId: string;
  status: OrderStatus;
  totalAmount: number;
  lineItems: OrderLineItem[];
  createdAt: string;
  updatedAt: string;
}

export interface Payment {
  id: string;
  orderId: string;
  amount: number;
  status: 'COMPLETED' | 'FAILED';
  createdAt: string;
}

export type NotificationType =
  | 'ORDER_PLACED'
  | 'PAYMENT_CONFIRMED'
  | 'PAYMENT_FAILED'
  | 'ORDER_SHIPPED'
  | 'ORDER_CANCELLED';

export interface Notification {
  id: string;
  userId: string;
  orderId: string;
  notificationType: NotificationType;
  message: string;
  createdAt: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}
