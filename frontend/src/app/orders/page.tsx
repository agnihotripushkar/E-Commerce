'use client';

import { useEffect, useState, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { Order, Notification } from '@/lib/types';
import { orderApi, notificationApi } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';

const STATUS_STYLES: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
  PAID: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  PAYMENT_FAILED: 'bg-red-50 text-red-700 border-red-200',
  CANCELLED: 'bg-gray-100 text-gray-500 border-gray-200',
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pending',
  PAID: 'Paid',
  PAYMENT_FAILED: 'Payment Failed',
  CANCELLED: 'Cancelled',
};

const NOTIF_EMOJI: Record<string, string> = {
  ORDER_PLACED: '📦',
  PAYMENT_CONFIRMED: '✅',
  PAYMENT_FAILED: '❌',
  ORDER_SHIPPED: '🚚',
  ORDER_CANCELLED: '🚫',
};

function OrdersContent() {
  const { user, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const newOrderId = searchParams.get('newOrder');

  const [orders, setOrders] = useState<Order[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.push('/auth/login?redirect=/orders');
      return;
    }
    Promise.all([
      orderApi.listByUser(user.id) as Promise<Order[]>,
      notificationApi.listByUser(user.id) as Promise<Notification[]>,
    ])
      .then(([o, n]) => {
        setOrders(o.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
        setNotifications(n.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [user, authLoading, router]);

  async function handleCancel(orderId: string) {
    setCancellingId(orderId);
    try {
      const updated = await orderApi.cancel(orderId) as Order;
      setOrders(prev => prev.map(o => o.id === orderId ? updated : o));
    } catch (e) {
      console.error(e);
    } finally {
      setCancellingId(null);
    }
  }

  if (authLoading || loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 space-y-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="bg-white rounded-2xl h-32 animate-pulse border border-gray-100" />
        ))}
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-bold text-gray-900 mb-2">My Orders</h1>
      <p className="text-gray-500 mb-8">Track your orders and view order history</p>

      {newOrderId && (
        <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 mb-6 flex items-center gap-3">
          <span className="text-2xl">🎉</span>
          <div>
            <p className="font-semibold text-emerald-800">Order placed successfully!</p>
            <p className="text-sm text-emerald-600">Payment is being processed. Your order will update shortly.</p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Orders */}
        <div className="lg:col-span-2 space-y-4">
          <h2 className="font-semibold text-gray-900">Orders ({orders.length})</h2>

          {orders.length === 0 ? (
            <div className="bg-white rounded-2xl border border-gray-100 p-10 text-center">
              <span className="text-4xl block mb-3">📦</span>
              <p className="font-semibold text-gray-700">No orders yet</p>
              <Link href="/products" className="text-emerald-600 hover:underline text-sm mt-2 block">
                Start shopping
              </Link>
            </div>
          ) : (
            orders.map(order => (
              <div key={order.id}
                className={`bg-white rounded-2xl border p-5 ${order.id === newOrderId ? 'border-emerald-300 ring-2 ring-emerald-100' : 'border-gray-100'}`}>
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div>
                    <p className="text-xs text-gray-400 font-mono">{order.id}</p>
                    <p className="text-sm text-gray-500 mt-0.5">
                      {new Date(order.createdAt).toLocaleDateString('en-US', {
                        year: 'numeric', month: 'long', day: 'numeric',
                      })}
                    </p>
                  </div>
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${STATUS_STYLES[order.status] ?? ''}`}>
                    {STATUS_LABEL[order.status] ?? order.status}
                  </span>
                </div>

                <div className="space-y-1 mb-3">
                  {order.lineItems.map((item, i) => (
                    <div key={i} className="flex justify-between text-sm text-gray-600">
                      <span>Product #{item.productId} × {item.quantity}</span>
                      <span>${(Number(item.unitPrice) * item.quantity).toFixed(2)}</span>
                    </div>
                  ))}
                </div>

                <div className="flex items-center justify-between border-t border-gray-50 pt-3">
                  <span className="font-bold text-gray-900">Total: ${Number(order.totalAmount).toFixed(2)}</span>
                  {order.status === 'PENDING' && (
                    <button
                      onClick={() => handleCancel(order.id)}
                      disabled={cancellingId === order.id}
                      className="text-xs text-red-500 hover:text-red-700 font-medium border border-red-200 hover:border-red-400 px-3 py-1.5 rounded-lg transition-colors disabled:opacity-50"
                    >
                      {cancellingId === order.id ? 'Cancelling...' : 'Cancel Order'}
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Notifications */}
        <div>
          <h2 className="font-semibold text-gray-900 mb-4">Notifications</h2>
          {notifications.length === 0 ? (
            <div className="bg-white rounded-2xl border border-gray-100 p-6 text-center text-sm text-gray-500">
              No notifications yet
            </div>
          ) : (
            <div className="space-y-2">
              {notifications.slice(0, 10).map(n => (
                <div key={n.id} className="bg-white rounded-xl border border-gray-100 p-3">
                  <div className="flex items-start gap-2">
                    <span className="text-lg">{NOTIF_EMOJI[n.notificationType] ?? '🔔'}</span>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-gray-700 leading-snug">{n.message}</p>
                      <p className="text-xs text-gray-400 mt-1">
                        {new Date(n.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default function OrdersPage() {
  return (
    <Suspense>
      <OrdersContent />
    </Suspense>
  );
}
